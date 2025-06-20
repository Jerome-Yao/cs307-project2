package edu.sustech.cs307.physicalOperator;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.sustech.cs307.exception.DBException;

import org.pmw.tinylog.Logger;

public class FilterOperator implements PhysicalOperator {
    private PhysicalOperator child;
    private Expression whereExpr;
    private Tuple currentTuple;
    private boolean isOpen = false;
    // 标记是否已经准备好下一个元组
    private boolean readyForNext = false;
    private List<Tuple> subQueryTuples = null; // 用于存储子查询的结果
    private boolean subQueryType = false; // 用于标记是否是子查询类型
    private boolean not = true;

    public FilterOperator(PhysicalOperator child, Expression whereExpr) {
        this.child = child;
        this.whereExpr = whereExpr;
    }

    public FilterOperator(PhysicalOperator child, Expression whereExpr, List<Tuple> subQueryTuples, boolean not) {
        this.child = child;
        this.whereExpr = whereExpr;
        this.subQueryTuples = subQueryTuples;
        this.subQueryType = true;
        this.not = not;
    }

    public FilterOperator(PhysicalOperator child, Collection<Expression> whereExpr) {
        this.child = child;
        // 只使用第一个表达式，简化逻辑
        this.whereExpr = whereExpr.iterator().next();
    }

    @Override
    public void Begin() throws DBException {
        Logger.debug("FilterOperator.Begin() 被调用");
        child.Begin();
        isOpen = true;
        currentTuple = null;
        readyForNext = false;

        // 在Begin后我们不主动查找第一个元组，而是等待hasNext()调用
    }

    @Override
    public boolean hasNext() throws DBException {
        Logger.debug("FilterOperator.hasNext() 被调用");
        if (!isOpen) {
            return false;
        }

        // 如果我们还没有准备好下一个元组，就尝试找一个
        if (!readyForNext) {
            return findNext();
        }

        // 如果已经准备好，且currentTuple不为null，则说明有下一个
        return currentTuple != null;
    }

    @Override
    public void Next() throws DBException {
        Logger.debug("FilterOperator.Next() 被调用");
        if (!isOpen) {
            return;
        }

        // 如果没有准备好，先准备
        if (!readyForNext) {
            hasNext(); // 这会调用findNext()来准备下一个元组
        }

        // 清除已准备状态，表示需要准备下一个元组
        readyForNext = false;
    }

    /**
     * 查找下一个符合条件的元组，并准备好它
     * 
     * @return 如果找到则返回true，否则返回false
     */
    private boolean findNext() throws DBException {
        // 标记没有找到合适的元组
        currentTuple = null;

        // 循环直到找到匹配的元组或没有更多元组
        while (child.hasNext()) {
            child.Next();
            Tuple tuple = child.Current();

            if (this.subQueryType) {
                // 如果是子查询类型，检查子查询元组是否为空
                if (subQueryTuples == null || subQueryTuples.isEmpty()) {
                    Logger.debug("FilterOperator子查询元组为空，跳过当前元组");
                    continue;
                }
                if (not) {
                    boolean found = false;
                    for (Tuple subTuple : subQueryTuples) {
                        if (tuple.evaluateSingleTuple(subTuple)) {
                            found = true;
                        }
                    }
                    if (found) {
                        continue; // get next
                    } else {
                        currentTuple = tuple;
                        readyForNext = true;
                        return true;
                    }
                }
                for (Tuple subTuple : subQueryTuples) {
                    if (tuple.evaluateSingleTuple(subTuple)) {
                        currentTuple = tuple;
                        readyForNext = true;
                        return true;
                    }
                }
            } else if (tuple != null && tuple.eval_expr(whereExpr)) {
                Logger.debug("FilterOperator找到匹配的元组: " + tuple);
                currentTuple = tuple;
                readyForNext = true;
                return true;
            }
        }

        // 没有找到匹配的元组
        Logger.debug("FilterOperator没有找到更多匹配的元组");
        return false;
    }

    @Override
    public Tuple Current() {
        Logger.debug("FilterOperator.Current() 被调用，返回: " + currentTuple);
        return currentTuple;
    }

    public RID getCurrentRID() {
        if (child instanceof SeqScanOperator) {
            // 如果子操作符是SeqScanOperator，获取当前RID
            return ((TableTuple) ((SeqScanOperator) child).Current()).getRID();
        } else {
            throw new RuntimeException("FilterOperator仅支持SeqScanOperator作为子操作符");
        }
    }

    @Override
    public void Close() {
        if (child != null) {
            child.Close();
        }
        isOpen = false;
        currentTuple = null;
        readyForNext = false;
        Logger.debug("FilterOperator.Close() 被调用");
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        return child.outputSchema();
    }

}
