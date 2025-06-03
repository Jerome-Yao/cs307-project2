package edu.sustech.cs307.BPlusTree;
import edu.sustech.cs307.BPlusTree.Node.BPlusTreeNode;
import edu.sustech.cs307.BPlusTree.Node.BPlusTreeLeafNode;
import edu.sustech.cs307.BPlusTree.Node.BPlusTreeInternalNode;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
import java.util.List;
import java.util.ArrayList;

public class BPlusTree {
    private BPlusTreeNode root;
    private int degree;
    
    public BPlusTree(int degree) {
        if (degree < 3) {
            throw new IllegalArgumentException("B+树的度数必须至少为3");
        }
        this.degree = degree;
        this.root = new BPlusTreeLeafNode(degree);
    }
    
    // 安全的值比较方法
    private int safeCompare(Value v1, Value v2) {
        try {
            return v1.compareTo(v2);
        } catch (ClassCastException e) {
            // 如果类型不匹配，抛出更友好的异常
            throw new IllegalArgumentException("无法比较不同类型的值: " + 
                v1.getClass().getSimpleName() + " 和 " + v2.getClass().getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException("值比较时发生错误: " + e.getMessage(), e);
        }
    }
    
    // 验证键类型一致性
    private void validateKeyType(Value newKey) {
        if (root.isLeaf()) {
            BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) root;
            if (!leaf.getKeys().isEmpty()) {
                Value existingKey = leaf.getKeys().get(0);
                if (!isSameType(newKey, existingKey)) {
                    throw new IllegalArgumentException("插入的键类型与现有键类型不匹配: " +
                        "新键类型=" + getValueType(newKey) + ", 现有键类型=" + getValueType(existingKey));
                }
            }
        }
    }
    
    // 检查两个值是否为同一类型
    private boolean isSameType(Value v1, Value v2) {
        return getValueType(v1).equals(getValueType(v2));
    }
    
    // 获取值的类型字符串
    private String getValueType(Value value) {
        if (value == null) return "null";
        Object val = value.getValue();
        if (val == null) return "null";
        return val.getClass().getSimpleName();
    }
    
    public void insert(Value key, RID rid) {
        try {
            // 验证键类型
            validateKeyType(key);
            
            BPlusTreeNode leaf = findLeaf(key);
            leaf.insert(key, rid);
            if (leaf.isNeedSplit()) {
                splitAndPropagate(leaf);
            }
        } catch (Exception e) {
            System.err.println("Error during insert: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private BPlusTreeNode findLeaf(Value key) {
        BPlusTreeNode current = root;
        
        while (!current.isLeaf()) {
            BPlusTreeInternalNode internal = (BPlusTreeInternalNode) current;
            current = internal.findChild(key);
        }
        
        return current;
    }
    
    private void splitAndPropagate(BPlusTreeNode node) {
        Value keyToPromote;
        BPlusTreeNode newNode = node.split();
        if (node.isLeaf()) {
            keyToPromote = newNode.getFirstKey();
        } else {
            keyToPromote = node.getLastKey();
            node.removeLastKey();
        }
        if (node == root) {
            BPlusTreeInternalNode newRoot = new BPlusTreeInternalNode(degree);
            newRoot.getChildren().add(node);
            newRoot.getKeys().add(keyToPromote);
            newRoot.getChildren().add(newNode);
            
            node.setParent(newRoot);
            newNode.setParent(newRoot);
            root = newRoot;
        } else {
            BPlusTreeInternalNode parent = (BPlusTreeInternalNode) node.getParent();
            newNode.setParent(parent);
            parent.insertChild(keyToPromote, newNode);
            
            if (parent.isNeedSplit()) {
                splitAndPropagate(parent);
            }
        }
    }
    
    public List<RID> search(Value key) {
        try {
            BPlusTreeNode node = findLeaf(key);
            if (node.isLeaf()) {
                BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) node;
                return leaf.search(key);
            } else {
                System.err.println("Error: Expected leaf node but found internal node during search.");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public RID searchSingle(Value key) {
        try {
            BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) findLeaf(key);
            return leaf.searchSingle(key);
        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
            return null;
        }
    }
    
    public List<RID> rangeSearch(Value startKey, Value endKey) {
        try {
            // 验证范围键类型一致性
            if (!isSameType(startKey, endKey)) {
                throw new IllegalArgumentException("范围查询的起始键和结束键类型不匹配");
            }
            
            BPlusTreeLeafNode startLeaf = (BPlusTreeLeafNode) findLeaf(startKey);
            return startLeaf.rangeSearch(startKey, endKey);
        } catch (Exception e) {
            System.err.println("Error during range search: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public boolean delete(Value key) {
        try {
            //  System.out.println("--------------------------------delete key-----------------: "+key.getType());
            // System.out.println("--------------------------------delete key-----------------: "+key.getValue());
           BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) findLeaf(key);
            // System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
            // System.out.println("------------------------leaf:--------------------------------- " );
//           System.out.println("------------------------leaf: " + leaf.getKeys());
        //    System.out.println("------------------------leaf: " + leaf.getFirstKey());
            //System.out.println("------------------------leaf: " + leaf.getValues().getFirst().getValue());
            //System.out.println("------------------------leaf: " + leaf.getValues().getFirst().getType());
            // if (leaf.getKeys() != null && !leaf.getKeys().isEmpty()) {
                // System.out.println("------------------------leaf first key: " + leaf.getKeys().get(0));
            // } else {
                // System.out.println("------------------------leaf has no keys or keys is null");
            // }
//            System.out.println("------------------------key: " + key);
//            System.out.println("------------------------values: " + leaf.getValues());
            // System.out.println("------------------------kkeyfffffffffffffffffffffffff " + key.getValue());
           boolean deleted = leaf.delete(key);
            
           if (deleted && leaf.needsRebalance() && leaf != root) {
               rebalanceAfterDeletion(leaf);

            }
//
           return deleted;
            // return true;
        } catch (Exception e) {
            System.err.println("Error during delete: " + e.getMessage());
            return false;
        }
    }
    
    private void rebalanceAfterDeletion(BPlusTreeLeafNode node) {
        BPlusTreeInternalNode parent = (BPlusTreeInternalNode) node.getParent();
        if (parent == null) return;
        
        int nodeIndex = parent.getChildren().indexOf(node);
        
        // 尝试从左兄弟借键
        if (nodeIndex > 0) {
            BPlusTreeLeafNode leftSibling = (BPlusTreeLeafNode) parent.getChildren().get(nodeIndex - 1);
            if (node.borrowFromLeft(leftSibling, parent, nodeIndex - 1)) {
                return;
            }
        }
        
        // 尝试从右兄弟借键
        if (nodeIndex < parent.getChildren().size() - 1) {
            BPlusTreeLeafNode rightSibling = (BPlusTreeLeafNode) parent.getChildren().get(nodeIndex + 1);
            if (node.borrowFromRight(rightSibling, parent, nodeIndex)) {
                return;
            }
        }
        
        // 需要合并
        if (nodeIndex > 0) {
            BPlusTreeLeafNode leftSibling = (BPlusTreeLeafNode) parent.getChildren().get(nodeIndex - 1);
            leftSibling.mergeWithRight(node);
            parent.removeChild(node, null);
        } else if (nodeIndex < parent.getChildren().size() - 1) {
            BPlusTreeLeafNode rightSibling = (BPlusTreeLeafNode) parent.getChildren().get(nodeIndex + 1);
            node.mergeWithRight(rightSibling);
            parent.removeChild(rightSibling, null);
        }
        
        // 检查父节点是否需要重新平衡
        if (parent.needsRebalance() && parent != root) {
            rebalanceInternalAfterDeletion(parent);
        } else if (parent.getChildren().size() == 1 && parent == root) {
            // 根节点只有一个子节点，则子节点成为新根
            root = parent.getChildren().get(0);
            root.setParent(null);
        }
    }
    
    private void rebalanceInternalAfterDeletion(BPlusTreeInternalNode node) {
        BPlusTreeInternalNode parent = (BPlusTreeInternalNode) node.getParent();
        if (parent == null) return;
        
        int nodeIndex = parent.getChildren().indexOf(node);
        
        // 尝试从兄弟节点借键
        if (nodeIndex > 0) {
            BPlusTreeInternalNode leftSibling = (BPlusTreeInternalNode) parent.getChildren().get(nodeIndex - 1);
            if (node.borrowFromLeftSibling(leftSibling, parent, nodeIndex - 1)) {
                return;
            }
        }
        
        if (nodeIndex < parent.getChildren().size() - 1) {
            BPlusTreeInternalNode rightSibling = (BPlusTreeInternalNode) parent.getChildren().get(nodeIndex + 1);
            if (node.borrowFromRightSibling(rightSibling, parent, nodeIndex)) {
                return;
            }
        }
        
        // 需要合并内部节点
        if (nodeIndex > 0) {
            BPlusTreeInternalNode leftSibling = (BPlusTreeInternalNode) parent.getChildren().get(nodeIndex - 1);
            leftSibling.mergeWithRightSibling(node, parent.getKeys().get(nodeIndex - 1));
            parent.removeChild(node, parent.getKeys().get(nodeIndex - 1));
        } else if (nodeIndex < parent.getChildren().size() - 1) {
            BPlusTreeInternalNode rightSibling = (BPlusTreeInternalNode) parent.getChildren().get(nodeIndex + 1);
            node.mergeWithRightSibling(rightSibling, parent.getKeys().get(nodeIndex));
            parent.removeChild(rightSibling, parent.getKeys().get(nodeIndex));
        }
        
        // 递归检查父节点
        if (parent.needsRebalance() && parent != root) {
            rebalanceInternalAfterDeletion(parent);
        } else if (parent.getChildren().size() == 1 && parent == root) {
            root = parent.getChildren().get(0);
            root.setParent(null);
        }
    }
    
    // 安全的值转字符串方法
    private String safeValueToString(Value value) {
        if (value == null) return "null";
        try {
            Object val = value.getValue();
            if (val == null) return "null";
            if (val instanceof String) {
                return "\"" + val + "\""; // 字符串加引号
            } else if (val instanceof Double) {
                return String.format("%.2f", (Double) val); // 格式化双精度数
            } else {
                return val.toString();
            }
        } catch (Exception e) {
            return "[Error: " + e.getMessage() + "]";
        }
    }
    
    // 安全的RID转字符串方法
    private String safeRIDToString(RID rid) {
        if (rid == null) return "null";
        try {
            return rid.toString();
        } catch (Exception e) {
            return "[RID Error: " + e.getMessage() + "]";
        }
    }
    
    // 打印树结构
    public void printTree() {
        if (root == null) {
            System.out.println("Empty tree");
            return;
        }
        
        try {
            System.out.println("\n=== B+树结构 ===");
            List<List<BPlusTreeNode>> levels = new ArrayList<>();
            List<BPlusTreeNode> currentLevel = new ArrayList<>();
            currentLevel.add(root);
            
            while (!currentLevel.isEmpty()) {
                levels.add(new ArrayList<>(currentLevel));
                List<BPlusTreeNode> nextLevel = new ArrayList<>();
                
                for (BPlusTreeNode node : currentLevel) {
                    if (!node.isLeaf()) {
                        BPlusTreeInternalNode internal = (BPlusTreeInternalNode) node;
                        nextLevel.addAll(internal.getChildren());
                    }
                }
                currentLevel = nextLevel;
            }
            
            for (int i = 0; i < levels.size(); i++) {
                System.out.print("Level " + i + ": ");
                for (BPlusTreeNode node : levels.get(i)) {
                    if (node.isLeaf()) {
                        BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) node;
                        System.out.print("[");
                        for (int j = 0; j < leaf.getKeys().size(); j++) {
                            System.out.print(safeValueToString(leaf.getKeys().get(j)) + ":" + 
                                safeRIDToString(leaf.getValues().get(j)));
                            if (j < leaf.getKeys().size() - 1) System.out.print(", ");
                        }
                        System.out.print("] ");
                    } else {
                        System.out.print("[");
                        for (int j = 0; j < node.getKeys().size(); j++) {
                            System.out.print(safeValueToString(node.getKeys().get(j)));
                            if (j < node.getKeys().size() - 1) System.out.print(", ");
                        }
                        System.out.print("] ");
                    }
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.err.println("Error during printTree: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 打印叶子节点链表
    public void printLeafChain() {
        try {
            BPlusTreeNode current = root;
            while (!current.isLeaf()) {
                BPlusTreeInternalNode internal = (BPlusTreeInternalNode) current;
                current = internal.getChildren().get(0);
            }
            
            System.out.print("\n叶子节点链表: ");
            BPlusTreeLeafNode leafCurrent = (BPlusTreeLeafNode) current;
            while (leafCurrent != null) {
                System.out.print("[");
                for (int i = 0; i < leafCurrent.getKeys().size(); i++) {
                    System.out.print(safeValueToString(leafCurrent.getKeys().get(i)));
                    if (i < leafCurrent.getKeys().size() - 1) System.out.print(", ");
                }
                System.out.print("]");
                if (leafCurrent.getNext() != null) {
                    System.out.print(" -> ");
                }
                leafCurrent = leafCurrent.getNext();
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error during printLeafChain: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 改进的验证方法
    public boolean validate() {
        try {
            return validateNode(root, null, null) && validateLeafChain();
        } catch (Exception e) {
            System.out.println("验证时出现异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean validateNode(BPlusTreeNode node, Value minKey, Value maxKey) {
        if (node == null) return true;
        
        try {
            // 检查键的顺序
            for (int i = 0; i < node.getKeys().size() - 1; i++) {
                if (safeCompare(node.getKeys().get(i), node.getKeys().get(i + 1)) >= 0) {
                    System.out.println("键顺序错误在节点中: " + safeValueToString(node.getKeys().get(i)) + 
                        " >= " + safeValueToString(node.getKeys().get(i + 1)));
                    return false;
                }
            }
            
            // 检查键的范围
            for (Value key : node.getKeys()) {
                if (minKey != null && safeCompare(key, minKey) < 0) {
                    System.out.println("键 " + safeValueToString(key) + " 小于最小值 " + safeValueToString(minKey));
                    return false;
                }
                if (maxKey != null && safeCompare(key, maxKey) > 0) {
                    System.out.println("键 " + safeValueToString(key) + " 大于等于最大值 " + safeValueToString(maxKey));
                    return false;
                }
            }
            
            if (!node.isLeaf()) {
                BPlusTreeInternalNode internal = (BPlusTreeInternalNode) node;
                
                // 检查子节点数量：应该比键的数量多1
                if (internal.getChildren().size() != node.getKeys().size() + 1) {
                    System.out.println("内部节点子节点数量错误: keys=" + node.getKeys().size() + 
                        ", children=" + internal.getChildren().size());
                    return false;
                }
                
                // 检查父子关系
                for (BPlusTreeNode child : internal.getChildren()) {
                    if (child.getParent() != node) {
                        System.out.println("父子关系错误");
                        return false;
                    }
                }
                
                // 递归检查子节点，传递正确的范围
                List<BPlusTreeNode> children = internal.getChildren();
                List<Value> keys = node.getKeys();
                
                // 第一个子节点
                if (!validateNode(children.get(0), minKey, keys.size() > 0 ? keys.get(0) : null)) {
                    return false;
                }
                
                // 中间子节点
                for (int i = 1; i < children.size() - 1; i++) {
                    if (!validateNode(children.get(i), keys.get(i - 1), keys.get(i))) {
                        return false;
                    }
                }
                
                // 最后一个子节点
                if (children.size() > 1) {
                    if (!validateNode(children.get(children.size() - 1), 
                        keys.size() > 0 ? keys.get(keys.size() - 1) : null, maxKey)) {
                        return false;
                    }
                }
            } else {
                BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) node;
                // 叶子节点：检查键值对数量
                if (node.getKeys().size() != leaf.getValues().size()) {
                    System.out.println("叶子节点键值数量不匹配: keys=" + node.getKeys().size() + 
                        ", values=" + leaf.getValues().size());
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            System.out.println("验证节点时出现异常: " + e.getMessage());
            return false;
        }
    }
    
    private boolean validateLeafChain() {
        try {
            if (root.isLeaf()) {
                // 检查叶子节点的父节点是否为null（只有一个叶子节点的情况）
                if (root.getParent() != null) {
                    System.out.println("只有一个叶子节点时，父节点应为null");
                    return false;
                }
                return true; // 只有一个叶子节点
            }
            
            // 找到最左边的叶子节点
            BPlusTreeNode current = root;
            while (!current.isLeaf()) {
                BPlusTreeInternalNode internal = (BPlusTreeInternalNode) current;
                if (internal.getChildren().isEmpty()) {
                    System.out.println("内部节点没有子节点");
                    return false;
                }
                current = internal.getChildren().get(0);
            }
            
            // 检查叶子链表的顺序
            Value lastKey = null;
            BPlusTreeLeafNode leafCurrent = (BPlusTreeLeafNode) current;
            while (leafCurrent != null) {
                for (Value key : leafCurrent.getKeys()) {
                    if (lastKey != null && safeCompare(key, lastKey) <= 0) {
                        System.out.println("叶子链表顺序错误: " + safeValueToString(lastKey) + 
                            " >= " + safeValueToString(key));
                        return false;
                    }
                    lastKey = key;
                }
                leafCurrent = leafCurrent.getNext();
            }
            
            return true;
        } catch (Exception e) {
            System.out.println("验证叶子链表时出现异常: " + e.getMessage());
            return false;
        }
    }
    
    public BPlusTreeNode getRoot() {
        return root;
    }
    
    // 测试主方法
    public static void main(String[] args) {
        System.out.println("=== B+树测试程序 ===");
        
        try {
            // 创建一个度数为3的B+树
            BPlusTree tree = new BPlusTree(3);

            // 测试整数类型
//            System.out.println("\n1. 测试整数类型插入...");
//            for (int i = 1; i <= 10; i++) {
//                tree.insert(new Value((long) i), new RID(i / 4 + 1, i % 4));
//            }
//
//            tree.printTree();
//            tree.printLeafChain();
//            System.out.println("tree delete 5: " + tree.delete(new Value(5L)));
//
//            // 搜索测试
//            System.out.println("\n2. 搜索测试:");
//            for (int i = 1; i <= 14; i++) {
//                RID result = tree.searchSingle(new Value((long) i));
//                System.out.println("搜索键 " + i + ": " + result);
//            }
//
//            // 范围查询测试
//            System.out.println("\n3. 范围查询测试:");
//            List<RID> rangeResult = tree.rangeSearch(new Value(2L), new Value(4L));
//            System.out.println("范围查询 (2 到 4): " + rangeResult);
//
//            System.out.println("\n验证树的完整性: " + tree.validate());
            
            // 测试字符串类型
            System.out.println("\n4. 测试字符串类型 (新树):");
            BPlusTree stringTree = new BPlusTree(3);
            String[] names = {"Alice", "Bob", "Charlie", "David", "Eve"};
            for (int i = 0; i < names.length; i++) {
                stringTree.insert(new Value(names[i]), new RID(i + 1, 0));
            }
            System.out.println("-----wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww-------------string delete Bob: " + stringTree.delete(new Value("Bob")));
            stringTree.printTree();
            stringTree.printLeafChain();
            
            // 测试双精度类型
//            System.out.println("\n5. 测试双精度类型 (新树):");
//            BPlusTree doubleTree = new BPlusTree(3);
//            double[] values = {1.5, 2.7, 3.1, 4.8, 5.2};
//            for (int i = 0; i < values.length; i++) {
//                doubleTree.insert(new Value(values[i]), new RID(i + 1, 0));
//            }
//            System.out.println("double test---------------------"+doubleTree.delete(new Value(1.5)));
//            doubleTree.printTree();
//            doubleTree.printLeafChain();
//
//            System.out.println("\n验证字符串树: " + stringTree.validate());
//            System.out.println("验证双精度树: " + doubleTree.validate());
//
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}