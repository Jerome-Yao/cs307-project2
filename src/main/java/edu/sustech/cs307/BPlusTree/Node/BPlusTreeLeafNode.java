package edu.sustech.cs307.BPlusTree.Node;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
import java.util.ArrayList;
import java.util.List;

public class BPlusTreeLeafNode extends BPlusTreeNode {
    private List<RID> values;
    private BPlusTreeLeafNode next;
    private BPlusTreeLeafNode prev;
    
    public BPlusTreeLeafNode(int degree) {
        super(degree, true);
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.next = null;
        this.prev = null;
    }
    
    @Override
    public boolean isFull() {
        return keys.size() >= degree;
    }

    @Override
    public boolean isNeedSplit() {
        return keys.size() > degree;
    }
    
    @Override
    public void insert(Value key, RID rid) {
        int pos = 0;
        // 找到正确的插入位置
        while (pos < keys.size() && keys.get(pos).compareTo(key) < 0) {
            pos++;
        }
        
        // 检查是否已存在该键
        if (pos < keys.size() && keys.get(pos).equals(key)) {
            values.set(pos, rid); // 更新现有值
            return;
        }
        
        // 插入新的键值对
        keys.add(pos, key);
        values.add(pos, rid);
    }
    
    @Override
    public BPlusTreeNode split() {
        int mid = (keys.size() + 1) / 2; // 分裂点计算
        BPlusTreeLeafNode newNode = new BPlusTreeLeafNode(degree);
        
        // Move half of the keys and values to new node
        newNode.keys.addAll(keys.subList(mid, keys.size()));
        newNode.values.addAll(values.subList(mid, values.size()));
        
        // Remove moved elements from current node
        keys.subList(mid, keys.size()).clear();
        values.subList(mid, values.size()).clear();
        
        // Update linked list pointers
        newNode.next = this.next;
        newNode.prev = this;
        if (this.next != null) {
            this.next.prev = newNode;
        }
        this.next = newNode;
        newNode.setParent(this.parent);
        
        return newNode;
    }
    
    @Override
    public Value getFirstKey() {
        // if (keys.isEmpty()) {
        //     System.out.println("=------------------------------Warning: Attempting to get first key from an empty leaf node.");
        // }
        return keys.isEmpty() ? null : keys.get(0);
    }

    @Override
    public Value getLastKey() {
        return keys.isEmpty()? null : keys.get(keys.size() - 1);
    }

    @Override
    public void removeLastKey() {
        if (!keys.isEmpty()) {
            keys.remove(keys.size() - 1);
        }
    }
    
    public List<RID> search(Value key) {
        List<RID> result = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            if (isValueEqual(keys.get(i), key)) {
                result.add(values.get(i));
            }
        }
        return result;
    }
    
    public RID searchSingle(Value key) {
        for (int i = 0; i < keys.size(); i++) {
            if (isValueEqual(keys.get(i), key)) {
                return values.get(i);
            }
        }
        return null;
    }
    
    public List<RID> rangeSearch(Value startKey, Value endKey) {
        List<RID> result = new ArrayList<>();
        BPlusTreeLeafNode current = this;
        
        while (current != null) {
            for (int i = 0; i < current.keys.size(); i++) {
                Value currentKey = current.keys.get(i);
                if (currentKey.compareTo(startKey) >= 0 && currentKey.compareTo(endKey) <= 0) {
                    result.add(current.values.get(i));
                } else if (currentKey.compareTo(endKey) > 0) {
                    return result;
                }
            }
            current = current.next;
        }
        return result;
    }
    
    // 修复删除操作 - 使用更安全的值比较方法
    public boolean delete(Value key) {
        // System.out.println("-------------------------------");
        // System.out.println("Deleting key: " + key + " (type: " + getValueType(key) + ")");
        // System.out.println("Current keys in leaf:");
        // for (int i = 0; i < keys.size(); i++) {
        //     System.out.println("  [" + i + "] " + keys.get(i) + " (type: " + getValueType(keys.get(i)) + 
        //                      ") equals check: " + isValueEqual(keys.get(i), key));
        // }
        // System.out.println("-------------------------------");
        // System.out.println("fffffffffffffffffffffffffffffff"+keys.size());
        for (int i = 0; i < keys.size(); i++) {
            // System.out.println("----------------------------"+keys.get(i).getValue()+"----------------------------");
            // System.out.println("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwkey.getValue() = " + key.getValue());
            if (isValueEqual(keys.get(i), key)) {
                // System.out.println("Found key at index " + i + ", removing...");
                keys.remove(i);
                values.remove(i);
                return true;
            }
        }
        System.out.println("Key not found for deletion");
        return false;
    }
    
    // 安全的值比较方法
    private boolean isValueEqual(Value v1, Value v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        
        try {
            // 首先尝试使用 Value 的 equals 方法
            if (v1.equals(v2)) return true;
            
            // 如果 equals 不可靠，尝试直接比较底层值
            Object val1 = v1.getValue();
            Object val2 = v2.getValue();
            
            if (val1 == null && val2 == null) return true;
            if (val1 == null || val2 == null) return false;
            
            // 对于字符串类型，进行特殊处理
            if (val1 instanceof String && val2 instanceof String) {
                return ((String) val1).equals((String) val2);
            }
            
            // 对于数值类型
            if (val1 instanceof Number && val2 instanceof Number) {
                return val1.equals(val2);
            }
            
            // 通用比较
            return val1.equals(val2);
            
        } catch (Exception e) {
            System.err.println("Error comparing values: " + e.getMessage());
            // 如果比较失败，尝试 compareTo 方法
            try {
                return v1.compareTo(v2) == 0;
            } catch (Exception e2) {
                System.err.println("Error using compareTo: " + e2.getMessage());
                return false;
            }
        }
    }

        /**
     * 优化版本：从特定叶子节点开始搜索大于指定键的记录
     * 这个方法假设调用者已经定位到了合适的起始叶子节点
     * @param key 比较的键值
     * @return 匹配的RID列表
     */
    public List<RID> searchGreaterThanFromNode(Value key) {
        List<RID> result = new ArrayList<>();
        BPlusTreeLeafNode current = this;
        
        while (current != null) {
            for (int i = 0; i < current.keys.size(); i++) {
                Value currentKey = current.keys.get(i);
                if (currentKey.compareTo(key) > 0) {
                    result.add(current.values.get(i));
                }
            }
            current = current.next;
        }
        return result;
    }

    /**
     * 优化版本：从特定叶子节点开始搜索大于等于指定键的记录
     * @param key 比较的键值
     * @return 匹配的RID列表
     */
    public List<RID> searchGreaterThanOrEqualFromNode(Value key) {
        List<RID> result = new ArrayList<>();
        BPlusTreeLeafNode current = this;
        
        while (current != null) {
            for (int i = 0; i < current.keys.size(); i++) {
                Value currentKey = current.keys.get(i);
                if (currentKey.compareTo(key) >= 0) {
                    result.add(current.values.get(i));
                }
            }
            current = current.next;
        }
        return result;
    }

    /**
     * 优化版本：搜索小于指定键的记录，到当前节点为止
     * @param key 比较的键值
     * @return 匹配的RID列表
     */
    public List<RID> searchLessThanToNode(Value key) {
        List<RID> result = new ArrayList<>();
        
        // 从最左侧的叶子节点开始
        BPlusTreeLeafNode leftmost = this;
        while (leftmost.prev != null) {
            leftmost = leftmost.prev;
        }
        
        BPlusTreeLeafNode current = leftmost;
        while (current != null && current != this.next) {
            for (int i = 0; i < current.keys.size(); i++) {
                Value currentKey = current.keys.get(i);
                if (currentKey.compareTo(key) < 0) {
                    result.add(current.values.get(i));
                } else {
                    return result;
                }
            }
            current = current.next;
        }
        return result;
    }

    /**
     * 优化版本：搜索小于指定键的记录，到当前节点为止
     * @param key 比较的键值
     * @return 匹配的RID列表
     */
    public List<RID> searchLessThanOrEqualToNode(Value key) {
        List<RID> result = new ArrayList<>();
        
        // 从最左侧的叶子节点开始
        BPlusTreeLeafNode leftmost = this;
        while (leftmost.prev != null) {
            leftmost = leftmost.prev;
        }
        
        BPlusTreeLeafNode current = leftmost;
        while (current != null && current != this.next) {
            for (int i = 0; i < current.keys.size(); i++) {
                Value currentKey = current.keys.get(i);
                if (currentKey.compareTo(key) <= 0) {
                    result.add(current.values.get(i));
                } else {
                    return result;
                }
            }
            current = current.next;
        }
        return result;
    }
    
    // 获取值的类型信息
    private String getValueType(Value value) {
        if (value == null) return "null";
        try {
            Object val = value.getValue();
            if (val == null) return "null";
            return val.getClass().getSimpleName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    // 检查节点是否需要重新平衡（键数量过少）
    public boolean needsRebalance() {
        int minKeys = (degree - 1) / 2;
        return keys.size() < minKeys;
    }
    
    // 从左兄弟借键
    public boolean borrowFromLeft(BPlusTreeLeafNode leftSibling, BPlusTreeInternalNode parent, int keyIndex) {
        int minKeys = (degree - 1) / 2;
        if (leftSibling.keys.size() <= minKeys) {
            return false;
        }
        
        // 移动左兄弟的最后一个键和值
        Value borrowedKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);
        RID borrowedValue = leftSibling.values.remove(leftSibling.values.size() - 1);
        
        keys.add(0, borrowedKey);
        values.add(0, borrowedValue);
        
        // 更新父节点的键
        if (keyIndex < parent.getKeys().size()) {
            parent.getKeys().set(keyIndex, this.keys.get(0));
        }
        return true;
    }
    
    // 从右兄弟借键
    public boolean borrowFromRight(BPlusTreeLeafNode rightSibling, BPlusTreeInternalNode parent, int keyIndex) {
        int minKeys = (degree - 1) / 2;
        if (rightSibling.keys.size() <= minKeys) {
            return false;
        }
        
        // 移动右兄弟的第一个键和值
        Value borrowedKey = rightSibling.keys.remove(0);
        RID borrowedValue = rightSibling.values.remove(0);
        
        keys.add(borrowedKey);
        values.add(borrowedValue);
        
        // 更新父节点的键
        if (keyIndex < parent.getKeys().size()) {
            parent.getKeys().set(keyIndex, rightSibling.keys.get(0));
        }
        return true;
    }
    
    // 与兄弟节点合并
    public void mergeWithRight(BPlusTreeLeafNode rightSibling) {
        keys.addAll(rightSibling.keys);
        values.addAll(rightSibling.values);
        
        // 更新链表指针
        this.next = rightSibling.next;
        if (rightSibling.next != null) {
            rightSibling.next.prev = this;
        }
    }
    
    // Getters and setters
    public List<RID> getValues() { return values; }
    // public List<
    public BPlusTreeLeafNode getNext() { return next; }
    public BPlusTreeLeafNode getPrev() { return prev; }
    public void setNext(BPlusTreeLeafNode next) { this.next = next; }
    public void setPrev(BPlusTreeLeafNode prev) { this.prev = prev; }
}