package edu.sustech.cs307.physicalOperator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pmw.tinylog.Logger;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.Tuple;
import net.sf.jsqlparser.schema.Column;

public class GroupByOperator implements PhysicalOperator {
    private final PhysicalOperator child;
    private List<TabCol> groupByColumns = new ArrayList<>();
    private boolean isOpen = false;
    private Iterator<Map.Entry<List<Object>, List<TableTuple>>> groupIterator;
    private Iterator<TableTuple> groupInnerIterator;
    private TableTuple currentTuple;

    public GroupByOperator(PhysicalOperator child, List<Column> groupByColumns, String tableName) {
        this.child = child;
        for (Column col : groupByColumns) {
            this.groupByColumns.add(new TabCol(tableName, col.getColumnName()));
        }
    }

    @Override
    public void Begin() throws DBException {
        try {
            Logger.debug("GroupByOperator.Begin() 被调用");
            child.Begin();
            isOpen = true;
            Map<List<Object>, List<TableTuple>> groupMap = new LinkedHashMap<>();

            while (child.hasNext()) {
                child.Next();
                TableTuple t = (TableTuple) child.Current();
                if (t == null)
                    continue;

                List<Object> key = new ArrayList<>();
                for (TabCol idx : groupByColumns) {
                    key.add(t.getValue(idx));
                }

                groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
            }
            groupIterator = groupMap.entrySet().iterator();
            if (!groupIterator.hasNext()) {
                isOpen = false;
                return;
            }
            groupInnerIterator = groupIterator.next().getValue().iterator();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to group by: " + e.getMessage() + "\n");
        }

    }

    @Override
    public boolean hasNext() throws DBException {
        if (!isOpen)
            return false;
        if (groupIterator.hasNext() || groupInnerIterator.hasNext()) {
            return true;
        }
        return false;
    }

    @Override
    public void Next() throws DBException {
        Logger.debug("GroupByOperator.Next() 被调用");
        if (!isOpen)
            return;
        // if (groupInnerIterator == null) {
        // isOpen = false;
        // return;
        // }
        if (groupInnerIterator.hasNext()) {
            currentTuple = groupInnerIterator.next();
        } else if (groupIterator.hasNext()) {
            groupInnerIterator = groupIterator.next().getValue().iterator();
            currentTuple = groupInnerIterator.next();
        } else {

        }
    }

    @Override
    public Tuple Current() {
        if (currentTuple == null) {
            return null;
        }
        // 使用 TableTuple 暂存 group key 和对应 group 的所有元组
        return currentTuple;
    }

    @Override
    public void Close() {
        child.Close();
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        return child.outputSchema();
    }

}