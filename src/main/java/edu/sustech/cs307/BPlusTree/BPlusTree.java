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
        Node curr = this.root;
        while (!curr.leaf) {
            int i = 0;
            while (i < curr.keys.size()) {
                if (key.getValue() < curr.keys.get(i)) {
                    break;
                }
                i += 1;
            }
            curr = curr.values.get(i);
        }
        int i = 0;
        while (i < curr.keys.size()) {
            if (curr.keys.get(i) == key) {
                return true;
            }
            i += 1;
        }
        return false;
    }

    public void insert(Value key) {
        Node curr = this.root;
        if (curr.keys.size() == 2 * this.degree) {
            Node newRoot = new Node(false);
            this.root = newRoot;
            newRoot.values.add(curr);
            this.split(newRoot, 0, curr);
            this.insertNonFull(newRoot, key);
        } else {
            this.insertNonFull(curr, key);
        }
    }

    private void insertNonFull(Node curr, Value key) {
        int i = 0;
        while (i < curr.keys.size()) {
            if (key < curr.keys.get(i)) {
                break;
            }
            i += 1;
        }
        if (curr.leaf) {
            curr.keys.add(i, key);
        } else {
            if (curr.values.get(i).keys.size() == 2 * this.degree) {
                this.split(curr, i, curr.values.get(i));
                if (key > curr.keys.get(i)) {
                    i += 1;
                }
            }
            this.insertNonFull(curr.values.get(i), key);
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
        Node curr = this.root;
        boolean found = false;
        int i = 0;
        while (i < curr.keys.size()) {
            if (key == curr.keys.get(i)) {
                found = true;
                break;
            } else if (key < curr.keys.get(i)) {
                break;
            }
            i += 1;
        }
        if (found) {
            if (curr.leaf) {
                curr.keys.remove(i);
            } else {
                Node pred = curr.values.get(i);
                if (pred.keys.size() >= this.degree) {
                    int predKey = this.getMaxKey(pred);
                    curr.keys.set(i, predKey);
                    this.deleteFromLeaf(predKey, pred);
                } else {
                    Node succ = curr.values.get(i + 1);
                    if (succ.keys.size() >= this.degree) {
                        int succKey = this.getMinKey(succ);
                        curr.keys.set(i, succKey);
                        this.deleteFromLeaf(succKey, succ);
                    } else {
                        this.merge(curr, i, pred, succ);
                        this.deleteFromLeaf(key, pred);
                    }
                }

                if (curr == this.root && curr.keys.size() == 0) {
                    this.root = curr.values.get(0);
                }
            }
        } else {
            if (curr.leaf) {
                return;
            } else {
                if (curr.values.get(i).keys.size() < this.degree) {
                    if (i != 0 && curr.values.get(i - 1).keys.size() >= this.degree) {
                        this.stealFromLeft(curr, i);
                    } else if (i != curr.keys.size() && curr.values.get(i + 1).keys.size() >= this.degree) {
                        this.stealFromRight(curr, i);
                    } else {
                        if (i == curr.keys.size()) {
                            i -= 1;
                        }
                        this.merge(curr, i, curr.values.get(i), curr.values.get(i + 1));
                    }
                }

                this.delete(key);
            }
        }
    }

    private void deleteFromLeaf(Value key, Node leaf) {
        leaf.keys.remove(Integer.valueOf(key));

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

    private int getMinKey(Node node) {
        while (!node.leaf) {
            node = node.values.get(0);
        }
        return node.keys.get(0);
    }

    private int getMaxKey(Node node) {
        while (!node.leaf) {
            node = node.values.get(node.values.size() - 1);
        }
        return node.keys.get(node.keys.size() - 1);
    }

    private Node findParent(Node child) {
        Node curr = this.root;
        while (!curr.leaf) {
            int i = 0;
            while (i < curr.values.size()) {
                if (child == curr.values.get(i)) {
                    return curr;
                } else if (child.keys.get(0) < curr.values.get(i).keys.get(0)) {
                    break;
                }
                i += 1;
            }
            curr = curr.values.get(i);
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
        List<Node> currLevel = new ArrayList<>();
        currLevel.add(this.root);

        while (!currLevel.isEmpty()) {
            List<Node> nextLevel = new ArrayList<>();

            for (Node node : currLevel) {
                System.out.print("[" + String.join(", ", node.keys.stream().map(String::valueOf).toArray(String[]::new)) + "] ");

                if (!node.leaf) {
                    nextLevel.addAll(node.values);
                }
            }

            System.out.println();
            currLevel = nextLevel;
        }
    }

    public static void main(String[] args) {
        // create a B+ tree with degree 3
        BPlusTree tree = new BPlusTree(3);

        // insert some keys
        tree.insert(1);
        tree.insert(2);
        tree.insert(3);
        tree.insert(4);
        tree.insert(5);
        tree.insert(6);
        tree.insert(7);
        tree.insert(8);
        tree.insert(9);

        // print the tree
        tree.printTree(); // [4] [2, 3] [6, 7, 8, 9] [1] [5]

        // delete a key
        tree.delete(3);

        // print the tree
        tree.printTree(); // [4] [2] [6, 7, 8, 9] [1] [5]
    }
}