package edu.sustech.cs307.physicalOperator;

// import static org.mockito.Answers.values;
import edu.sustech.cs307.record.Record;
import java.beans.Expression;
//import java.lang.foreign.ValueLayout;
import java.util.ArrayList;

import edu.sustech.cs307.BPlusTree.BPlusTree;
import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.meta.TableMeta;
import edu.sustech.cs307.record.RecordFileHandle;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.TempTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import edu.sustech.cs307.record.RID;

public class DeleteOperator implements PhysicalOperator {

    private final String data_file;
    private final DBManager dbManager;
    private final PhysicalOperator child;
    private int rowCount = 0;
    private RecordFileHandle fileHandle;

    public DeleteOperator(PhysicalOperator child, String tableName, DBManager dbManager) {
        this.data_file = tableName;
        this.dbManager = dbManager;
        this.child = child;
        try {
            this.fileHandle = dbManager.getRecordManager().OpenFile(data_file);
        } catch (DBException e) {
            throw new RuntimeException("Failed to open file: " + e.getMessage() + "\n");
        }

    }

    @Override
    public void Begin() throws DBException {
        child.Begin();
    }

    @Override
    public boolean hasNext() throws DBException {
        return child.hasNext();
    }

    @Override
    public void Next() throws DBException {
        child.Next();
        try {
            Tuple tuple = child.Current();
            if (tuple != null) {
                RID rid = null;
                if (child instanceof FilterOperator) {
                    rid = ((FilterOperator) child).getCurrentRID();
                } else if (child instanceof SeqScanOperator) {
                    rid = ((TableTuple) ((SeqScanOperator) child).Current()).getRID();
                } else {
                    throw new RuntimeException("unsupported child operator in delete operator");
                }

                TableMeta tableMeta = dbManager.getMetaManager().getTable(data_file);
                if (tableMeta != null) {
                    Record record = fileHandle.GetRecord(rid);
                    // 创建 TableTuple 实例（复用其反序列化逻辑）
                    TableTuple tableTuple = new TableTuple(data_file, tableMeta, record, rid);
                    // 调用 getValues() 获取所有列的 Value 列表
                    Value[] valuesArray = tableTuple.getValues();
                    List<Value> values = new ArrayList<>(List.of(valuesArray)); // 转换为 List<Value>
                    // System.out.println(values);
                    // 现在 values 已存储该记录的所有列值（用于索引删除）
                    int tmp = values.size() - 1; // 从最后一个开始删除
                    for (String indexName : tableMeta.getColumns().keySet()) {
                        if (tmp < 0) {
                            break;
                        }
                        // System.out.println("indexName: " + indexName);
                        BPlusTree tree = tableMeta.getBTreeIndex(indexName);
                        // 注意：删除操作应调用 tree.delete 而非 insert（根据实际需求调整）
                        if(values.get(tmp).getType() == ValueType.CHAR) {
                            // 对于 CHAR 类型的值，可能需要转换为合适的格式
                            String valueStr = values.get(tmp).toString();
                            System.out.println("the result of delete: " + tree.delete(new Value(valueStr, ValueType.CHAR)));
                            // System.out.println("Deleting from index " + indexName + " with value: " + valueStr);
                        } else {
                            // System.out.println("Deleting from index " + indexName + " with value: " + values.get(tmp));
                            System.out.println("the result of delete: " + tree.delete(values.get(tmp)));
                        }
                        // System.out.println("fsfsfsssssssssssssssssssssssssssssssssssssss " + values.get(tmp));
                        System.out.println(tree);
                        tree.printTree();
                        tmp--;
                    }
                }
                fileHandle.DeleteRecord(rid); // 使用获取的 rid 删除记录
                rowCount++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete records: " + e.getMessage() + "\n");
        }
    }

    @Override
    public void Close() {
        child.Close();
    }

    @Override
    public Tuple Current() {
        ArrayList<Value> values = new ArrayList<>();
        values.add(new Value(rowCount, ValueType.INTEGER));
        return new TempTuple(values);
    }

    @Override
    public ArrayList<ColumnMeta> outputSchema() {
        ArrayList<ColumnMeta> outputSchema = new ArrayList<>();
        outputSchema.add(new ColumnMeta("delete", "Count", ValueType.INTEGER, 0, 0));
        return outputSchema;
    }

}
