package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.record.Record;
import edu.sustech.cs307.record.RecordFileHandle;
import edu.sustech.cs307.index.IndexHandle;
import edu.sustech.cs307.index.IndexManager;

import java.util.ArrayList;
import java.util.List;

/**
 * B+树索引扫描操作符
 * 用于通过索引高效查找满足条件的记录
 */
public class IndexScanOperator implements PhysicalOperator {
    private String tableName;
    private String columnName;
    private Object searchKey;
    private ComparisonType comparisonType;
    private DBManager dbManager;
    private TableMeta tableMeta;
    private RecordFileHandle fileHandle;
    private IndexHandle indexHandle;
    private List<RID> resultRIDs;
    private int currentIndex;
    private boolean isOpen = false;

    public enum ComparisonType {
        EQUAL,
        LESS_THAN,
        GREATER_THAN,
        LESS_EQUAL,
        GREATER_EQUAL
    }

    public IndexScanOperator(String tableName, String columnName, Object searchKey, 
                           ComparisonType comparisonType, DBManager dbManager) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.searchKey = searchKey;
        this.comparisonType = comparisonType;
        this.dbManager = dbManager;
        try {
            this.tableMeta = dbManager.getMetaManager().getTable(tableName);
        } catch (DBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Begin() throws DBException {
        try {
            // 打开表文件
            fileHandle = dbManager.getRecordManager().OpenFile(tableName);
            
            // 打开索引文件
            IndexManager indexManager = dbManager.getIndexManager();
            String indexName = tableName + "_" + columnName + "_index";
            indexHandle = indexManager.OpenIndex(indexName);
            
            // 根据比较类型执行不同的索引查询
            resultRIDs = new ArrayList<>();
            
            switch (comparisonType) {
                case EQUAL:
                    // 精确查找
                    List<RID> exactMatch = indexHandle.searchEqual(searchKey);
                    if (exactMatch != null) {
                        resultRIDs.addAll(exactMatch);
                    }
                    break;
                    
                case LESS_THAN:
                    // 范围查找：小于指定值
                    resultRIDs = indexHandle.searchLessThan(searchKey);
                    break;
                    
                case GREATER_THAN:
                    // 范围查找：大于指定值
                    resultRIDs = indexHandle.searchGreaterThan(searchKey);
                    break;
                    
                case LESS_EQUAL:
                    // 范围查找：小于等于指定值
                    resultRIDs = indexHandle.searchLessEqual(searchKey);
                    break;
                    
                case GREATER_EQUAL:
                    // 范围查找：大于等于指定值
                    resultRIDs = indexHandle.searchGreaterEqual(searchKey);
                    break;
            }
            
            currentIndex = 0;
            isOpen = true;
            
        } catch (DBException e) {
            e.printStackTrace();
            isOpen = false;
            throw e;
        }
    }

    @Override
    public boolean hasNext() throws DBException {
        if (!isOpen) {
            return false;
        }
        return currentIndex < resultRIDs.size();
    }

    @Override
    public void Next() throws DBException {
        if (!isOpen || !hasNext()) {
            return;
        }
        currentIndex++;
    }

    @Override
    public Tuple Current() {
        if (!isOpen || currentIndex >= resultRIDs.size()) {
            return null;
        }
        
        try {
            RID currentRID = resultRIDs.get(currentIndex);
            Record record = fileHandle.GetRecord(currentRID);
            return new TableTuple(tableName, tableMeta, record, currentRID);
        } catch (DBException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void Close() {
        if (!isOpen) {
            return;
        }
        
        try {
            if (fileHandle != null) {
                dbManager.getRecordManager().CloseFile(fileHandle);
            }
            if (indexHandle != null) {
                dbManager.getIndexManager().CloseIndex(indexHandle);
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
        
        fileHandle = null;
        indexHandle = null;
        resultRIDs = null;
        isOpen = false;
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        return tableMeta.columns_list;
    }

    public RID getCurrentRID() {
        if (!isOpen || currentIndex >= resultRIDs.size()) {
            return null;
        }
        return resultRIDs.get(currentIndex);
    }
}