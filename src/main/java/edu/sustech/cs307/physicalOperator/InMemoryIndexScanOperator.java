package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.index.InMemoryOrderedIndex;
import edu.sustech.cs307.record.Record;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RecordFileHandle;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.BPlusTree.BPlusTree;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TableMeta;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

public class InMemoryIndexScanOperator implements PhysicalOperator {

    private InMemoryOrderedIndex index;
    private BPlusTree bPlusTree; 
    private String tableName;
    private DBManager dbManager;
    private TableMeta tableMeta;
    private RecordFileHandle fileHandle;
    private Record currentRecord;

    private int currentPageNum;
    private int currentSlotNum;
    private int totalPages;
    private int recordsPerPage;
    private boolean isOpen = false;
    public InMemoryIndexScanOperator(InMemoryOrderedIndex index, int degree) {
        this.index = index;
        this.bPlusTree = new BPlusTree(degree);
        Set<Map.Entry<Value, RID>> entries = index.getTreeMapEntries();
        for (Map.Entry<Value, RID> entry : entries) {
            bPlusTree.insert(entry.getKey(), entry.getValue()); 
        }
    }

    @Override
    public boolean hasNext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasNext'");
    }

    @Override
    public void Begin() throws DBException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Begin'");
    }

    @Override
    public void Next() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Next'");
    }

    @Override
    public Tuple Current() { // Return Tuple
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Current'");
    }

    @Override
    public void Close() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Close'");
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        // TODO Auto-generated method stub
        return null;
    }
}
