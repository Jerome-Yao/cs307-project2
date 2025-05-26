package edu.sustech.cs307.aggregate;

import edu.sustech.cs307.meta.TabCol;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SelectItem;


import java.util.ArrayList;
import java.util.List;

public class AggregateParser {
    
    public static List<AggregateExpression> parseAggregatesFromSelectItems(List<SelectItem<?>> selectItems, String tableName) {
        List<AggregateExpression> aggregates = new ArrayList<>();
        
        for (SelectItem<?> item : selectItems) {
            Expression expr = item.getExpression();
            if (expr instanceof Function function) {
                AggregateExpression aggExpr = parseFunction(function, tableName);
                if (aggExpr != null) {
                    aggregates.add(aggExpr);
                }
            }
        }
        
        return aggregates;
    }
    
    public static boolean hasAggregateFunction(List<SelectItem<?>> selectItems) {
        for (SelectItem<?> item : selectItems) {
            Expression expr = item.getExpression();
            if (expr instanceof Function function) {
                if (isAggregateFunction(function.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static AggregateExpression parseFunction(Function function, String tableName) {
        String functionName = function.getName().toUpperCase();
        
        if (!isAggregateFunction(functionName)) {
            return null;
        }
        
        AggregateFunction aggFunc = AggregateFunction.valueOf(functionName);
        
        // Get target column
        TabCol targetColumn = null;
        if (function.getParameters() != null && !function.getParameters().isEmpty()) {
            Expression firstParam = function.getParameters().get(0);
            if (firstParam instanceof Column column) {
                String colName = column.getColumnName();
                targetColumn = new TabCol(tableName, colName);
            }
        }
        
        // Handle COUNT(*) case
        if (aggFunc == AggregateFunction.COUNT && targetColumn == null) {
            targetColumn = new TabCol(tableName, "*");
        }
        
        if (targetColumn == null) {
            return null;
        }
        
        // Generate alias
        String alias = functionName + "_" + targetColumn.getColumnName();
        
        // Check for DISTINCT
        boolean distinct = function.isDistinct();
        
        return new AggregateExpression(aggFunc, targetColumn, alias, distinct);
    }
    
    private static boolean isAggregateFunction(String functionName) {
        try {
            AggregateFunction.valueOf(functionName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}