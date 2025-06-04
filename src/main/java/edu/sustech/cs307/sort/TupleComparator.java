package edu.sustech.cs307.sort;

import edu.sustech.cs307.exception.DBException;
import edu.sustech.cs307.tuple.Tuple;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import edu.sustech.cs307.meta.TabCol;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.schema.Column;

import java.util.Comparator;
import java.util.List;

/**
 * Comparator for sorting tuples based on ORDER BY criteria.
 * Only supports int and double data types.
 */
public class TupleComparator implements Comparator<Tuple> {
    private final List<OrderByElement> orderByElements;
    private final List<TabCol> orderByColumns;

    public TupleComparator(List<OrderByElement> orderByElements, List<TabCol> orderByColumns) {
        this.orderByElements = orderByElements;
        this.orderByColumns = orderByColumns;
    }

    @Override
    public int compare(Tuple t1, Tuple t2) {
        for (int i = 0; i < orderByElements.size(); i++) {
            OrderByElement element = orderByElements.get(i);
            TabCol column = orderByColumns.get(i);
            
            Value v1, v2;
            try {
                v1 = t1.getValue(column);
                v2 = t2.getValue(column);
            } catch (DBException e) {
                throw new RuntimeException("Error getting value from tuple", e);
            }
            
            int comparison = compareValues(v1, v2);
            
            // If values are not equal, apply sort order
            if (comparison != 0) {
                // Check if descending order (ASC is default)
                if (element.isAsc() == false) {
                    comparison = -comparison;
                }
                return comparison;
            }
            // If values are equal, continue to next column
        }
        return 0; // All columns are equal
    }

    private int compareValues(Value v1, Value v2) {
        // Handle null values
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        if (v1.value == null && v2.value == null) return 0;
        if (v1.value == null) return -1;
        if (v2.value == null) return 1;

        ValueType type1 = v1.type;
        ValueType type2 = v2.type;

        // Only support INTEGER and FLOAT types
        if (type1 == ValueType.INTEGER && type2 == ValueType.INTEGER) {
            Long int1 = (Long) v1.value;
            Long int2 = (Long) v2.value;
            return Long.compare(int1, int2);
        } else if (type1 == ValueType.FLOAT && type2 == ValueType.FLOAT) {
            Double double1 = (Double) v1.value;
            Double double2 = (Double) v2.value;
            return Double.compare(double1, double2);
        } else if (type1 == ValueType.INTEGER && type2 == ValueType.FLOAT) {
            // Convert int to double for comparison
            Double double1 = ((Long) v1.value).doubleValue();
            Double double2 = (Double) v2.value;
            return Double.compare(double1, double2);
        } else if (type1 == ValueType.FLOAT && type2 == ValueType.INTEGER) {
            // Convert int to double for comparison
            Double double1 = (Double) v1.value;
            Double double2 = ((Long) v2.value).doubleValue();
            return Double.compare(double1, double2);
        } else {
            // Unsupported types for ORDER BY
            throw new UnsupportedOperationException("ORDER BY only supports int and double data types");
        }
    }
}
