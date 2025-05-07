package edu.sustech.cs307.logicalOperator;

import java.util.Collections;

public class LogicalDropOperator extends LogicalOperator {
    private final String tableName;

    public LogicalDropOperator(String tableName) {
        super(Collections.emptyList());
        this.tableName = tableName;
    }

    public String toString() {
        return "DropOperator(table=" + tableName
                + ")\n ├── " + childern.get(0);
    }
}
