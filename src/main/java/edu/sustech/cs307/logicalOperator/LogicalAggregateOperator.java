package edu.sustech.cs307.logicalOperator;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import edu.sustech.cs307.aggregate.AggregateExpression;

import java.util.Collections;
import java.util.List;

public class LogicalAggregateOperator extends LogicalOperator {
    private final String tableName;
    private final ExpressionList<Expression> groupByExpressions;
    private final List<AggregateExpression> aggregateExpressions;

    public LogicalAggregateOperator(LogicalOperator child, String tableName, 
                                  ExpressionList<Expression> groupByExpressions,
                                  List<AggregateExpression> aggregateExpressions) {
        super(Collections.singletonList(child));
        this.tableName = tableName;
        this.groupByExpressions = groupByExpressions;
        this.aggregateExpressions = aggregateExpressions;
    }

    public ExpressionList<Expression> getGroupByExpressions() {
        return groupByExpressions;
    }

    public List<AggregateExpression> getAggregateExpressions() {
        return aggregateExpressions;
    }

    public LogicalOperator getChild() {
        return childern.get(0);
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "AggregateOperator(groupBy=" + groupByExpressions + 
               ", aggregates=" + aggregateExpressions + ")\n ├── " + getChild();
    }
}