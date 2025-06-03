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
        return keys.size() >= degree - 1;
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
        return keys.isEmpty() ? null : keys.get(0);
    }
    
    public List<RID> search(Value key) {
        List<RID> result = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(key)) {
                result.add(values.get(i));
            }
        }
        return result;
    }
    
    public RID searchSingle(Value key) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(key)) {
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
    
    // 修复删除操作
    public boolean delete(Value key) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(key)) {
                keys.remove(i);
                values.remove(i);
                return true;
            }
        }
        return false;
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
    public BPlusTreeLeafNode getNext() { return next; }
    public BPlusTreeLeafNode getPrev() { return prev; }
    public void setNext(BPlusTreeLeafNode next) { this.next = next; }
    public void setPrev(BPlusTreeLeafNode prev) { this.prev = prev; }
}
