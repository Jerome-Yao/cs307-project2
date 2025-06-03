package edu.sustech.cs307.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.exception.ExceptionTypes;
import edu.sustech.cs307.BPlusTree.*;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class TableMeta {
    public String tableName;
    public ArrayList<ColumnMeta> columns_list;

    @JsonIgnore
    public Map<String, ColumnMeta> columns; // 列名 -> 列的元数据

    private Map<String, IndexType> indexes; // 索引信息
    
    @JsonIgnore
    private Map<String, BPlusTree> indexTrees; // 列名 -> B+树索引
    
    private static final int DEFAULT_BTREE_DEGREE = 4; // 默认B+树度数

    private Map<String, Integer> column_rank;

    public enum IndexType {
        BTREE
    }

    public TableMeta(String tableName) {
        this.tableName = tableName;
        this.columns = new HashMap<>();
        this.indexes = new HashMap<>();
        this.indexTrees = new HashMap<>();
        // System.out.println("1111111111111111111111111111111111111111" );
    }

    public TableMeta(String tableName, ArrayList<ColumnMeta> columns) {
        this.tableName = tableName;
        this.columns_list = columns;
        this.columns = new HashMap<>();
        this.indexes = new HashMap<>();
        this.indexTrees = new HashMap<>();
        for (ColumnMeta column : columns) {
            this.columns.put(column.name, column);
            createBTreeIndex(column.getName());
            // System.out.println("Column added: " + column.name);
        }
        // System.out.println("2222222222222222222222222222222222222222222" );
    }

    @JsonCreator
    public TableMeta(@JsonProperty("tableName") String tableName, 
                     @JsonProperty("columns_list") ArrayList<ColumnMeta> columns_list, 
                     @JsonProperty("indexes") Map<String, IndexType> indexes) {
        this.tableName = tableName;
        this.columns_list = columns_list;
        this.columns = new HashMap<>();
        this.indexes = indexes != null ? indexes : new HashMap<>();
        this.indexTrees = new HashMap<>();
        
        for (var column : columns_list) {
            this.columns.put(column.name, column);
        }
        // System.out.println("333333333333333333333333333333333333333333" );
        // 为已存在的索引创建B+树
        initializeIndexTrees();
    }

    /**
     * 初始化索引树
     */
    private void initializeIndexTrees() {
        for (String columnName : indexes.keySet()) {
            createBTreeIndex(columnName);
        }
    }

    /**
     * 为指定列创建B+树索引
     * @param columnName 列名
     * @throws DBException 如果列不存在或索引已存在
     */
    public void createIndex(String columnName) throws DBException {
        createIndex(columnName, IndexType.BTREE);
    }

    /**
     * 为指定列创建指定类型的索引
     * @param columnName 列名
     * @param indexType 索引类型
     * @throws DBException 如果列不存在或索引已存在
     */
    public void createIndex(String columnName, IndexType indexType) throws DBException {
        if (!this.columns.containsKey(columnName)) {
            throw new DBException(ExceptionTypes.ColumnDoseNotExist(columnName));
        }
        
        if (this.indexes.containsKey(columnName)) {
            throw new DBException("Index already exists for column: " + columnName);
        }
        
        this.indexes.put(columnName, indexType);
        
        if (indexType == IndexType.BTREE) {
            createBTreeIndex(columnName);
        }
    }

    /**
     * 创建B+树索引
     * @param columnName 列名
     */
    private void createBTreeIndex(String columnName) {
        BPlusTree tree = new BPlusTree(DEFAULT_BTREE_DEGREE);
        this.indexTrees.put(columnName, tree);
        tree.printTree();
    }

    /**
     * 删除索引
     * @param columnName 列名
     * @throws DBException 如果索引不存在
     */
    public void dropIndex(String columnName) throws DBException {
        if (!this.indexes.containsKey(columnName)) {
            throw new DBException("Index does not exist for column: " + columnName);
        }
        
        this.indexes.remove(columnName);
        this.indexTrees.remove(columnName);
    }

    /**
     * 向索引中插入键值对
     * @param columnName 列名
     * @param key 键值
     * @param rid 记录ID
     * @throws DBException 如果索引不存在
     */
    public void insertIntoIndex(String columnName, Value key, RID rid) throws DBException {
        if (!this.indexes.containsKey(columnName)) {
            throw new DBException("Index does not exist for column: " + columnName);
        }
        
        BPlusTree tree = this.indexTrees.get(columnName);
        if (tree != null) {
            tree.insert(key, rid);
        }
    }

    /**
     * 从索引中删除键值
     * @param columnName 列名
     * @param key 键值
     * @return 是否删除成功
     * @throws DBException 如果索引不存在
     */
    public boolean deleteFromIndex(String columnName, Value key) throws DBException {
        if (!this.indexes.containsKey(columnName)) {
            throw new DBException("Index does not exist for column: " + columnName);
        }
        
        BPlusTree tree = this.indexTrees.get(columnName);
        if (tree != null) {
            return tree.delete(key);
        }
        return false;
    }

    /**
     * 在索引中搜索单个值
     * @param columnName 列名
     * @param key 搜索键
     * @return 记录ID，如果未找到返回null
     * @throws DBException 如果索引不存在
     */
    public RID searchIndex(String columnName, Value key) throws DBException {
        if (!this.indexes.containsKey(columnName)) {
            throw new DBException("Index does not exist for column: " + columnName);
        }
        
        BPlusTree tree = this.indexTrees.get(columnName);
        if (tree != null) {
            return tree.searchSingle(key);
        }
        return null;
    }

    /**
     * 在索引中搜索所有匹配的值
     * @param columnName 列名
     * @param key 搜索键
     * @return 记录ID列表
     * @throws DBException 如果索引不存在
     */
    public List<RID> searchIndexAll(String columnName, Value key) throws DBException {
        if (!this.indexes.containsKey(columnName)) {
            throw new DBException("Index does not exist for column: " + columnName);
        }
        
        BPlusTree tree = this.indexTrees.get(columnName);
        if (tree != null) {
            return tree.search(key);
        }
        return new ArrayList<>();
    }

    /**
     * 范围搜索
     * @param columnName 列名
     * @param startKey 起始键
     * @param endKey 结束键
     * @return 记录ID列表
     * @throws DBException 如果索引不存在
     */
    public List<RID> rangeSearchIndex(String columnName, Value startKey, Value endKey) throws DBException {
        if (!this.indexes.containsKey(columnName)) {
            throw new DBException("Index does not exist for column: " + columnName);
        }
        
        BPlusTree tree = this.indexTrees.get(columnName);
        if (tree != null) {
            return tree.rangeSearch(startKey, endKey);
        }
        return new ArrayList<>();
    }

    /**
     * 获取指定列的B+树索引
     * @param columnName 列名
     * @return B+树，如果不存在返回null
     */
    public BPlusTree getBTreeIndex(String columnName) {
        return this.indexTrees.get(columnName);
    }

    /**
     * 检查列是否有索引
     * @param columnName 列名
     * @return 是否有索引
     */
    public boolean hasIndex(String columnName) {
        return this.indexes.containsKey(columnName);
    }

    /**
     * 获取列的索引类型
     * @param columnName 列名
     * @return 索引类型，如果不存在返回null
     */
    public IndexType getIndexType(String columnName) {
        return this.indexes.get(columnName);
    }

    /**
     * 获取所有有索引的列名
     * @return 列名集合
     */
    public java.util.Set<String> getIndexedColumns() {
        return this.indexes.keySet();
    }

    /**
     * 打印指定列的索引树结构（用于调试）
     * @param columnName 列名
     */
    public void printIndexTree(String columnName) {
        BPlusTree tree = this.indexTrees.get(columnName);
        if (tree != null) {
            System.out.println("Index tree for column: " + columnName);
            tree.printTree();
            tree.printLeafChain();
        } else {
            System.out.println("No index tree found for column: " + columnName);
        }
    }

    /**
     * 验证指定列的索引树结构（用于调试）
     * @param columnName 列名
     * @return 验证结果
     */
    public boolean validateIndexTree(String columnName) {
        BPlusTree tree = this.indexTrees.get(columnName);
        if (tree != null) {
            return tree.validate();
        }
        return false;
    }

    // 原有方法保持不变
    public void addColumn(ColumnMeta column) throws DBException {
        String columnName = column.name;
        if (this.columns.containsKey(columnName)) {
            throw new DBException(ExceptionTypes.ColumnAlreadyExist(columnName));
        }
        this.columns.put(columnName, column);
    }

    public void dropColumn(String columnName) throws DBException {
        if (!this.columns.containsKey(columnName)) {
            throw new DBException(ExceptionTypes.ColumnDoseNotExist(columnName));
        }
        
        // 如果该列有索引，先删除索引
        if (this.indexes.containsKey(columnName)) {
            this.indexes.remove(columnName);
            this.indexTrees.remove(columnName);
        }
        
        this.columns.remove(columnName);
    }

    public ColumnMeta getColumnMeta(String columnName) {
        if (this.columns.containsKey(columnName)) {
            return this.columns.get(columnName);
        }
        return null;
    }

    public Map<String, ColumnMeta> getColumns() {
        return this.columns;
    }

    public void setColumns(Map<String, ColumnMeta> columns) {
        this.columns = columns;
    }

    public int columnCount() {
        return this.columns.size();
    }

    public boolean hasColumn(String columnName) {
        return this.columns.containsKey(columnName);
    }

    public Map<String, IndexType> getIndexes() {
        return indexes;
    }

    public void setIndexes(Map<String, IndexType> indexes) {
        this.indexes = indexes;
        // 重新初始化索引树
        this.indexTrees.clear();
        initializeIndexTrees();
    }

    /**
     * 获取所有索引树（主要用于内部操作）
     * @return 索引树映射
     */
    @JsonIgnore
    public Map<String, BPlusTree> getIndexTrees() {
        return indexTrees;
    }
}