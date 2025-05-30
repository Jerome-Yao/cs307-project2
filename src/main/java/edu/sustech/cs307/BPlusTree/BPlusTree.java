package edu.sustech.cs307.BPlusTree;
import java.util.ArrayList;
import java.util.List;
import edu.sustech.cs307.value.Value;

class Node {
    List<Value> keys;
    List<Node> values;
    boolean leaf;
    Node next;

    public Node(boolean leaf) {
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
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

    public boolean search(Value key) {
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
                if (currentNode.keys.get(i).equals(key) ) {
                    return true;
                }
                i += 1;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
            return false; // If any exception occurs, we assume the key is not found
        }
    }

    public void insert(Value key) {
        Node currentNode = this.root;
        if (currentNode.keys.size() == 2 * this.degree) {
            Node newRoot = new Node(false);
            this.root = newRoot;
            newRoot.values.add(currentNode);
            this.split(newRoot, 0, currentNode);
            this.insertNonFull(newRoot, key);
        } else {
            this.insertNonFull(currentNode, key);
        }
    }

    private void insertNonFull(Node currentNode, Value key) {
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
            } else {
                if (currentNode.values.get(i).keys.size() == 2 * this.degree) {
                    this.split(currentNode, i, currentNode.values.get(i));
                    if (key.compareTo(currentNode.keys.get(i)) > 0) {
                        i += 1;
                    }
                }
                this.insertNonFull(currentNode.values.get(i), key);
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

        if (!node.leaf) {
            new_node.values.addAll(node.values.subList(this.degree, node.values.size()));
            node.values.subList(this.degree, node.values.size()).clear();
        }
    }


    private void stealFromLeft(Node parent, int i) {
        Node node = parent.values.get(i);
        Node leftSibling = parent.values.get(i - 1);
        node.keys.add(0, parent.keys.get(i - 1));
        parent.keys.set(i - 1, leftSibling.keys.remove(leftSibling.keys.size() - 1));
        if (!node.leaf) {
            node.values.add(0, leftSibling.values.remove(leftSibling.values.size() - 1));
        }
    }

    private void stealFromRight(Node parent, int i) {
        Node node = parent.values.get(i);
        Node rightSibling = parent.values.get(i + 1);
        node.keys.add(parent.keys.get(i));
        parent.keys.set(i, rightSibling.keys.remove(0));
        if (!node.leaf) {
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
        if (!key.isValid()) {
            System.out.println("Deleting key: " + key.getType() + key.getValue());
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        leaf.keys.remove(key);

        if (leaf == this.root || leaf.keys.size() >= Math.floor(this.degree / 2)) {
            return;
        }

        Node parent = this.findParent(leaf);
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
        Node currentNode = this.root;
        while (!currentNode.leaf) {
            int i = 0;
            while (i < currentNode.values.size()) {
                if (child == currentNode.values.get(i)) {
                    return currentNode;
                } else if (child.keys.get(0).compareTo(currentNode.values.get(i).keys.get(0)) < 0) {
                    break;
                }
                i += 1;
            }
            currentNode = currentNode.values.get(i);
        }
        return null;
    }

    private void merge(Node parent, int i, Node pred, Node succ) {
        pred.keys.addAll(succ.keys);
        pred.values.addAll(succ.values);
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
        if (!node.leaf) {
            node.values.add(0, prev.values.remove(prev.values.size() - 1));
        }
    }

    private void rotateLeft(Node parent, int i) {
        Node node = parent.values.get(i);
        Node next = parent.values.get(i + 1);
        node.keys.add(parent.keys.get(i));
        parent.keys.set(i, next.keys.remove(0));
        if (!node.leaf) {
            node.values.add(next.values.remove(0));
        }
    }

    public void printTree() {
        List<Node> currentNodeLevel = new ArrayList<>();
        currentNodeLevel.add(this.root);

        while (!currentNodeLevel.isEmpty()) {
            List<Node> nextLevel = new ArrayList<>();

            for (Node node : currentNodeLevel) {
                System.out.print("[" + String.join(", ", node.keys.stream().map(String::valueOf).toArray(String[]::new)) + "] ");

                if (!node.leaf) {
                    nextLevel.addAll(node.values);
                }
            }

            System.out.println();
            currentNodeLevel = nextLevel;
        }
    }

    public static void main(String[] args) {
        // create a B+ tree with degree 3
        BPlusTree tree = new BPlusTree(3);

        // insert some keys
        tree.insert(new Value(1L));  // 使用 Value 的构造函数
        tree.insert(new Value(2L));
        tree.insert(new Value(3L));
        tree.insert(new Value(4L));
        tree.insert(new Value(5L));
        tree.insert(new Value(6L));
        tree.insert(new Value(7L));
        tree.insert(new Value(8L));
        tree.insert(new Value(9L));
        tree.printTree();
        // delete a key
        tree.delete(new Value(3L)); // 删除键 3

        // print the tree
        tree.printTree(); // [4] [2] [6, 7, 8, 9] [1] [5]
    }
}