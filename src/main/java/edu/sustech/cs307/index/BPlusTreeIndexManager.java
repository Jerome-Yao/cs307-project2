package edu.sustech.cs307.index;

import edu.sustech.cs307.BPlusTree.BPlusTree;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.meta.ColumnMeta;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class BPlusTreeIndexManager {
    private Map<String, BPlusTree> indexes;
    private static final int DEFAULT_DEGREE = 4;
    
    public BPlusTreeIndexManager() {
        this.indexes = new HashMap<>();
    }
    
    public void createIndex(String tableName, String columnName, int degree) {
        String indexKey = getIndexKey(tableName, columnName);
        BPlusTree index = new BPlusTree(degree > 0 ? degree : DEFAULT_DEGREE);
        indexes.put(indexKey, index);
    }
    
    public void createIndex(String tableName, String columnName) {
        createIndex(tableName, columnName, DEFAULT_DEGREE);
    }
    
    public void insertIntoIndex(String tableName, String columnName, Value key, RID rid) {
        String indexKey = getIndexKey(tableName, columnName);
        BPlusTree index = indexes.get(indexKey);
        if (index != null) {
            index.insert(key, rid);
        }
    }
    
    // public List<RID> searchIndex(String tableName, String columnName, Value key) {
    //     String indexKey = getIndexKey(tableName, columnName);
    //     BPlusTree index = indexes.get(indexKey);
    //     if (index != null) {
    //         return index.search(key);
    //     }
    //     return new ArrayList<>();
    // }
    
    // public List<RID> rangeSearchIndex(String tableName, String columnName, 
    //                                  Value startKey, Value endKey) {
    //     String indexKey = getIndexKey(tableName, columnName);
    //     BPlusTree index = indexes.get(indexKey);
    //     if (index != null) {
    //         return index.rangeSearch(startKey, endKey);
    //     }
    //     return new ArrayList<>();
    // }
    
    public boolean hasIndex(String tableName, String columnName) {
        String indexKey = getIndexKey(tableName, columnName);
        return indexes.containsKey(indexKey);
    }
    
    public void dropIndex(String tableName, String columnName) {
        String indexKey = getIndexKey(tableName, columnName);
        indexes.remove(indexKey);
    }
    
    private String getIndexKey(String tableName, String columnName) {
        return tableName + "." + columnName;
    }
    
    public BPlusTree getIndex(String tableName, String columnName) {
        String indexKey = getIndexKey(tableName, columnName);
        return indexes.get(indexKey);
    }
}