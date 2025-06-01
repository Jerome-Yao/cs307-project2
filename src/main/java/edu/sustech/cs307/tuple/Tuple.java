package edu.sustech.cs307.tuple;

import java.util.ArrayList;
import java.util.List;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueComparer;
import edu.sustech.cs307.value.ValueType;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.LateralSubSelect;

public abstract class Tuple {
    public abstract Value getValue(TabCol tabCol) throws DBException;

    public abstract TabCol[] getTupleSchema();

    public abstract Value[] getValues() throws DBException;

    public boolean eval_expr(Expression expr) throws DBException {
        return evaluateCondition(this, expr);
    }

    public boolean eval_expr(List<Tuple> targetTuples, Expression whereExpr) throws DBException {
        for (Tuple tuple : targetTuples) {
            if (evaluateCondition(tuple, whereExpr)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({ "unchecked", "deprecation" }) // For ExpressionList.getExpressions() and its raw type
    private boolean evaluateCondition(Tuple tuple, Expression whereExpr) throws DBException {
        // todo: add Or condition
        if (whereExpr instanceof AndExpression andExpr) {
            // Recursively evaluate left and right expressions
            return evaluateCondition(tuple, andExpr.getLeftExpression())
                    && evaluateCondition(tuple, andExpr.getRightExpression());
        } else if (whereExpr instanceof OrExpression orExpr) {
            // Recursively evaluate left and right expressions
            return evaluateCondition(tuple, orExpr.getLeftExpression())
                    || evaluateCondition(tuple, orExpr.getRightExpression());
        } else if (whereExpr instanceof BinaryExpression binaryExpression) {
            return evaluateBinaryExpression(tuple, binaryExpression);
        } else if (whereExpr instanceof InExpression inExpr) {
            Value leftValue = tuple.evaluateExpression(inExpr.getLeftExpression());

            if (leftValue == null || leftValue.value == null) {
                return false;
            }

            Expression rightOperand = inExpr.getRightExpression(); // Use getRightExpression()

            if (rightOperand instanceof ExpressionList expressionList) {
                List<Expression> items = expressionList.getExpressions(); // This is deprecated and returns raw List
                boolean foundMatch = false;
                boolean listContainsNull = false;

                if (items == null || items.isEmpty()) {
                    return inExpr.isNot();
                }

                for (Expression itemExpr : items) {
                    Value rightValue = tuple.evaluateExpression(itemExpr);
                    if (rightValue == null || rightValue.value == null) {
                        listContainsNull = true;
                        continue;
                    }
                    if (ValueComparer.compare(leftValue, rightValue) == 0) {
                        foundMatch = true;
                        break;
                    }
                }

                if (inExpr.isNot()) { // NOT IN logic
                    if (foundMatch)
                        return false;
                    if (listContainsNull)
                        return false;
                    return true;
                } else { // IN logic
                    if (foundMatch)
                        return true;
                    return false;
                }
            } else {
                throw new RuntimeException("Unsupported IN expression");
            }
        } else {
            return true; // For non-binary and non-AND expressions, just return true for now
        }
    }

    private boolean evaluateBinaryExpression(Tuple tuple, BinaryExpression binaryExpr) {
        Expression leftExpr = binaryExpr.getLeftExpression();
        Expression rightExpr = binaryExpr.getRightExpression();
        String operator = binaryExpr.getStringExpression();
        Value leftValue = null;
        Value rightValue = null;

        try {
            if (leftExpr instanceof Column leftColumn) {
                // leftValue = tuple.getValue(new TabCol(leftColumn.getTableName(),
                // leftColumn.getColumnName()));
                // get table name
                String table_name = leftColumn.getTableName();
                if (tuple instanceof TableTuple) {
                    TableTuple tableTuple = (TableTuple) tuple;
                    table_name = tableTuple.getTableName();
                }
                leftValue = tuple.getValue(new TabCol(table_name, leftColumn.getColumnName()));
                if (leftValue.type == ValueType.CHAR) {
                    leftValue = new Value(leftValue.toString());
                }
            } else {
                leftValue = getConstantValue(leftExpr); // Handle constant left value
            }

            if (rightExpr instanceof Column rightColumn) {
                // rightValue = tuple.getValue(new TabCol(rightColumn.getTableName(),
                // rightColumn.getColumnName()));
                // get table name
                String table_name = rightColumn.getTableName();
                if (tuple instanceof TableTuple && table_name == null) {
                    TableTuple tableTuple = (TableTuple) tuple;
                    table_name = tableTuple.getTableName();
                }
                rightValue = tuple.getValue(new TabCol(table_name, rightColumn.getColumnName()));
            } else {
                rightValue = getConstantValue(rightExpr); // Handle constant right value

            }

            if (leftValue == null || rightValue == null) {
                return false;
            }

            int comparisonResult = ValueComparer.compare(leftValue, rightValue);
            if (operator.equals("=")) {
                return comparisonResult == 0;
            } else if (operator.equals("<")) {
                return comparisonResult < 0;
            } else if (operator.equals(">")) {
                return comparisonResult > 0;
            } else if (operator.equals("<=")) {
                return comparisonResult <= 0;
            } else if (operator.equals(">=")) {
                return comparisonResult >= 0;
            }

        } catch (DBException e) {
            e.printStackTrace(); // Handle exception properly
        }
        return false;
    }

    public boolean evaluateSingleTuple(Tuple tuple) {
        Value rightValue = null;
        try {
            rightValue = tuple.getValues()[0];
            List<Value> typeMatchValues = new ArrayList<>();
            for (Value value : this.getValues()) {
                if (value.type != rightValue.type) {
                    // If types do not match, skip this comparison
                    continue;
                } else {
                    typeMatchValues.add(value);
                }
            }
            if (typeMatchValues.size() == 1 || rightValue == null) {
                return false;
            }

            for (Value leftValue : typeMatchValues) {
                // Compare each value in the tuple with the right value
                int comparisonResult = ValueComparer.compare(leftValue, rightValue);
                if (comparisonResult == 0) {
                    return true; // InExpression matches if any value matches
                }
            }

        } catch (DBException e) {
            e.printStackTrace(); // Handle exception properly
        }
        return false;
    }

    private Value getConstantValue(Expression expr) {
        if (expr instanceof StringValue) {
            return new Value(((StringValue) expr).getValue(), ValueType.CHAR);
        } else if (expr instanceof DoubleValue) {
            return new Value(((DoubleValue) expr).getValue(), ValueType.FLOAT);
        } else if (expr instanceof LongValue) {
            return new Value(((LongValue) expr).getValue(), ValueType.INTEGER);
        }
        return null; // Unsupported constant type
    }

    public Value evaluateExpression(Expression expr) throws DBException {
        if (expr instanceof StringValue) {
            return new Value(((StringValue) expr).getValue(), ValueType.CHAR);
        } else if (expr instanceof DoubleValue) {
            return new Value(((DoubleValue) expr).getValue(), ValueType.FLOAT);
        } else if (expr instanceof LongValue) {
            return new Value(((LongValue) expr).getValue(), ValueType.INTEGER);
        } else if (expr instanceof Column) {
            Column col = (Column) expr;
            return getValue(new TabCol(col.getTableName(), col.getColumnName()));
        } else {
            throw new DBException(ExceptionTypes.UnsupportedExpression(expr));
        }
    }

}
