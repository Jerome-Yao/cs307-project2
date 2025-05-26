package edu.sustech.cs307.aggregate;

import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.value.ValueType;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class AggregateCalculator {
    
    public static Value calculate(AggregateFunction function, List<Value> values, boolean distinct) {
        if (values.isEmpty() && function != AggregateFunction.COUNT) {
            return new Value((String) null);
        }
        
        List<Value> processedValues = values;
        if (distinct) {
            Set<Object> uniqueValues = new HashSet<>();
            processedValues = values.stream()
                .filter(v -> v.value != null && uniqueValues.add(v.value))
                .toList();
        }
        
        switch (function) {
            case COUNT:
                return new Value((long) (distinct ? processedValues.size() : values.size()));
                
            case SUM:
                return calculateSum(processedValues);
                
            case AVG:
                return calculateAvg(processedValues);
                
            case MIN:
                return calculateMin(processedValues);
                
            case MAX:
                return calculateMax(processedValues);
                
            default:
                throw new IllegalArgumentException("Unsupported aggregate function: " + function);
        }
    }
    
    private static Value calculateSum(List<Value> values) {
        if (values.isEmpty()) return new Value((String) null);
        
        Value first = values.get(0);
        if (first.type == ValueType.INTEGER) {
            long sum = 0;
            for (Value v : values) {
                if (v.value != null) {
                    sum += (Long) v.value;
                }
            }
            return new Value(sum);
        } else if (first.type == ValueType.FLOAT) {
            double sum = 0.0;
            for (Value v : values) {
                if (v.value != null) {
                    sum += (Double) v.value;
                }
            }
            return new Value(sum);
        }
        throw new IllegalArgumentException("SUM not supported for type: " + first.type);
    }
    
    private static Value calculateAvg(List<Value> values) {
        if (values.isEmpty()) return new Value((String) null);
        
        Value sumValue = calculateSum(values);
        if (sumValue.value == null) return new Value((String) null);
        
        long count = values.stream().mapToLong(v -> v.value != null ? 1 : 0).sum();
        if (count == 0) return new Value((String) null);
        
        if (sumValue.type == ValueType.INTEGER) {
            return new Value((double) (Long) sumValue.value / count);
        } else {
            return new Value((Double) sumValue.value / count);
        }
    }
    
    private static Value calculateMin(List<Value> values) {
        if (values.isEmpty()) return new Value((String) null);
        
        Value min = null;
        for (Value v : values) {
            if (v.value != null) {
                if (min == null || compareValues(v, min) < 0) {
                    min = v;
                }
            }
        }
        return min != null ? min : new Value((String) null);
    }
    
    private static Value calculateMax(List<Value> values) {
        if (values.isEmpty()) return new Value((String) null);
        
        Value max = null;
        for (Value v : values) {
            if (v.value != null) {
                if (max == null || compareValues(v, max) > 0) {
                    max = v;
                }
            }
        }
        return max != null ? max : new Value((String) null);
    }
    
    @SuppressWarnings("unchecked")
    private static int compareValues(Value v1, Value v2) {
        if (v1.type != v2.type) {
            throw new IllegalArgumentException("Cannot compare values of different types");
        }
        
        Object val1 = v1.value;
        Object val2 = v2.value;
        
        if (val1 instanceof Comparable && val2 instanceof Comparable) {
            return ((Comparable<Object>) val1).compareTo(val2);
        }
        
        throw new IllegalArgumentException("Values are not comparable");
    }
}