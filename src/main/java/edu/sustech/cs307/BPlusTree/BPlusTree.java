package edu.sustech.cs307.BPlusTree;
import java.util.ArrayList;
import java.util.List;
import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
class Node {
    List<Value> keys;
    List<Node> values;  // 对于内部节点，存储子节点指针
    List<RID> rids;     // 对于叶子节点，存储RID信息
    boolean leaf;
    Node next;          // 叶子节点的next指针，用于链接叶子节点

    public Node(boolean leaf) {
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.rids = new ArrayList<>();  // 初始化RID列表
        this.leaf = leaf;
        this.next = null;
    }
}

public class BPlusTree {
    private Node root;
    private int degree;

    public BPlusTree(int degree) {
        this.root = new Node(true);
        this.degree = degree;
    }

    public RID search(Value key) {
        try{
            Node currentNode = this.root;
            while (!currentNode.leaf) {
                int i = 0;
                while (i < currentNode.keys.size()) {
                    if (key.compareTo(currentNode.keys.get(i)) < 0) {
                        break;
                    }
                    i += 1;
                }
                currentNode = currentNode.values.get(i);
            }
            int i = 0;
            while (i < currentNode.keys.size()) {
                if (currentNode.keys.get(i).equals(key)) {
                    return currentNode.rids.get(i);  // 返回对应的RID
                }
                i += 1;
            }
            return null;  // 未找到则返回null
        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
            return null;
        }
    }

    public void insert(Value key, RID rid) {  // 修改insert方法，接受RID参数
        Node currentNode = this.root;
        if (currentNode.keys.size() == 2 * this.degree) {
            Node newRoot = new Node(false);
            this.root = newRoot;
            newRoot.values.add(currentNode);
            this.split(newRoot, 0, currentNode);
            this.insertNonFull(newRoot, key, rid);
        } else {
            this.insertNonFull(currentNode, key, rid);
        }
    }

    private void insertNonFull(Node currentNode, Value key, RID rid) {
        try {
            int i = 0;
            while (i < currentNode.keys.size()) {
                if (key.compareTo(currentNode.keys.get(i)) < 0) {
                    break;
                }
                i += 1;
            }
            if (currentNode.leaf) {
                currentNode.keys.add(i, key);
                currentNode.rids.add(i, rid);  // 在叶子节点中同时插入RID
            } else {
                if (currentNode.values.get(i).keys.size() == 2 * this.degree) {
                    this.split(currentNode, i, currentNode.values.get(i));
                    if (key.compareTo(currentNode.keys.get(i)) > 0) {
                        i += 1;
                    }
                }
                this.insertNonFull(currentNode.values.get(i), key, rid);
            }
        } catch (Exception e) {
            System.err.println("Error during insert: " + e.getMessage());
        }
    }

    private void split(Node parent, int index, Node node) {
        Node new_node = new Node(node.leaf);
        parent.values.add(index + 1, new_node);
        parent.keys.add(index, node.keys.get(this.degree - 1));

        new_node.keys.addAll(node.keys.subList(this.degree, node.keys.size()));
        node.keys.subList(this.degree - 1, node.keys.size()).clear();

        if (node.leaf) {
            // 对于叶子节点，需要处理RID的分割
            new_node.rids.addAll(node.rids.subList(this.degree, node.rids.size()));
            node.rids.subList(this.degree, node.rids.size()).clear();
            
            // 设置叶子节点的next指针
            new_node.next = node.next;
            node.next = new_node;
        } else {
            new_node.values.addAll(node.values.subList(this.degree, node.values.size()));
            node.values.subList(this.degree, node.values.size()).clear();
        }
    }

    private void stealFromLeft(Node parent, int i) {
        Node node = parent.values.get(i);
        Node leftSibling = parent.values.get(i - 1);
        node.keys.add(0, parent.keys.get(i - 1));
        parent.keys.set(i - 1, leftSibling.keys.remove(leftSibling.keys.size() - 1));
        
        if (node.leaf) {
            // 对于叶子节点，同时移动RID
            node.rids.add(0, leftSibling.rids.remove(leftSibling.rids.size() - 1));
        } else {
            node.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
        }
    }

    private void stealFromRight(Node parent, int i) {
        Node node = parent.values.get(i);
        Node rightSibling = parent.values.get(i + 1);
        node.keys.add(parent.keys.get(i));
        parent.keys.set(i, rightSibling.keys.remove(0));
        
        if (node.leaf) {
            // 对于叶子节点，同时移动RID
            node.rids.add(rightSibling.rids.remove(0));
        } else {
            node.values.add(rightSibling.values.remove(0));
        }
    }

    public void delete(Value key) {
        Node currentNode = this.root;
        boolean found = false;
        int i = 0;
        while (i < currentNode.keys.size()) {
            if (key.equals(currentNode.keys.get(i))) {
                found = true;
                break;
            } else if (key.compareTo(currentNode.keys.get(i)) < 0) {
                break;
            }
            i += 1;
        }
        if (found) {
            if (currentNode.leaf) {
                currentNode.keys.remove(i);
                currentNode.rids.remove(i);  // 同时删除对应的RID
            } else {
                Node pred = currentNode.values.get(i);
                if (pred.keys.size() >= this.degree) {
                    Value predKey = this.getMaxKey(pred);
                    currentNode.keys.set(i, predKey);
                    this.deleteFromLeaf(predKey, pred);
                } else {
                    Node succ = currentNode.values.get(i + 1);
                    if (succ.keys.size() >= this.degree) {
                        Value succKey = this.getMinKey(succ);
                        currentNode.keys.set(i, succKey);
                        this.deleteFromLeaf(succKey, succ);
                    } else {
                        this.merge(currentNode, i, pred, succ);
                        this.deleteFromLeaf(key, pred);
                    }
                }

                if (currentNode == this.root && currentNode.keys.size() == 0) {
                    this.root = currentNode.values.get(0);
                }
            }
        } else {
            if (currentNode.leaf) {
                return;
            } else {
                if (currentNode.values.get(i).keys.size() < this.degree) {
                    if (i != 0 && currentNode.values.get(i - 1).keys.size() >= this.degree) {
                        this.stealFromLeft(currentNode, i);
                    } else if (i != currentNode.keys.size() && currentNode.values.get(i + 1).keys.size() >= this.degree) {
                        this.stealFromRight(currentNode, i);
                    } else {
                        if (i == currentNode.keys.size()) {
                            i -= 1;
                        }
                        this.merge(currentNode, i, currentNode.values.get(i), currentNode.values.get(i + 1));
                    }
                }

                this.delete(key);
            }
        }
    }

    private void deleteFromLeaf(Value key, Node leaf) {
        int keyIndex = -1;
        for (int i = 0; i < leaf.keys.size(); i++) {
            if (leaf.keys.get(i).equals(key)) {
                keyIndex = i;
                break;
            }
        }
        
        if (keyIndex != -1) {
            leaf.keys.remove(keyIndex);
            leaf.rids.remove(keyIndex);  // 同时删除对应的RID
        }

        if (leaf == this.root || leaf.keys.size() >= Math.floor(this.degree / 2)) {
            return;
        }

        Node parent = this.findParent(leaf);
        if (parent == null) return;
        
        int i = parent.values.indexOf(leaf);

        if (i > 0 && parent.values.get(i - 1).keys.size() > Math.floor(this.degree / 2)) {
            this.rotateRight(parent, i);
        } else if (i < parent.keys.size() && parent.values.get(i + 1).keys.size() > Math.floor(this.degree / 2)) {
            this.rotateLeft(parent, i);
        } else {
            if (i == parent.keys.size()) {
                i -= 1;
            }
            this.merge(parent, i, parent.values.get(i), parent.values.get(i + 1));
        }
    }

    private Value getMinKey(Node node) {
        while (!node.leaf) {
            node = node.values.get(0);
        }
        return node.keys.get(0);
    }

    private Value getMaxKey(Node node) {
        while (!node.leaf) {
            node = node.values.get(node.values.size() - 1);
        }
        return node.keys.get(node.keys.size() - 1);
    }

    private Node findParent(Node child) {
        if (child == this.root) return null;
        
        return findParentHelper(this.root, child);
    }
    
    private Node findParentHelper(Node current, Node child) {
        if (current.leaf) return null;
        
        for (int i = 0; i < current.values.size(); i++) {
            if (current.values.get(i) == child) {
                return current;
            }
            Node result = findParentHelper(current.values.get(i), child);
            if (result != null) return result;
        }
        return null;
    }

    private void merge(Node parent, int i, Node pred, Node succ) {
        pred.keys.addAll(succ.keys);
        
        if (pred.leaf) {
            // 对于叶子节点，合并RID并更新next指针
            pred.rids.addAll(succ.rids);
            pred.next = succ.next;  // 更新next指针
        } else {
            pred.values.addAll(succ.values);
        }
        
        parent.values.remove(i + 1);
        parent.keys.remove(i);

        if (parent == this.root && parent.keys.size() == 0) {
            this.root = pred;
        }
    }

    private void rotateRight(Node parent, int i) {
        Node node = parent.values.get(i);
        Node prev = parent.values.get(i - 1);
        node.keys.add(0, parent.keys.get(i - 1));
        parent.keys.set(i - 1, prev.keys.remove(prev.keys.size() - 1));
        
        if (node.leaf) {
            // 对于叶子节点，同时移动RID
            node.rids.add(0, prev.rids.remove(prev.rids.size() - 1));
        } else {
            node.values.add(0, prev.values.remove(prev.values.size() - 1));
        }
    }

    private void rotateLeft(Node parent, int i) {
        Node node = parent.values.get(i);
        Node next = parent.values.get(i + 1);
        node.keys.add(parent.keys.get(i));
        parent.keys.set(i, next.keys.remove(0));
        
        if (node.leaf) {
            // 对于叶子节点，同时移动RID
            node.rids.add(next.rids.remove(0));
        } else {
            node.values.add(next.values.remove(0));
        }
    }

    // 新增：范围查询方法，利用叶子节点的next指针
    public List<RID> rangeSearch(Value startKey, Value endKey) {
        List<RID> result = new ArrayList<>();
        
        // 找到起始叶子节点
        Node currentNode = this.root;
        while (!currentNode.leaf) {
            int i = 0;
            while (i < currentNode.keys.size()) {
                if (startKey.compareTo(currentNode.keys.get(i)) < 0) {
                    break;
                }
                i += 1;
            }
            currentNode = currentNode.values.get(i);
        }
        
        // 遍历叶子节点链表
        while (currentNode != null) {
            for (int i = 0; i < currentNode.keys.size(); i++) {
                Value key = currentNode.keys.get(i);
                if (key.compareTo(startKey) >= 0 && key.compareTo(endKey) <= 0) {
                    result.add(currentNode.rids.get(i));
                } else if (key.compareTo(endKey) > 0) {
                    return result;  // 超出范围，提前返回
                }
            }
            currentNode = currentNode.next;  // 使用next指针移动到下一个叶子节点
        }
        
        return result;
    }

    // 新增：详细调试方法
    public void debugPrint() {
        System.out.println("=== B+ Tree Debug Info ===");
        debugPrintHelper(this.root, 0);
        System.out.println("=== End Debug Info ===");
    }
    
    private void debugPrintHelper(Node node, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "Node (leaf=" + node.leaf + "):");
        System.out.println(indent + "  Keys: " + node.keys);
        if (node.leaf) {
            System.out.println(indent + "  RIDs: " + node.rids);
            System.out.println(indent + "  Next: " + (node.next != null ? "exists" : "null"));
        } else {
            System.out.println(indent + "  Children count: " + node.values.size());
            for (int i = 0; i < node.values.size(); i++) {
                System.out.println(indent + "    Child " + i + ":");
                debugPrintHelper(node.values.get(i), depth + 2);
            }
        }
    }

    public void printTree() {
        List<Node> currentNodeLevel = new ArrayList<>();
        currentNodeLevel.add(this.root);
        int level = 0;

        while (!currentNodeLevel.isEmpty()) {
            List<Node> nextLevel = new ArrayList<>();
            System.out.print("Level " + level + ": ");

            for (Node node : currentNodeLevel) {
                if (node.leaf) {
                    // 叶子节点：显示所有key-RID对
                    System.out.print("[");
                    for (int i = 0; i < node.keys.size(); i++) {
                        System.out.print(node.keys.get(i) + ":" + node.rids.get(i));
                        if (i < node.keys.size() - 1) {
                            System.out.print(", ");
                        }
                    }
                    System.out.print("] ");
                } else {
                    // 内部节点：只显示keys
                    System.out.print("[" + String.join(", ", node.keys.stream().map(String::valueOf).toArray(String[]::new)) + "] ");
                    nextLevel.addAll(node.values);
                }
            }

            System.out.println();
            currentNodeLevel = nextLevel;
            level++;
        }
    }

    // 新增：打印叶子节点链表
    public void printLeafChain() {
        // 找到最左边的叶子节点
        Node current = this.root;
        while (!current.leaf) {
            current = current.values.get(0);
        }
        
        System.out.print("Leaf chain: ");
        while (current != null) {
            System.out.print("[" + String.join(", ", current.keys.stream().map(String::valueOf).toArray(String[]::new)) + "]");
            if (current.next != null) {
                System.out.print(" -> ");
            }
            current = current.next;
        }
        System.out.println();
    }

    public static void main(String[] args) {
        // create a B+ tree with degree 3
        BPlusTree tree = new BPlusTree(3);

        // insert some keys with RIDs
        System.out.println("Inserting keys 1-9...");
        tree.insert(new Value(1L), new RID(1, 1));
        tree.insert(new Value(2L), new RID(1, 2));
        tree.insert(new Value(3L), new RID(1, 3));
        tree.insert(new Value(4L), new RID(2, 1));
        tree.insert(new Value(5L), new RID(2, 2));
        tree.insert(new Value(6L), new RID(2, 3));
        tree.insert(new Value(7L), new RID(3, 1));
        tree.insert(new Value(8L), new RID(3, 2));
        tree.insert(new Value(9L), new RID(3, 3));
        
        // System.out.println("\nTree structure:");
        // tree.printTree();
        
        System.out.println("\nDetailed debug info:");
        tree.debugPrint();
        
        // System.out.println("\nLeaf node chain:");
        // tree.printLeafChain();
        
        // test search
        // System.out.println("\nSearch tests:");
        // System.out.println("Search for key 5: " + tree.search(new Value(5L)));
        // System.out.println("Search for key 1: " + tree.search(new Value(1L)));
        // System.out.println("Search for key 9: " + tree.search(new Value(9L)));
        
        // test range search
        // System.out.println("\nRange search (3 to 7): " + tree.rangeSearch(new Value(3L), new Value(7L)));
        
        // delete a key
        // System.out.println("\nDeleting key 3...");
        tree.delete(new Value(3L));
        // System.out.println("After deleting key 3:");
        tree.printTree();
        // tree.printLeafChain();
    }
}