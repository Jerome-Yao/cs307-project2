package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TabCol;
import edu.sustech.cs307.sort.TupleComparator;
import edu.sustech.cs307.tuple.Tuple;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.schema.Column;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Physical operator for sorting tuples based on ORDER BY clause.
 * Implements the Volcano model with Begin(), hasNext(), Next(), Current(), Close() interface.
 */
public class SortOperator implements PhysicalOperator {
    private final PhysicalOperator child;
    private final List<OrderByElement> orderByElements;
    private final List<TabCol> orderByColumns;
    
    private boolean isOpen = false;
    private List<Tuple> sortedTuples;
    private Iterator<Tuple> resultIterator;
    private Tuple currentTuple;

    public SortOperator(PhysicalOperator child, List<OrderByElement> orderByElements) {
        this.child = child;
        this.orderByElements = orderByElements;
        this.orderByColumns = extractColumns(orderByElements);
    }

    private List<TabCol> extractColumns(List<OrderByElement> orderByElements) {
        List<TabCol> columns = new ArrayList<>();
        for (OrderByElement element : orderByElements) {
            if (element.getExpression() instanceof Column column) {
                String tableName = column.getTable() != null ? column.getTable().getName() : "";
                String columnName = column.getColumnName();
                columns.add(new TabCol(tableName, columnName));
            } else {
                throw new UnsupportedOperationException("ORDER BY only supports column references");
            }
        }
        return columns;
    }

    @Override
    public void Begin() throws DBException {
        child.Begin();
        isOpen = true;
        
        // Read all tuples from child operator
        sortedTuples = new ArrayList<>();
        while (child.hasNext()) {
            child.Next();
            Tuple tuple = child.Current();
            if (tuple != null) {
                sortedTuples.add(tuple);
            }
        }
        
        // Sort the tuples using our comparator
        TupleComparator comparator = new TupleComparator(orderByElements, orderByColumns);
        Collections.sort(sortedTuples, comparator);
        
        // Initialize iterator
        resultIterator = sortedTuples.iterator();
        currentTuple = null;
    }

    @Override
    public boolean hasNext() throws DBException {
        if (!isOpen) return false;
        return resultIterator != null && resultIterator.hasNext();
    }

    @Override
    public void Next() throws DBException {
        if (!isOpen || resultIterator == null) {
            currentTuple = null;
            return;
        }
        
        if (resultIterator.hasNext()) {
            currentTuple = resultIterator.next();
        } else {
            currentTuple = null;
        }
    }

    @Override
    public Tuple Current() {
        return currentTuple;
    }

    @Override
    public void Close() {
        child.Close();
        isOpen = false;
        sortedTuples = null;
        resultIterator = null;
        currentTuple = null;
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        // Sort operator preserves the schema of its child
        return child.outputSchema();
    }
}
