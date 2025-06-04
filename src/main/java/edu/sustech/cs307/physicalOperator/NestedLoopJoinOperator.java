package edu.sustech.cs307.physicalOperator;

import java.lang.classfile.instruction.TableSwitchInstruction;
import java.util.ArrayList;
import java.util.Collection;
// import java.util.NoSuchElementException; // Optional: if strict error handling is desired

import edu.sustech.cs307.DBEntry;
import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol; // Added import
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.tuple.JoinTuple; // Added import
import net.sf.jsqlparser.expression.Expression;

public class NestedLoopJoinOperator implements PhysicalOperator {

    private final PhysicalOperator leftOperator;
    private final PhysicalOperator rightOperator;
    private final Collection<Expression> joinConditions;

    private Tuple currentLeftTuple;
    private Tuple nextJoinedTuple; // Holds the next tuple to be returned by Current()
    private boolean leftExhausted;

    private ArrayList<ColumnMeta> outputSchemaCache;
    private TabCol[] joinTupleTabColSchemaCache;

    public NestedLoopJoinOperator(PhysicalOperator leftOperator, PhysicalOperator rightOperator,
            Collection<Expression> expr) {
        this.leftOperator = leftOperator;
        this.rightOperator = rightOperator;
        this.joinConditions = expr;

        this.currentLeftTuple = null;
        this.nextJoinedTuple = null;
        this.leftExhausted = false;
        this.outputSchemaCache = null;
        this.joinTupleTabColSchemaCache = null;
    }

    @Override
    public void Begin() throws DBException {
        if (leftOperator == null || rightOperator == null) {
            throw new RuntimeException("Child operator(s) cannot be null.");
        }
        leftOperator.Begin();
        rightOperator.Begin(); // Initial Begin for right operator

        this.currentLeftTuple = null;
        this.nextJoinedTuple = null;
        this.leftExhausted = false;

        // Fetch the first left tuple
        if (leftOperator.hasNext()) {
            leftOperator.Next();
            currentLeftTuple = leftOperator.Current();
            // Right operator is already Begun, but for NLJ, we might need to "rewind" it
            // per left tuple.
            // The findNextMatchingTuple logic will handle rewinding (Close then Begin)
            // rightOperator.
        } else {
            leftExhausted = true;
        }
    }

    @Override
    public boolean hasNext() throws DBException {
        nextJoinedTuple = null;
        // Try to find the next matching tuple and store it in nextJoinedTuple
        return findNextMatchingTuple();
    }

    private boolean findNextMatchingTuple() throws DBException {
        while (true) { // Outer loop for left tuples
            if (leftExhausted) {
                return false; // No more left tuples to process
            }

            if (currentLeftTuple == null) { // Need to advance left operator
                if (leftOperator.hasNext()) {
                    leftOperator.Next();
                    currentLeftTuple = leftOperator.Current();
                    // Reset/Rewind the right operator for the new left tuple
                    rightOperator.Close();
                    rightOperator.Begin();
                } else {
                    leftExhausted = true;
                    return false; // Left operator exhausted
                }
            }

            // Inner loop for right tuples
            while (rightOperator.hasNext()) {
                rightOperator.Next();
                Tuple rightTuple = rightOperator.Current();

                if (rightTuple == null && currentLeftTuple == null)
                    continue; // Both null, skip
                if (currentLeftTuple == null) { // Should not happen if leftExhausted is false
                    leftExhausted = true; // Safety break
                    return false;
                }
                if (rightTuple == null)
                    continue; // Skip null right tuple

                boolean flag = true;
                for (Expression condition : joinConditions) {
                    if (!currentLeftTuple.evaluateSingleTuple(rightTuple)) {
                        flag = false;
                    }
                }
                if (!flag) {
                    continue;
                }
                nextJoinedTuple = new JoinTuple(currentLeftTuple, rightTuple, getJoinTupleTabColSchema());
                return true;
            }

            // Right operator exhausted for the currentLeftTuple, so advance left.
            currentLeftTuple = null;
        }
    }

    @Override
    public void Next() throws DBException {
        // Assumes hasNext() has been called and returned true, populating
        // nextJoinedTuple.
        // This method consumes the tuple, so the next call to hasNext() will search
        // again.
        // if (nextJoinedTuple != null) {
        //     nextJoinedTuple = null;
        // }
        // If nextJoinedTuple is null, it implies hasNext() was false or Next() called
        // out of sequence.
        // No explicit error throwing here due to void return, relies on caller
        // contract.
    }

    @Override
    public Tuple Current() {
        // Returns the tuple found by the last successful call to hasNext().
        // If hasNext() was not called, or returned false, or Next() was called since,
        // this will be null.
        return nextJoinedTuple;
    }

    @Override
    public void Close() {
        if (leftOperator != null) {
            leftOperator.Close();
        }
        if (rightOperator != null) {
            rightOperator.Close();
        }
        // Reset internal state
        currentLeftTuple = null;
        nextJoinedTuple = null;
        leftExhausted = true;
        // outputSchemaCache and joinTupleTabColSchemaCache can remain for potential
        // re-Begin,
        // or be nulled out if strict resource cleanup per Close() is desired.
        // For simplicity, we let them persist if the operator instance is reused.
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        if (outputSchemaCache == null) {
            outputSchemaCache = new ArrayList<>();
            if (leftOperator != null && leftOperator.outputSchema() != null) {
                outputSchemaCache.addAll(leftOperator.outputSchema());
            }
            if (rightOperator != null && rightOperator.outputSchema() != null) {
                outputSchemaCache.addAll(rightOperator.outputSchema());
            }
        }
        return outputSchemaCache;
    }

    private TabCol[] getJoinTupleTabColSchema() throws DBException {
        if (this.joinTupleTabColSchemaCache == null) {
            ArrayList<ColumnMeta> combinedMeta = outputSchema();
            if (combinedMeta == null) {
                throw new RuntimeException("Combined schema is null in NestedLoopJoinOperator");
            }
            this.joinTupleTabColSchemaCache = new TabCol[combinedMeta.size()];
            for (int i = 0; i < combinedMeta.size(); i++) {
                ColumnMeta cm = combinedMeta.get(i);
                if (cm == null) {
                    throw new RuntimeException("Null ColumnMeta in combined schema at index " + i);
                }
                // CRITICAL ASSUMPTION: ColumnMeta must provide a way to get table and column
                // names.
                // Replace .getTableName() and .getColumnName() with actual methods from your
                // ColumnMeta class.
                String tableName = cm.tableName; // e.g., cm.getRelationName() or cm.getSourceTable()
                String columnName = cm.name; // e.g., cm.getName()

                if (tableName == null || columnName == null) {
                    // Fallback or error if names are not directly available
                    // For example, if ColumnMeta only has columnName and table is implicit or
                    // derived
                    // This part highly depends on your ColumnMeta and TabCol design.
                    // As a placeholder, if tableName is null, try to use a generic or derived name.
                    // This is a common issue if ColumnMeta is for projected columns without
                    // explicit table source.
                    // For now, we'll throw an error to highlight the need for proper mapping.
                    throw new RuntimeException("ColumnMeta at index " + i +
                            " (col: " + (columnName != null ? columnName : "UNKNOWN") +
                            ") does not provide sufficient information to create a TabCol. " +
                            "Ensure ColumnMeta has accessible table and column names.");
                }
                this.joinTupleTabColSchemaCache[i] = new TabCol(tableName, columnName);
            }
        }
        return this.joinTupleTabColSchemaCache;
    }

    private boolean evaluateCondition(Tuple leftTuple, Tuple rightTuple, Collection<Expression> conditions)
            throws DBException {
        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions means Cartesian product (or cross join)
        }

        // TODO: Placeholder for actual expression evaluation.
        // This requires a robust expression evaluator that can:
        // 1. Understand JSQLParser's Expression objects.
        // 2. Resolve column references within the expressions against the leftTuple and
        // rightTuple.
        // (e.g., "tableA.col1 = tableB.col2")
        // 3. Access values from the tuples using your Value system.
        // 4. Perform comparisons and logical operations.
        //
        // For now, this placeholder assumes that if any join conditions exist,
        // they are met. This is NOT correct for a functional database.
        System.err.println("WARNING: NestedLoopJoinOperator.evaluateCondition is a placeholder. " +
                "It currently returns TRUE for any non-empty join conditions. " +
                "Actual expression evaluation logic needs to be implemented.");
        return true;
    }
}
