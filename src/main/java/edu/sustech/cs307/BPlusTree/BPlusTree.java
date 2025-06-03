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
    
    public void insert(Value key, RID rid) {
        try {
            BPlusTreeNode leaf = findLeaf(key);
            leaf.insert(key, rid);
            
            if (leaf.isFull()) {
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
            current = internal.findChild(key); //第一个大于等于key的子节点
        }
        
        return current;
    }
    
    private void splitAndPropagate(BPlusTreeNode node) {
        BPlusTreeNode newNode = node.split();
        Value keyToPromote;
        
        if (node.isLeaf()) {
            // 叶子节点分裂：提升的键是新节点的第一个键
            keyToPromote = newNode.getFirstKey();
        } else {
            // 内部节点分裂：提升的键是中间键，需要从原节点移除
            BPlusTreeInternalNode internalNode = (BPlusTreeInternalNode) node;
            keyToPromote = internalNode.getPromotedKey(); // 需要实现这个方法
            // internalNode.getKeys().remove(keyToPromote);
        }
        
        if (node == root) {
            // 创建新根节点
            BPlusTreeInternalNode newRoot = new BPlusTreeInternalNode(degree);
            newRoot.getChildren().add(node);
            newRoot.getKeys().add(keyToPromote);
            newRoot.getChildren().add(newNode);
            
            // 设置父子关系
            node.setParent(newRoot);
            newNode.setParent(newRoot);
            root = newRoot;
        } else {
            // 将新节点和提升的键插入到父节点
            BPlusTreeInternalNode parent = (BPlusTreeInternalNode) node.getParent();
            newNode.setParent(parent);
            parent.insertChild(keyToPromote, newNode);
            
            if (parent.isFull()) {
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
            BPlusTreeLeafNode startLeaf = (BPlusTreeLeafNode) findLeaf(startKey);
            return startLeaf.rangeSearch(startKey, endKey);
        } catch (Exception e) {
            System.err.println("Error during range search: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public boolean delete(Value key) {
        try {
            BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) findLeaf(key);
            boolean deleted = leaf.delete(key);
            
            if (deleted && leaf.needsRebalance() && leaf != root) {
                rebalanceAfterDeletion(leaf);
            }
            
            return deleted;
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
    
    // 打印树结构
    public void printTree() {
        if (root == null) {
            System.out.println("Empty tree");
            return;
        }
        
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
                        System.out.print(leaf.getKeys().get(j) + ":" + leaf.getValues().get(j));
                        if (j < leaf.getKeys().size() - 1) System.out.print(", ");
                    }
                    System.out.print("] ");
                } else {
                    System.out.print("[");
                    for (int j = 0; j < node.getKeys().size(); j++) {
                        System.out.print(node.getKeys().get(j));
                        if (j < node.getKeys().size() - 1) System.out.print(", ");
                    }
                    System.out.print("] ");
                }
            }
            System.out.println();
        }
    }
    
    // 打印叶子节点链表
    public void printLeafChain() {
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
                System.out.print(leafCurrent.getKeys().get(i));
                if (i < leafCurrent.getKeys().size() - 1) System.out.print(", ");
            }
            System.out.print("]");
            if (leafCurrent.getNext() != null) {
                System.out.print(" -> ");
            }
            leafCurrent = leafCurrent.getNext();
        }
        System.out.println();
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
        
        // 检查键的顺序
        for (int i = 0; i < node.getKeys().size() - 1; i++) {
            if (node.getKeys().get(i).compareTo(node.getKeys().get(i + 1)) >= 0) {
                System.out.println("键顺序错误在节点中: " + node.getKeys().get(i) + " >= " + node.getKeys().get(i + 1));
                return false;
            }
        }
        if (maxKey != null){
            System.out.println("键顺序正确在节点中: " + node.getKeys());
        }
        // 检查键的范围
        for (Value key : node.getKeys()) {
            if (minKey != null && key.compareTo(minKey) < 0) {
                System.out.println("键 " + key + " 小于最小值 " + minKey);
                return false;
            }
            if (maxKey != null && key.compareTo(maxKey) > 0) {
                // System.out.println("fffffffffffffffffffffffffffffffffff");
                // System.out.println(maxKey == null ? "最大值未定义" : "键 " + key + " 大于等于最大值 " + maxKey);
                System.out.println("键 " + key + " 大于等于最大值 " + maxKey);
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
    }
    
    private boolean validateLeafChain() {
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
                if (lastKey != null && key.compareTo(lastKey) <= 0) {
                    System.out.println("叶子链表顺序错误: " + lastKey + " >= " + key);
                    return false;
                }
                lastKey = key;
            }
            leafCurrent = leafCurrent.getNext();
        }
        
        return true;
    }
    
    public BPlusTreeNode getRoot() {
        return root;
    }
    
    // 测试主方法
    public static void main(String[] args) {
        System.out.println("=== B+树测试程序 ===");
        
        // 创建一个度数为4的B+树
        BPlusTree tree = new BPlusTree(3);

        // 插入测试数据
        //System.out.println("\n1. 插入键 1-10...");
        for (int i = 1; i <= 10; i++) {
//            System.out.println("插入键 " + i + " 前验证: " + tree.validate());
            tree.insert(new Value((long) i), new RID(i / 4 + 1, i % 4));
//            System.out.println("插入键 " + i + " 后验证: " + tree.validate());
            // if (!tree.validate()) {
            //     System.out.println("插入键 " + i + " 后验证失败，停止测试");
            //     break;
            // }
        }
        
        tree.printTree();
        tree.printLeafChain();
        
    //     // 搜索测试
         System.out.println("\n2. 搜索测试:");
         for (int i = 1; i <= 14; i++) {
             RID result = tree.searchSingle(new Value((long) i));
             System.out.println("搜索键 " + i + ": " + result);
         }
        
    //     // 范围查询测试
    //     System.out.println("\n3. 范围查询测试:");
    //     List<RID> rangeResult = tree.rangeSearch(new Value(3L), new Value(7L));
    //     System.out.println("范围查询 (3 到 7): " + rangeResult);
        
    //     // 删除测试
    //     System.out.println("\n4. 删除测试:");
    //     System.out.println("删除键 3: " + tree.delete(new Value(3L)));
    //     System.out.println("删除键 15: " + tree.delete(new Value(15L)));
        
    //     System.out.println("\n删除后的树结构:");
    //     tree.printTree();
    //     tree.printLeafChain();
        
    //     System.out.println("\n验证删除: 搜索键 3: " + tree.searchSingle(new Value(3L)));
        
    //     // 验证树的完整性
    //     System.out.println("\n5. 树的完整性验证: " + tree.validate());
        
    //     // 额外插入测试，触发更多分裂
    //     System.out.println("\n6. 额外插入测试 (11-20):");
    //     for (int i = 11; i <= 20; i++) {
    //         tree.insert(new Value((long) i), new RID(i / 4 + 1, i % 4));
    //         System.out.println("插入键 " + i + " 后验证: " + tree.validate());
    //     }
        
    //     tree.printTree();
    //     tree.printLeafChain();
    //     System.out.println("最终验证: " + tree.validate());
    }
}