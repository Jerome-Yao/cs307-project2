package edu.sustech.cs307.physicalOperator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pmw.tinylog.Logger;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.aggregate.AggregateExpression;
import edu.sustech.cs307.DBEntry;
import edu.sustech.cs307.aggregate.AggregateCalculator;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import net.sf.jsqlparser.schema.Column;

public class AggregateOperator implements PhysicalOperator {
    private final PhysicalOperator child;
    private List<TabCol> groupByColumns = new ArrayList<>();
    private List<AggregateExpression> aggregateExpressions = new ArrayList<>();
    private boolean isOpen = false;
    private Iterator<TableTuple> resultIterator;
    private TableTuple currentTuple;
    private ArrayList<ColumnMeta> outputSchema;
    private String tableName;

    public AggregateOperator(PhysicalOperator child, List<Column> groupByColumns,
            List<AggregateExpression> aggregateExpressions, String tableName) {
        this.child = child;
        for (Column col : groupByColumns) {
            this.groupByColumns.add(new TabCol(tableName, col.getColumnName()));
        }
        this.aggregateExpressions = aggregateExpressions;
        this.tableName = tableName;
        this.outputSchema = buildOutputSchema();
    }

    @Override
    public void Begin() throws DBException {
        try {
            Logger.debug("AggregateOperator.Begin() 被调用");
            child.Begin();
            isOpen = true;
            Map<List<Value>, List<TableTuple>> groupMap = new LinkedHashMap<>();

            // Group tuples by group-by columns
            while (child.hasNext()) {
                child.Next();
                TableTuple t = (TableTuple) child.Current();
                if (t == null)
                    continue;

                List<Value> key = new ArrayList<>();
                for (TabCol idx : groupByColumns) {
                    key.add(t.getValue(idx));
                }

                groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
            }

            // Calculate aggregates for each group
            List<TableTuple> resultTuples = new ArrayList<>();
            for (Map.Entry<List<Value>, List<TableTuple>> entry : groupMap.entrySet()) {
                List<Value> groupKey = entry.getKey();
                List<TableTuple> groupTuples = entry.getValue();

                TableTuple aggregatedTuple = calculateAggregates(groupKey, groupTuples);
                resultTuples.add(aggregatedTuple);
            }

            resultIterator = resultTuples.iterator();
            if (!resultIterator.hasNext()) {
                isOpen = false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform aggregation: " + e.getMessage() + "\n");
        }
    }

    private TableTuple calculateAggregates(List<Value> groupKey, List<TableTuple> groupTuples) {
        Map<TabCol, Value> resultValues = new LinkedHashMap<>();

        // Add group-by column values
        // for (int i = 0; i < groupByColumns.size(); i++) {
        // TabCol groupCol = groupByColumns.get(i);
        // resultValues.put(groupCol, groupKey.get(i));
        // }

        // Calculate aggregate values
        for (AggregateExpression aggExpr : aggregateExpressions) {
            List<Value> columnValues = new ArrayList<>();
            TabCol targetCol = aggExpr.getTargetColumn();

            // tuples in each group
            for (TableTuple tuple : groupTuples) {
                try {
                    Value tupleValue = tuple.getValue(targetCol);
                    if (tupleValue != null && tupleValue.value != null) {
                        columnValues.add(tupleValue);
                    }
                } catch (DBException e) {
                    // Skip this value if there's an error accessing it
                    continue;
                }
            }

            Value aggregateResult = AggregateCalculator.calculate(
                    aggExpr.getFunction(), columnValues, aggExpr.isDistinct());

            // Create a TabCol for the aggregate result
            TabCol aggCol = new TabCol(tableName, aggExpr.getAlias());
            resultValues.put(aggCol, aggregateResult);
        }

        // Create a simplified tuple for aggregate results
        // We need to create a temporary TableMeta and Record for this
        // try {
        //     System.out.println("here");
        //     System.out.println(DBEntry.getRecordString(createAggregateTableTuple(resultValues)));
        // } catch (DBException e) {
        //     System.out.println("empty tuple");

        // }
        return createAggregateTableTuple(resultValues);
    }

    @Override
    public boolean hasNext() throws DBException {
        if (!isOpen)
            return false;
        return resultIterator != null && resultIterator.hasNext();
    }

    @Override
    public void Next() throws DBException {
        Logger.debug("AggregateOperator.Next() 被调用");
        if (!isOpen || resultIterator == null)
            return;

        if (resultIterator.hasNext()) {
            currentTuple = resultIterator.next();
        } else {
            currentTuple = null;
        }
    }

    @Override
    public Tuple Current() {
        return currentTuple;
    }

    @Override
    public void Close() {
        child.Close();
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        return outputSchema;
    }

    // the size of Map should be the size of group (assume there's only one result
    // in each group)
    private TableTuple createAggregateTableTuple(Map<TabCol, Value> resultValues) {
        // Create a simple implementation for aggregate results
        return new TableTuple("aggregate", null, null, null) {
            @Override
            public Value getValue(TabCol tabCol) throws DBException {
                TabCol temp=resultValues.entrySet().iterator().next().getKey();
                assert(tabCol.equals(temp));
                // Value value = resultValues.get(tabCol);
                if (resultValues.get(tabCol) == null){
                    // System.out.println("Value is null for column at getValue");
                    return null;
                }
                else{
                    return resultValues.get(tabCol);
                }
                

                // Determine the type based on the value
                // if (value instanceof Long) {
                //     return new Value((Long) value);
                // } else if (value instanceof Double) {
                //     return new Value((Double) value);
                // } else if (value instanceof String) {
                //     return new Value((String) value);
                // } else {
                //     return new Value(value.toString());
                // }
            }

            @Override
            public TabCol[] getTupleSchema() {
                return resultValues.keySet().toArray(new TabCol[0]);
            }

            @Override
            public Value[] getValues() throws DBException {
                Value[] values = new Value[resultValues.size()];
                int i = 0;
                for (TabCol key : resultValues.keySet()) {
                    values[i++] = getValue(key);
                }
                return values;
            }
        };
    }

    private ArrayList<ColumnMeta> buildOutputSchema() {
        ArrayList<ColumnMeta> newSchema = (ArrayList<ColumnMeta>) child.outputSchema().clone();
        int currentOffset = 0;
        ArrayList<ColumnMeta> childOutputSchema = this.child.outputSchema(); // Fetch once

        // Add group-by columns to schema
        for (TabCol groupColKey : this.groupByColumns) {
            boolean found = false;
            for (ColumnMeta childColMeta : childOutputSchema) {
                // Match by column name. Optionally, if groupColKey has a table name, match that too.
                String groupColTableName = groupColKey.getTableName();
                if (childColMeta.name.equalsIgnoreCase(groupColKey.getColumnName()) &&
                    (groupColTableName == null || groupColTableName.isEmpty() || groupColTableName.equalsIgnoreCase(childColMeta.tableName))) {
                    newSchema.add(new ColumnMeta(childColMeta.tableName, childColMeta.name, childColMeta.type,
                            childColMeta.len, currentOffset));
                    currentOffset += childColMeta.len;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // This should ideally be caught earlier by a semantic analyzer.
                Logger.warn("Group by column " + groupColKey.toString() + " not found in child schema.");
                // Consider throwing an exception or adding a placeholder with default properties.
                // For now, we'll skip adding it if not found, which might lead to issues later.
            }
        }

        // Add aggregate columns to schema
        for (AggregateExpression aggExpr : this.aggregateExpressions) {
            ValueType resultType;
            int resultLength;
            String aggAlias = aggExpr.getAlias();
            TabCol targetCol = aggExpr.getTargetColumn(); // This is edu.sustech.cs307.meta.TabCol

            switch (aggExpr.getFunction()) {
                case COUNT:
                    resultType = ValueType.INTEGER;
                    resultLength = 4; // Standard size for INTEGER (e.g., Java int)
                    break;
                case AVG:
                    resultType = ValueType.FLOAT;
                    resultLength = 4; // Standard size for FLOAT (e.g., Java float)
                    break;
                case SUM:
                    // Default to FLOAT, then check specific input type if a target column exists
                    resultType = ValueType.FLOAT;
                    resultLength = 4; 
                    if (targetCol != null && targetCol.getColumnName() != null && !targetCol.getColumnName().isEmpty()) {
                        boolean targetFound = false;
                        for (ColumnMeta colMeta : childOutputSchema) {
                            String targetColTableName = targetCol.getTableName();
                            if (colMeta.name.equalsIgnoreCase(targetCol.getColumnName()) &&
                                (targetColTableName == null || targetColTableName.isEmpty() || targetColTableName.equalsIgnoreCase(colMeta.tableName))) {
                                if (colMeta.type == ValueType.INTEGER) {
                                    resultType = ValueType.INTEGER;
                                    // resultLength remains 4 for INTEGER
                                }
                                // else, SUM of non-INTEGER (e.g., FLOAT) results in FLOAT.
                                // If system supports DOUBLE, SUM of DOUBLE should be DOUBLE. Current logic maps to FLOAT.
                                targetFound = true;
                                break;
                            }
                        }
                        if (!targetFound) {
                             Logger.warn("Target column " + targetCol.toString() + " for SUM not found. Defaulting to FLOAT.");
                        }
                    } else { // SUM(*) or SUM(literal) - typically COUNT(*) semantics or depends on literal type
                        Logger.warn("SUM without a direct column target (e.g., SUM(*) or SUM(literal)). Defaulting type to INTEGER (like COUNT).");
                        resultType = ValueType.INTEGER; // Treat like COUNT
                        resultLength = 4;
                    }
                    break;
                case MIN:
                case MAX:
                    // Default if target column not found or not applicable
                    resultType = ValueType.CHAR; 
                    resultLength = 255; // Default length for fallback CHAR
                    if (targetCol != null && targetCol.getColumnName() != null && !targetCol.getColumnName().isEmpty()) {
                        boolean targetFound = false;
                        for (ColumnMeta colMeta : childOutputSchema) {
                            String targetColTableName = targetCol.getTableName();
                             if (colMeta.name.equalsIgnoreCase(targetCol.getColumnName()) &&
                                (targetColTableName == null || targetColTableName.isEmpty() || targetColTableName.equalsIgnoreCase(colMeta.tableName))) {
                                resultType = colMeta.type;
                                resultLength = colMeta.len;
                                targetFound = true;
                                break;
                            }
                        }
                        if (!targetFound) {
                             Logger.warn("Target column " + targetCol.toString() + " for MIN/MAX not found. Using default CHAR type/length.");
                        }
                    } else {
                         Logger.warn("MIN/MAX without a target column. Using default CHAR type/length.");
                    }
                    break;
                default:
                    Logger.warn("Unhandled aggregate function type: " + aggExpr.getFunction() + ". Using default CHAR type/length.");
                    resultType = ValueType.CHAR;
                    resultLength = 255;
                    break;
            }
            // Use null for tableName for aggregate results, as they don't belong to a specific base table.
            newSchema.add(new ColumnMeta(this.tableName, aggAlias, resultType, resultLength, currentOffset));
            currentOffset += resultLength;
        }
        return newSchema;
    }

    private ValueType getAggregateResultType(AggregateExpression aggExpr) {
        switch (aggExpr.getFunction()) {
            case COUNT:
                return ValueType.INTEGER;
            case AVG:
                return ValueType.FLOAT;
            case SUM:
                // SUM preserves the input type for INTEGER, returns FLOAT for others
                ArrayList<ColumnMeta> childSchema = child.outputSchema();
                for (ColumnMeta col : childSchema) {
                    if (col.name.equals(aggExpr.getTargetColumn().getColumnName())) {
                        return col.type == ValueType.INTEGER ? ValueType.INTEGER : ValueType.FLOAT;
                    }
                }
                return ValueType.FLOAT;
            case MIN:
            case MAX:
                // MIN/MAX preserve the input type
                childSchema = child.outputSchema();
                for (ColumnMeta col : childSchema) {
                    if (col.name.equals(aggExpr.getTargetColumn().getColumnName())) {
                        return col.type;
                    }
                }
                return ValueType.CHAR; // default fallback
            default:
                return ValueType.CHAR;
        }
    }
}