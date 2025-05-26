package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.tuple.ProjectTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.meta.TabCol;

import java.util.ArrayList;
import java.util.List;

public class ProjectOperator implements PhysicalOperator {
    private PhysicalOperator child;
    private List<TabCol> outputSchema; // Use bounded wildcard
    private Tuple currentTuple;

    public ProjectOperator(PhysicalOperator child, List<TabCol> outputSchema) { // Use bounded wildcard
        this.child = child;
        this.outputSchema = outputSchema;
        boolean scanAll = false;
        for (TabCol tabCol : outputSchema) {
            if (tabCol.getTableName().equals("*")) {
                scanAll = true;
                break;
            }
        }
        if (scanAll) {
            List<TabCol> newOutputSchema = new ArrayList<>();
            for (ColumnMeta tabCol : child.outputSchema()) {
                newOutputSchema.add(new TabCol(tabCol.tableName, tabCol.name));
            }
            this.outputSchema = newOutputSchema;
        }
        // PhysicalOperator scanOperator= this.child;
        // if (!(scanOperator instanceof SeqScanOperator)) {
        //     scanOperator=scanOperator.getChild();
        // }

        // if (this.outputSchema.get(0).getTableName().equals("*")) {
        //     List<TabCol> newOutputSchema = new ArrayList<>();
        //     for (ColumnMeta tabCol : child.outputSchema()) {
        //         newOutputSchema.add(new TabCol(tabCol.tableName, tabCol.name));
        //     }
        //     this.outputSchema = newOutputSchema;
        // }
    }

    @Override
    public boolean hasNext() throws DBException {
        return child.hasNext();
    }

    @Override
    public void Begin() throws DBException {
        child.Begin();
    }

    @Override
    public void Next() throws DBException {
        if (hasNext()) {
            child.Next();
            Tuple inputTuple = child.Current();
            if (inputTuple != null) {

                currentTuple = new ProjectTuple(inputTuple, outputSchema); // Create ProjectTuple
            } else {
                currentTuple = null;
            }
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
        currentTuple = null;
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        ArrayList<ColumnMeta> projectedSchemaResult = new ArrayList<>();
        ArrayList<ColumnMeta> childSchema = this.child.outputSchema();

        // this.outputSchema is the List<TabCol> field of ProjectOperator,
        // representing the columns to be projected, in the desired order, with desired
        // names/aliases.
        for (TabCol desiredOutputColumn : this.outputSchema) {
            ColumnMeta foundChildColumnMeta = null;
            for (ColumnMeta childCol : childSchema) {
                // Match primarily by column name (which handles aliases from aggregates
                // effectively,
                // as AggregateOperator's output schema uses aliases for aggregate function
                // results).
                if (childCol.name.equals(desiredOutputColumn.getColumnName())) {
                    // If the desired column projection specifies a table name,
                    // it should match the child's column table name for a more precise match.
                    // If desiredOutputColumn.tableName is null/empty (common for aliases or
                    // unambiguous columns),
                    // the name match is sufficient.
                    if (desiredOutputColumn.getTableName() == null ||
                            desiredOutputColumn.getTableName().isEmpty() ||
                            desiredOutputColumn.getTableName().equals("*") || // Should not happen if constructor *
                                                                              // resolved
                            desiredOutputColumn.getTableName().equalsIgnoreCase(childCol.tableName)) {
                        foundChildColumnMeta = childCol;
                        break; // Found the corresponding column meta from child
                    }
                }
            }

            if (foundChildColumnMeta != null) {
                // Determine the final table name for the output ColumnMeta.
                // Prefer the table name from the projection list if it's specific.
                // Otherwise, use the table name from the found child column.
                String finalTableName = desiredOutputColumn.getTableName();
                if (finalTableName == null || finalTableName.isEmpty() || finalTableName.equals("*")) {
                    finalTableName = foundChildColumnMeta.tableName;
                }

                projectedSchemaResult.add(new ColumnMeta(
                        finalTableName,
                        desiredOutputColumn.getColumnName(), // Use the name from the projection list (authoritative for
                                                             // alias)
                        foundChildColumnMeta.type,
                        foundChildColumnMeta.len, // Corrected field name
                        projectedSchemaResult.size() // Ordinal position in this new projected schema
                ));
            } else {
                // This means a column specified in the SELECT list (and thus in
                // this.outputSchema)
                // was not found in the output of the child operator. This indicates an issue
                // upstream (e.g., logical planning error) or a query for a non-existent column.
            }
        }
        return projectedSchemaResult;
    }
}
