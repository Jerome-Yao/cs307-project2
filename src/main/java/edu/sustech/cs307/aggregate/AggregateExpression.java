package edu.sustech.cs307.aggregate;

import edu.sustech.cs307.meta.TabCol;

public class AggregateExpression {
    private final AggregateFunction function;
    private final TabCol targetColumn;
    private final String alias;
    private final boolean distinct;

    public AggregateExpression(AggregateFunction function, TabCol targetColumn, String alias, boolean distinct) {
        this.function = function;
        this.targetColumn = targetColumn;
        this.alias = alias;
        this.distinct = distinct;
    }

    public AggregateExpression(AggregateFunction function, TabCol targetColumn, String alias) {
        this(function, targetColumn, alias, false);
    }

    public AggregateExpression(AggregateFunction function, TabCol targetColumn) {
        this(function, targetColumn, function.name() + "_" + targetColumn.getColumnName(), false);
    }

    public AggregateFunction getFunction() {
        return function;
    }

    public TabCol getTargetColumn() {
        return targetColumn;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public String toString() {
        return function + "(" + (distinct ? "DISTINCT " : "") + targetColumn + ")" + 
               (alias != null ? " AS " + alias : "");
    }
}