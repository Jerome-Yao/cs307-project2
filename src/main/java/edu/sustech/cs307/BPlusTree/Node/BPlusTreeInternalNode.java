package edu.sustech.cs307.BPlusTree.Node;

import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
import java.util.ArrayList;
import java.util.List;

public class BPlusTreeInternalNode extends BPlusTreeNode {
    private List<BPlusTreeNode> children;
    
    public BPlusTreeInternalNode(int degree) {
        super(degree, false);
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
    }
    
    @Override
    public boolean isFull() {
        return keys.size() >= degree - 1;
    }
    
    @Override
    public void insert(Value key, RID rid) {
        // Internal nodes don't directly store RID values
        throw new UnsupportedOperationException("Internal nodes don't store RID values directly");
    }
    
    public void insertChild(Value key, BPlusTreeNode child) {
        int pos = 0;
        while (pos < keys.size() && keys.get(pos).compareTo(key) < 0) {
            pos++;
        }
        keys.add(pos, key);
        children.add(pos + 1, child);
        child.setParent(this);
    }
    
    @Override
    public BPlusTreeNode split() {
        int mid = keys.size() / 2;
        BPlusTreeInternalNode newNode = new BPlusTreeInternalNode(degree);
        
        // Move keys after middle to new node (excluding middle key)
        if (mid + 1 < keys.size()) {
            newNode.keys.addAll(keys.subList(mid + 1, keys.size()));
        }
        
        // Move children after middle to new node
        newNode.children.addAll(children.subList(mid + 1, children.size()));
        
        // Update parent pointers for moved children
        for (BPlusTreeNode child : newNode.children) {
            child.setParent(newNode);
        }
        
        // Remove moved elements from current node
        keys.subList(mid, keys.size()).clear();
        children.subList(mid + 1, children.size()).clear();
        
        return newNode;
    }
    
    @Override
    public Value getFirstKey() {
        return keys.isEmpty() ? null : keys.get(0);
    }
    
    // 修复查找子节点的逻辑
    public BPlusTreeNode findChild(Value key) {
        int pos = 0;
        while (pos < keys.size() && key.compareTo(keys.get(pos)) >= 0) {
            pos++;
        }
        return children.get(pos);
    }
    
    
    // 检查节点是否需要重新平衡
    public boolean needsRebalance() {
        int minKeys = (degree - 1) / 2;
        return keys.size() < minKeys;
    }
    
    // 获取分裂时提升的键
    public Value getPromotedKey() {
        int mid = keys.size() / 2;
        return keys.get(mid);
    }
    
    // 从左兄弟借键
    public boolean borrowFromLeftSibling(BPlusTreeInternalNode leftSibling, 
                                       BPlusTreeInternalNode parent, int keyIndex) {
        if (leftSibling.keys.size() <= (degree - 1) / 2) {
            return false; // 左兄弟也是最小状态，无法借出
        }
        
        // 从父节点下拉一个键
        Value parentKey = parent.keys.get(keyIndex);
        keys.add(0, parentKey);
        
        // 从左兄弟移动最大的子节点到当前节点
        BPlusTreeNode borrowedChild = leftSibling.children.remove(leftSibling.children.size() - 1);
        children.add(0, borrowedChild);
        borrowedChild.setParent(this);
        
        // 左兄弟的最大键上移到父节点
        Value borrowedKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);
        parent.keys.set(keyIndex, borrowedKey);
        
        return true;
    }
    
    // 从右兄弟借键
    public boolean borrowFromRightSibling(BPlusTreeInternalNode rightSibling, 
                                        BPlusTreeInternalNode parent, int keyIndex) {
        if (rightSibling.keys.size() <= (degree - 1) / 2) {
            return false; // 右兄弟也是最小状态，无法借出
        }
        
        // 从父节点下拉一个键
        Value parentKey = parent.keys.get(keyIndex);
        keys.add(parentKey);
        
        // 从右兄弟移动最小的子节点到当前节点
        BPlusTreeNode borrowedChild = rightSibling.children.remove(0);
        children.add(borrowedChild);
        borrowedChild.setParent(this);
        
        // 右兄弟的最小键上移到父节点
        Value borrowedKey = rightSibling.keys.remove(0);
        parent.keys.set(keyIndex, borrowedKey);
        
        return true;
    }
    
    // 与右兄弟合并
    public void mergeWithRightSibling(BPlusTreeInternalNode rightSibling, Value separatorKey) {
        // 添加分隔键
        keys.add(separatorKey);
        
        // 添加右兄弟的所有键
        keys.addAll(rightSibling.keys);
        
        // 添加右兄弟的所有子节点
        for (BPlusTreeNode child : rightSibling.children) {
            child.setParent(this);
            children.add(child);
        }
        
        // 清空右兄弟（可选，因为它将被删除）
        rightSibling.keys.clear();
        rightSibling.children.clear();
    }
    
    // 改进的删除子节点方法
    public void removeChild(BPlusTreeNode child, Value separatorKey) {
        int childIndex = children.indexOf(child);
        if (childIndex == -1) return;
        
        children.remove(childIndex);
        
        // 移除对应的分隔键
        if (separatorKey != null) {
            keys.remove(separatorKey);
        } else {
            // 如果没有指定分隔键，根据子节点位置推断
            if (childIndex > 0 && childIndex - 1 < keys.size()) {
                keys.remove(childIndex - 1);
            } else if (childIndex < keys.size()) {
                keys.remove(childIndex);
            }
        }
    }
    
    // 获取指定子节点的分隔键
    public Value getSeparatorKey(BPlusTreeNode child) {
        int childIndex = children.indexOf(child);
        if (childIndex <= 0 || childIndex > keys.size()) {
            return null;
        }
        return keys.get(childIndex - 1);
    }
    
    // 更新分隔键
    public void updateSeparatorKey(Value oldKey, Value newKey) {
        int keyIndex = keys.indexOf(oldKey);
        if (keyIndex != -1) {
            keys.set(keyIndex, newKey);
        }
    }
    
    // 检查节点是否有足够的键进行借出
    public boolean canLendKey() {
        return keys.size() > (degree - 1) / 2;
    }
    
    // 获取最小键
    public Value getMinKey() {
        if (keys.isEmpty()) return null;
        return keys.get(0);
    }
    
    // 获取最大键
    public Value getMaxKey() {
        if (keys.isEmpty()) return null;
        return keys.get(keys.size() - 1);
    }
    
    // 查找键的位置
    public int findKeyPosition(Value key) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(key)) {
                return i;
            }
        }
        return -1;
    }
    
    // 获取子节点的索引
    public int getChildIndex(BPlusTreeNode child) {
        return children.indexOf(child);
    }
    
    // 验证内部节点的完整性
    public boolean validateIntegrity() {
        // 检查键和子节点数量关系
        if (children.size() != keys.size() + 1) {
            System.out.println("内部节点键和子节点数量不匹配: keys=" + keys.size() + 
                             ", children=" + children.size());
            return false;
        }
        
        // 检查所有子节点的父指针
        for (BPlusTreeNode child : children) {
            if (child.getParent() != this) {
                System.out.println("子节点的父指针错误");
                return false;
            }
        }
        
        // 检查键的顺序
        for (int i = 0; i < keys.size() - 1; i++) {
            if (keys.get(i).compareTo(keys.get(i + 1)) >= 0) {
                System.out.println("内部节点键顺序错误");
                return false;
            }
        }
        
        return true;
    }
    
    // Getters and setters
    public List<BPlusTreeNode> getChildren() { 
        return children; 
    }
    
    public Value getMiddleKeyForSplit() {
        return keys.get(keys.size() / 2);
    }
    
    // 设置子节点列表（仅用于测试）
    public void setChildren(List<BPlusTreeNode> children) {
        this.children = children;
        // 更新所有子节点的父指针
        for (BPlusTreeNode child : children) {
            child.setParent(this);
        }
    }
    
    // 添加子节点（不插入键）
    public void addChild(BPlusTreeNode child) {
        children.add(child);
        child.setParent(this);
    }
    
    // 在指定位置插入子节点
    public void insertChildAt(int index, BPlusTreeNode child) {
        children.add(index, child);
        child.setParent(this);
    }
    
    // 获取子节点数量
    public int getChildCount() {
        return children.size();
    }
    
    // 是否为空节点
    public boolean isEmpty() {
        return keys.isEmpty() && children.isEmpty();
    }
    
    // 清空节点
    public void clear() {
        keys.clear();
        for (BPlusTreeNode child : children) {
            child.setParent(null);
        }
        children.clear();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("InternalNode{keys=").append(keys);
        sb.append(", childCount=").append(children.size());
        sb.append(", degree=").append(degree).append("}");
        return sb.toString();
    }
}