package edu.sustech.cs307.physicalOperator;

import edu.sustech.cs307.BPlusTree.BPlusTree;
// import edu.sustech.cs307.BPlusTree.bplustree;
import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.TempTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import edu.sustech.cs307.record.RID;
import java.util.List;
import java.util.ArrayList;

public class InsertOperator implements PhysicalOperator {
    private final String data_file;
    private final List<Value> values;
    private final DBManager dbManager;
    private final int columnSize;
    private int rowCount;
    private boolean outputed;

    public InsertOperator(String data_file, List<String> columnNames, List<Value> values, DBManager dbManager) {
        this.data_file = data_file;
        this.values = values;
        this.dbManager = dbManager;
        this.columnSize = columnNames.size();
        this.rowCount = 0;
        this.outputed = false;
    }

    @Override
    public boolean hasNext() {
        return !this.outputed;
    }

        @Override
    public void Begin() throws DBException {
        try {
            var fileHandle = dbManager.getRecordManager().OpenFile(data_file);
            // Serialize values to ByteBuf
            ByteBuf buffer = Unpooled.buffer();
            for (int i = 0; i < values.size(); i++) {
                buffer.writeBytes(values.get(i).ToByte());
                if (i != 0 && (i + 1) % columnSize == 0) {
                    RID rid = fileHandle.InsertRecord(buffer);
                    buffer.clear();

                    // 获取表元数据
                    TableMeta tableMeta = dbManager.getMetaManager().getTable(data_file);
                    // System.out.println(tableMeta);
//                     System.out.println("0000000000000000000000000000000000");
                    if (tableMeta != null) {
                        // 遍历所有索引
                        // System.out.println("------------"+tableMeta.getIndexTrees().keySet()+"-----------------");
//                         System.out.println("111111111111111111111111111111");
                        tableMeta.printColumns();
                        int tmp = 0;
//                         System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        // System.out.println(values.size());
                        for (String indexName : tableMeta.getColumns().keySet()) {
                            if (tmp >= values.size()) {
                                break;
                            }
//                            System.out.println("2222222222222222222222222222222");
                            System.out.println("indexName: "+indexName);
                            // 假设索引名对应列名，找到对应列的值
                            // int columnIndex = getColumnIndex(indexName);
                            // if (columnIndex != -1) {
                            //     Value value = values.get(columnIndex);
                            //     if (value.getValue() instanceof Comparable) {
                            //         BPlusTree<?> bPlusTree = tableMeta.getIndexBPlusTree(indexName);
                            //         if (bPlusTree != null) {
                            //             // 类型转换并插入索引
                            //             @SuppressWarnings("unchecked")
                            //             BPlusTree<Comparable<?>> tree = (BPlusTree<Comparable<?>>) bPlusTree;
                            //             tree.insert((Comparable<?>) value.getValue());
                            //         }
                            //     }
                            // }
                            // System.out.println(rid);
                            BPlusTree tree = tableMeta.getBTreeIndex(indexName);
                            // System.out.println(rid);
                            // if (tree == null) {
                            //     System.out.println("Index " + indexName + " not found.");
                            //     continue;
                            // }
                            // System.out.println(rid);
                            // System.out.println(values.get(tmp));
                            tree.insert(values.get(tmp), rid);
                            System.out.println(tree);
                            // System.out.println(values);
                            // System.out.println("344444444444444444444444444444444443333");
                            tree.printTree();
                            // System.out.println(rid);
                            tmp++;
                            // System.out.println(rid);
                        }
                    }
                }
            }
            this.rowCount = values.size() / columnSize;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to insert record: " + e.getMessage() + "\n");
        }
    }

    @Override
    public void Next() {
    }

    @Override
    public Tuple Current() {
        ArrayList<Value> values = new ArrayList<>();
        values.add(new Value(rowCount, ValueType.INTEGER));
        this.outputed = true;
        return new TempTuple(values);
    }

    @Override
    public void Close() {
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        ArrayList<ColumnMeta> outputSchema = new ArrayList<>();
        outputSchema.add(new ColumnMeta("insert", "numberOfInsertRows", ValueType.INTEGER, 0, 0));
        return outputSchema;
    }

    public void reset() {
        // nothing to do
    }

    public Tuple getNextTuple() {
        return null;
    }
}
