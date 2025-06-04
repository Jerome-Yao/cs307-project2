package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.BPlusTree.BPlusTree;
import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.index.BPlusTreeIndexManager;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.record.Record;
import edu.sustech.cs307.record.RecordFileHandle;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BPlusTreeIndexScanOperator implements PhysicalOperator {
    private final String tableName;
    private final String columnName;
    private final Value searchKey;
    private final Value startKey;
    private final Value endKey;
    private final DBManager dbManager;
    private final TableMeta table;
    
    private TableMeta tableMeta;
    private RecordFileHandle fileHandle;
    private Iterator<RID> ridIterator;
    private Tuple currentTuple;
    private boolean isOpen = false;
    private boolean isRangeQuery = false;
    
    // Constructor for exact match
    public BPlusTreeIndexScanOperator(String tableName, String columnName, 
                                     Value searchKey, DBManager dbManager) throws DBException {
        this.tableName = tableName;
        this.columnName = columnName;
        this.searchKey = searchKey;
        this.startKey = null;
        this.endKey = null;
        this.dbManager = dbManager;
        this.table = dbManager.getMetaManager().getTable(tableName);
        // this.indexManager = indexManager;
        this.isRangeQuery = false;
    }
    
    // Constructor for range query
    public BPlusTreeIndexScanOperator(String tableName, String columnName,
                                     Value startKey, Value endKey, DBManager dbManager)throws DBException {
        this.tableName = tableName;
        this.columnName = columnName;
        this.searchKey = null;
        this.startKey = startKey;
        this.endKey = endKey;
        this.dbManager = dbManager;
        this.table = dbManager.getMetaManager().getTable(tableName);
        // this.indexManager = indexManager;
        this.isRangeQuery = true;
    }
    
    @Override
    public void Begin() throws DBException {
        tableMeta = dbManager.getMetaManager().getTable(tableName);
        if (tableMeta == null) {
            throw new DBException("Table " + tableName + " does not exist.");
        }
        fileHandle = dbManager.getRecordManager().OpenFile(tableName);
    
        List<RID> rids;
        BPlusTree tree = table.getBTreeIndex(columnName);
        if (isRangeQuery) {
            // BPlusTree tree = table.getBTreeIndex(columnName);
            // 调用 B+ 树索引管理器的范围查询方法
            rids = tree.rangeSearch(startKey, endKey);
        } else {
            // 调用 B+ 树索引管理器的精确查询方法
            rids = tree.search(searchKey);
        }
        ridIterator = rids.iterator(); // 初始化 RID 迭代器
        isOpen = true;
    }
    
    @Override
    public boolean hasNext() throws DBException {
        return isOpen && ridIterator != null && ridIterator.hasNext();
    }
    
    @Override
    public void Next() throws DBException {
        if (!hasNext()) {
            currentTuple = null;
            return;
        }
        
        RID rid = ridIterator.next();
        Record record = fileHandle.GetRecord(rid);
        currentTuple = new TableTuple(tableName, tableMeta, record, rid);
    }
        
    
    @Override
    public Tuple Current() {
        return currentTuple;
    }
    
    @Override
    public void Close() {
        if (fileHandle != null) {
            try {
                dbManager.getRecordManager().CloseFile(fileHandle);
            } catch (DBException e) {
                // Log error but don't throw
                e.printStackTrace();
            }
        }
        isOpen = false;
        ridIterator = null;
        currentTuple = null;
    }
    
    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        return tableMeta != null ? tableMeta.columns_list : new ArrayList<>();
    }
}