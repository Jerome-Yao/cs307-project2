package edu.sustech.cs307.physicalOperator;

import java.util.ArrayList;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.meta.ColumnMeta;
import edu.sustech.cs307.record.RecordFileHandle;
import edu.sustech.cs307.system.DBManager;
import edu.sustech.cs307.tuple.TableTuple;
import edu.sustech.cs307.tuple.TempTuple;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;

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
                if (child instanceof FilterOperator) {
                    fileHandle.DeleteRecord(((FilterOperator) child).getCurrentRID());
                    rowCount++;
                } else if (child instanceof SeqScanOperator) {
                    fileHandle.DeleteRecord(((TableTuple) ((SeqScanOperator) child).Current()).getRID());
                    rowCount++;
                } else {
                    throw new RuntimeException("unsupported child operator in delete operator");
                }
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
