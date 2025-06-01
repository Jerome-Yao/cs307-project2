package edu.sustech.cs307.logicalOperator;

import java.util.Collections;
import java.util.List;

import edu.sustech.cs307.tuple.Tuple;
import net.sf.jsqlparser.expression.Expression;

public class LogicalFilterOperator extends LogicalOperator {

    private final Expression condition;
    private final LogicalOperator child;
    private List<Tuple> subQueryTuples = null;
    private boolean not = false;

    public LogicalFilterOperator(LogicalOperator child, Expression condition) {
        super(Collections.singletonList(child));
        this.child = child;
        this.condition = condition;
    }

    public LogicalFilterOperator(
            LogicalOperator child,
            Expression condition,
            List<Tuple> subQueryTuples, boolean not) {
        super(Collections.singletonList(child));
        this.child = child;
        this.condition = condition;
        this.subQueryTuples = subQueryTuples;
        this.not = not;
    }

    public LogicalOperator getChild() {
        return child;
    }

    public Expression getWhereExpr() {
        return condition;
    }

    public List<Tuple> getSubQueryTuples() {
        return subQueryTuples;
    }

    public boolean isNot() {
        return not;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String nodeHeader = "LogicalFilterOperator(condition=" + condition + ")";
        LogicalOperator child = getChildren().get(0); // 获取过滤的子节点

        // 拆分子节点的多行字符串
        String[] childLines = child.toString().split("\\R");

        // 当前节点
        sb.append(nodeHeader);

        // 子节点处理
        if (childLines.length > 0) {
            sb.append("\n    └── ").append(childLines[0]);
            for (int i = 1; i < childLines.length; i++) {
                sb.append("\n    ").append(childLines[i]);
            }
        }

        return sb.toString();
    }
}
