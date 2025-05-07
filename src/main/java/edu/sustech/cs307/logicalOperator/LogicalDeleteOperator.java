package edu.sustech.cs307.logicalOperator;

import net.sf.jsqlparser.expression.Expression;

import java.util.Collections;

public class LogicalDeleteOperator extends LogicalOperator {
    private final String tableName;
    private final Expression expressions;

    public LogicalDeleteOperator(LogicalOperator child, String tableName, Expression expression) {
        super(Collections.singletonList(child));

        this.tableName = tableName;
        this.expressions = expression;
    }

    public String toString() {
        return "DeleteOperator(table=" + tableName + ", expressions=" + expressions
                + ")\n ├── " + childern.get(0);
    }
}
