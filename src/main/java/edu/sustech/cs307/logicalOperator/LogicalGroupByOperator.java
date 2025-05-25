package edu.sustech.cs307.logicalOperator;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.util.Collections;

public class LogicalGroupByOperator extends LogicalOperator {
    private final String tableName;
    private final ExpressionList<Expression> groupByExpressions;

    public LogicalGroupByOperator(LogicalOperator child, String tableName, ExpressionList<Expression> groupByExpressions) {
        super(Collections.singletonList(child));
        this.groupByExpressions = groupByExpressions;
        this.tableName = tableName;
    }

    public ExpressionList<Expression> getGroupByExpressions() {
        return groupByExpressions;
    }

    public LogicalOperator getChild() {
        return childern.get(0);
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "GroupByOperator(groupBy=" + groupByExpressions + ")\n ├── " + getChild();
    }
}
