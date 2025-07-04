package edu.sustech.cs307.value;

import java.nio.ByteBuffer;

public class Value implements Comparable<Value> {
    public Object value;
    public ValueType type;
    public static final int INT_SIZE = 8;
    public static final int FLOAT_SIZE = 8;
    public static final int CHAR_SIZE = 64;

    public Value(Object value, ValueType type) {
        this.value = value;
        this.type = type;
    }

    public Value(Long value) {
        this.value = value;
        type = ValueType.INTEGER;
    }

    public Value(Double value) {
        this.value = value;
        type = ValueType.FLOAT;
    }

    public Value(String value) {
        this.value = value;
        type = ValueType.CHAR;
    }

    /**
     * 将当前值转换为字节数组。
     * 
     * @return 字节数组表示的值，根据值的类型（INTEGER、FLOAT、CHAR）进行转换。
     * @throws RuntimeException 如果值的类型不受支持。
     */
    public byte[] ToByte() {
        return switch (type) {
            case INTEGER -> {
                ByteBuffer buffer1 = ByteBuffer.allocate(8);
                buffer1.putLong((long) value);
                yield buffer1.array();
            }
            case FLOAT -> {
                ByteBuffer buffer2 = ByteBuffer.allocate(8);
                buffer2.putDouble((double) value);
                yield buffer2.array();
            }
            case CHAR -> {
                String str = (String) value;
                ByteBuffer buffer3 = ByteBuffer.allocate(64);
                buffer3.putInt(str.length());
                buffer3.put(str.getBytes());
                yield buffer3.array();
            }
            default -> throw new RuntimeException("Unsupported value type: " + type);
        };
    }

    /**
     * 根据给定的字节数组和值类型创建一个 Value 对象。
     *
     * @param bytes 字节数组，表示要转换的值。
     * @param type  值的类型，支持 INTEGER、FLOAT 和 CHAR。
     * @return 转换后的 Value 对象。
     * @throws RuntimeException 如果提供的值类型不受支持。
     */
    public static Value FromByte(byte[] bytes, ValueType type) {
        return switch (type) {
            case INTEGER -> {
                ByteBuffer buffer1 = ByteBuffer.wrap(bytes);
                yield new Value(buffer1.getLong());
            }
            case FLOAT -> {
                ByteBuffer buffer2 = ByteBuffer.wrap(bytes);
                yield new Value(buffer2.getDouble());
            }
            case CHAR -> {
                ByteBuffer buffer3 = ByteBuffer.wrap(bytes);
                var length = buffer3.getInt();
                // int is 4 byte
                String s = new String(bytes, 4, length);
                yield new Value(s);
            }
            default -> throw new RuntimeException("Unsupported value type: " + type);
        };

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Value)) return false;
        Value other = (Value) o;
        return java.util.Objects.equals(type, other.type) &&
               java.util.Objects.equals(value, other.value);
    }

    @Override
    public int compareTo(Value other) {
        if (this.type != other.type) {
            throw new IllegalArgumentException("Cannot compare values of different types");
        }
        return switch (this.type) {
            case INTEGER -> {
                long thisInt = (Long) this.value;
                long otherInt = (Long) other.value;
                yield Long.compare(thisInt, otherInt); // 添加 yield 返回比较结果
            }
            case FLOAT -> {
                double thisFloat = (Double) this.value;
                double otherFloat = (Double) other.value;
                yield Double.compare(thisFloat, otherFloat);
            }
            case CHAR -> {
                String thisStr = (String) this.value;
                String otherStr = (String) other.value;
                yield thisStr.compareTo(otherStr);
            }
            default -> throw new IllegalArgumentException("Unsupported value type: " + this.type);
        };
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, value);
    }
    public ValueType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public boolean isValid() {
        return value != null && type != null && 
               (type == ValueType.INTEGER && value instanceof Long ||
                type == ValueType.FLOAT && value instanceof Double ||
                type == ValueType.CHAR && value instanceof String);
    }

    @Override
    public String toString() {
        switch (type) {
            case INTEGER, FLOAT ->{
                return this.value.toString();
            }
            case CHAR -> {
                byte[] bytes = ((String) this.value).getBytes();
                ByteBuffer buffer3 = ByteBuffer.wrap(bytes);
                var length = buffer3.getInt();
                // int is 4 byte
                return new String(bytes, 4, length);
            }
            default -> throw new RuntimeException("Unsupported value type: " + type);
        }
    }
}
