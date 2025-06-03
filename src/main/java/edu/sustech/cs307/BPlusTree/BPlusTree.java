package edu.sustech.cs307.BPlusTree;
import java.util.ArrayList;
import java.util.List;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
import edu.sustech.cs307.index.InMemoryOrderedIndex;

class Node {
    List<Value> keys;
    List<Node> children;    // 内部节点的子节点指针
    List<RID> rids;         // 叶子节点的RID信息
    boolean isLeaf;
    Node next;              // 叶子节点的next指针，用于链接叶子节点
    Node parent;            // 父节点指针，便于查找和维护

    public Node(boolean isLeaf) {
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.rids = new ArrayList<>();
        this.isLeaf = isLeaf;
        this.next = null;
        this.parent = null;
    }
}

public class BPlusTree {
    private Node root;
    private int degree;     // 最大子节点数，键的最大数量为degree-1

    public BPlusTree(int degree) {
        if (degree < 3) {
            throw new IllegalArgumentException("B+树的度数必须至少为3");
        }
        this.root = new Node(true);
        this.degree = degree;
    }

    public RID search(Value key) {
        try {
            Node leaf = findLeaf(key);
            
            // 在叶子节点中查找
            for (int i = 0; i < leaf.keys.size(); i++) {
                if (leaf.keys.get(i).equals(key)) {
                    return leaf.rids.get(i);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
            return null;
        }
    }

    // 找到应该包含给定键的叶子节点
    private Node findLeaf(Value key) {
        Node current = root;
        
        while (!current.isLeaf) {
            int i = 0;
            // 找到第一个大于等于key的位置
            while (i < current.keys.size() && key.compareTo(current.keys.get(i)) >= 0) {
                i++;
            }
            current = current.children.get(i);
        }
        
        return current;
    }

    public void insert(Value key, RID rid) {
        try {
            Node leaf = findLeaf(key);
            
            // 检查是否已存在该键
            for (int i = 0; i < leaf.keys.size(); i++) {
                if (leaf.keys.get(i).equals(key)) {
                    // 更新已存在的键
                    leaf.rids.set(i, rid);
                    return;
                }
            }
            
            insertIntoLeaf(leaf, key, rid);
            
            // 如果叶子节点溢出，需要分裂
            if (leaf.keys.size() >= degree) {
                splitLeaf(leaf);
            }
        } catch (Exception e) {
            System.err.println("Error during insert: " + e.getMessage());
        }
    }

    private void insertIntoLeaf(Node leaf, Value key, RID rid) {
        int pos = 0;
        // 找到插入位置，保持有序
        while (pos < leaf.keys.size() && key.compareTo(leaf.keys.get(pos)) > 0) {
            pos++;
        }
        
        leaf.keys.add(pos, key);
        leaf.rids.add(pos, rid);
    }

    private void splitLeaf(Node leaf) {
        int mid = degree / 2;
        
        // 创建新的叶子节点
        Node newLeaf = new Node(true);
        newLeaf.parent = leaf.parent;
        
        // 移动一半的键和RID到新节点
        newLeaf.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        newLeaf.rids.addAll(leaf.rids.subList(mid, leaf.rids.size()));
        
        // 删除原节点中已移动的部分
        leaf.keys.subList(mid, leaf.keys.size()).clear();
        leaf.rids.subList(mid, leaf.rids.size()).clear();
        
        // 更新链表指针
        newLeaf.next = leaf.next;
        leaf.next = newLeaf;
        
        // 向父节点插入新键
        Value promoteKey = newLeaf.keys.get(0);  // B+树中，叶子节点的第一个键提升到父节点
        
        if (leaf.parent == null) {
            // 根节点分裂，创建新根
            createNewRoot(leaf, newLeaf, promoteKey);
        } else {
            insertIntoParent(leaf.parent, newLeaf, promoteKey);
        }
    }

    private void createNewRoot(Node left, Node right, Value key) {
        Node newRoot = new Node(false);
        newRoot.keys.add(key);
        newRoot.children.add(left);
        newRoot.children.add(right);
        
        left.parent = newRoot;
        right.parent = newRoot;
        
        this.root = newRoot;
    }

    private void insertIntoParent(Node parent, Node newChild, Value key) {
        int pos = 0;
        // 找到插入位置
        while (pos < parent.keys.size() && key.compareTo(parent.keys.get(pos)) > 0) {
            pos++;
        }
        
        parent.keys.add(pos, key);
        parent.children.add(pos + 1, newChild);
        newChild.parent = parent;
        
        // 如果父节点也溢出，需要分裂
        if (parent.keys.size() >= degree) {
            splitInternal(parent);
        }
    }

    private void splitInternal(Node node) {
        int mid = degree / 2;
        
        // 创建新的内部节点
        Node newNode = new Node(false);
        newNode.parent = node.parent;
        
        // 提升中间键
        Value promoteKey = node.keys.get(mid);
        
        // 移动键和子节点
        newNode.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        newNode.children.addAll(node.children.subList(mid + 1, node.children.size()));
        
        // 更新子节点的父指针
        for (Node child : newNode.children) {
            child.parent = newNode;
        }
        
        // 删除原节点中已移动的部分
        node.keys.subList(mid, node.keys.size()).clear();
        node.children.subList(mid + 1, node.children.size()).clear();
        
        if (node.parent == null) {
            // 根节点分裂
            createNewRoot(node, newNode, promoteKey);
        } else {
            insertIntoParent(node.parent, newNode, promoteKey);
        }
    }

    public boolean delete(Value key) {
        try {
            Node leaf = findLeaf(key);
            
            // 在叶子节点中查找并删除
            for (int i = 0; i < leaf.keys.size(); i++) {
                if (leaf.keys.get(i).equals(key)) {
                    leaf.keys.remove(i);
                    leaf.rids.remove(i);
                    
                    // 检查是否需要重新平衡
                    if (leaf.keys.size() < (degree - 1) / 2 && leaf != root) {
                        rebalanceAfterDeletion(leaf);
                    }
                    return true;
                }
            }
            return false;  // 未找到键
        } catch (Exception e) {
            System.err.println("Error during delete: " + e.getMessage());
            return false;
        }
    }

    private void rebalanceAfterDeletion(Node node) {
        int minKeys = (degree - 1) / 2;
        
        if (node.keys.size() >= minKeys) {
            return;  // 不需要重新平衡
        }
        
        Node parent = node.parent;
        if (parent == null) {
            return;  // 根节点
        }
        
        int nodeIndex = parent.children.indexOf(node);
        
        // 尝试从左兄弟借键
        if (nodeIndex > 0) {
            Node leftSibling = parent.children.get(nodeIndex - 1);
            if (leftSibling.keys.size() > minKeys) {
                borrowFromLeft(node, leftSibling, parent, nodeIndex - 1);
                return;
            }
        }
        
        // 尝试从右兄弟借键
        if (nodeIndex < parent.children.size() - 1) {
            Node rightSibling = parent.children.get(nodeIndex + 1);
            if (rightSibling.keys.size() > minKeys) {
                borrowFromRight(node, rightSibling, parent, nodeIndex);
                return;
            }
        }
        
        // 需要合并节点
        if (nodeIndex > 0) {
            Node leftSibling = parent.children.get(nodeIndex - 1);
            mergeNodes(leftSibling, node, parent, nodeIndex - 1);
        } else {
            Node rightSibling = parent.children.get(nodeIndex + 1);
            mergeNodes(node, rightSibling, parent, nodeIndex);
        }
    }

    private void borrowFromLeft(Node node, Node leftSibling, Node parent, int parentKeyIndex) {
        if (node.isLeaf) {
            // 叶子节点：直接移动最大的键和RID
            Value borrowedKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);
            RID borrowedRid = leftSibling.rids.remove(leftSibling.rids.size() - 1);
            
            node.keys.add(0, borrowedKey);
            node.rids.add(0, borrowedRid);
            
            // 更新父节点的键
            parent.keys.set(parentKeyIndex, borrowedKey);
        } else {
            // 内部节点
            Value parentKey = parent.keys.get(parentKeyIndex);
            Value borrowedKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);
            Node borrowedChild = leftSibling.children.remove(leftSibling.children.size() - 1);
            
            node.keys.add(0, parentKey);
            node.children.add(0, borrowedChild);
            borrowedChild.parent = node;
            
            parent.keys.set(parentKeyIndex, borrowedKey);
        }
    }

    private void borrowFromRight(Node node, Node rightSibling, Node parent, int parentKeyIndex) {
        if (node.isLeaf) {
            // 叶子节点
            Value borrowedKey = rightSibling.keys.remove(0);
            RID borrowedRid = rightSibling.rids.remove(0);
            
            node.keys.add(borrowedKey);
            node.rids.add(borrowedRid);
            
            // 更新父节点的键
            parent.keys.set(parentKeyIndex, rightSibling.keys.get(0));
        } else {
            // 内部节点
            Value parentKey = parent.keys.get(parentKeyIndex);
            Value borrowedKey = rightSibling.keys.remove(0);
            Node borrowedChild = rightSibling.children.remove(0);
            
            node.keys.add(parentKey);
            node.children.add(borrowedChild);
            borrowedChild.parent = node;
            
            parent.keys.set(parentKeyIndex, borrowedKey);
        }
    }

    private void mergeNodes(Node left, Node right, Node parent, int parentKeyIndex) {
        if (left.isLeaf) {
            // 合并叶子节点
            left.keys.addAll(right.keys);
            left.rids.addAll(right.rids);
            left.next = right.next;
        } else {
            // 合并内部节点
            left.keys.add(parent.keys.get(parentKeyIndex));
            left.keys.addAll(right.keys);
            left.children.addAll(right.children);
            
            // 更新子节点的父指针
            for (Node child : right.children) {
                child.parent = left;
            }
        }
        
        // 从父节点删除键和右子节点
        parent.keys.remove(parentKeyIndex);
        parent.children.remove(parentKeyIndex + 1);
        
        // 如果父节点变为空且不是根节点
        if (parent.keys.isEmpty() && parent == root) {
            root = left;
            left.parent = null;
        } else if (parent.keys.size() < (degree - 1) / 2 && parent != root) {
            rebalanceAfterDeletion(parent);
        }
    }

    // 范围查询
    public List<RID> rangeSearch(Value startKey, Value endKey) {
        List<RID> result = new ArrayList<>();
        
        Node current = findLeaf(startKey);
        
        while (current != null) {
            for (int i = 0; i < current.keys.size(); i++) {
                Value key = current.keys.get(i);
                if (key.compareTo(startKey) >= 0 && key.compareTo(endKey) <= 0) {
                    result.add(current.rids.get(i));
                } else if (key.compareTo(endKey) > 0) {
                    return result;
                }
            }
            current = current.next;
        }
        
        return result;
    }

    // 打印树结构
    public void printTree() {
        if (root == null) {
            System.out.println("Empty tree");
            return;
        }
        
        List<List<Node>> levels = new ArrayList<>();
        List<Node> currentLevel = new ArrayList<>();
        currentLevel.add(root);
        
        while (!currentLevel.isEmpty()) {
            levels.add(new ArrayList<>(currentLevel));
            List<Node> nextLevel = new ArrayList<>();
            
            for (Node node : currentLevel) {
                if (!node.isLeaf) {
                    nextLevel.addAll(node.children);
                }
            }
            currentLevel = nextLevel;
        }
        
        for (int i = 0; i < levels.size(); i++) {
            System.out.print("Level " + i + ": ");
            for (Node node : levels.get(i)) {
                if (node.isLeaf) {
                    System.out.print("[");
                    for (int j = 0; j < node.keys.size(); j++) {
                        System.out.print(node.keys.get(j) + ":" + node.rids.get(j));
                        if (j < node.keys.size() - 1) System.out.print(", ");
                    }
                    System.out.print("] ");
                } else {
                    System.out.print("[" + String.join(", ", 
                        node.keys.stream().map(String::valueOf).toArray(String[]::new)) + "] ");
                }
            }
            System.out.println();
        }
    }

    // 打印叶子节点链表
    public void printLeafChain() {
        Node current = root;
        while (!current.isLeaf) {
            current = current.children.get(0);
        }
        
        System.out.print("Leaf chain: ");
        while (current != null) {
            System.out.print("[" + String.join(", ", 
                current.keys.stream().map(String::valueOf).toArray(String[]::new)) + "]");
            if (current.next != null) {
                System.out.print(" -> ");
            }
            current = current.next;
        }
        System.out.println();
    }

    // 验证B+树的完整性
    public boolean validate() {
        return validateNode(root, null, null) && validateLeafChain();
    }

    private boolean validateNode(Node node, Value min, Value max) {
        // 检查键的顺序
        for (int i = 0; i < node.keys.size() - 1; i++) {
            if (node.keys.get(i).compareTo(node.keys.get(i + 1)) >= 0) {
                return false;
            }
        }
        
        // 检查键的范围
        for (Value key : node.keys) {
            if ((min != null && key.compareTo(min) < 0) || 
                (max != null && key.compareTo(max) > 0)) {
                return false;
            }
        }
        
        if (!node.isLeaf) {
            // 检查子节点数量
            if (node.children.size() != node.keys.size() + 1) {
                return false;
            }
            
            // 递归检查子节点
            for (int i = 0; i < node.children.size(); i++) {
                Value childMin = (i == 0) ? min : node.keys.get(i - 1);
                Value childMax = (i == node.keys.size()) ? max : node.keys.get(i);
                
                if (!validateNode(node.children.get(i), childMin, childMax)) {
                    return false;
                }
            }
        } else {
            // 叶子节点：检查RID数量
            if (node.keys.size() != node.rids.size()) {
                return false;
            }
        }
        
        return true;
    }

    private boolean validateLeafChain() {
        Node current = root;
        while (!current.isLeaf) {
            current = current.children.get(0);
        }
        
        Value lastKey = null;
        while (current != null) {
            for (Value key : current.keys) {
                if (lastKey != null && key.compareTo(lastKey) <= 0) {
                    return false;
                }
                lastKey = key;
            }
            current = current.next;
        }
        
        return true;
    }

    public static void main(String[] args) {
        // 创建一个度数为4的B+树
        BPlusTree tree = new BPlusTree(4);

        // 插入测试数据
        System.out.println("插入键 1-10...");
        for (int i = 1; i <= 10; i++) {
            tree.insert(new Value((long) i), new RID(i / 4 + 1, i % 4));
        }
        
        System.out.println("\n树结构:");
        tree.printTree();
        
        System.out.println("\n叶子节点链表:");
        tree.printLeafChain();
        
        // 搜索测试
        System.out.println("\n搜索测试:");
        System.out.println("搜索键 5: " + tree.search(new Value(5L)));
        System.out.println("搜索键 15: " + tree.search(new Value(15L)));
        
        // 范围查询测试
        System.out.println("\n范围查询 (3 到 7): " + tree.rangeSearch(new Value(3L), new Value(7L)));
        
        // 删除测试
        System.out.println("\n删除键 3...");
        tree.delete(new Value(3L));
        System.out.println("删除后的树结构:");
        tree.printTree();
        System.out.println("搜索键 3: " + tree.search(new Value(3L)));
        
        // 验证树的完整性
        System.out.println("\n树的完整性验证: " + tree.validate());
        InMemoryOrderedIndex test = new InMemoryOrderedIndex("/home/wgx/database/cs307-project2/src/test/testBPlusTree/test.json");
        test.printIndex();
    }
}