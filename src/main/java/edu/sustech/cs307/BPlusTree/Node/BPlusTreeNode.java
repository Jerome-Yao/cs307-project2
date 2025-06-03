package edu.sustech.cs307.BPlusTree.Node;

import edu.sustech.cs307.value.Value;
import edu.sustech.cs307.record.RID;
import java.util.List;

public abstract class BPlusTreeNode {
    protected boolean isLeaf;
    protected int degree;
    protected BPlusTreeNode parent;
    protected List<Value> keys;
    
    public BPlusTreeNode(int degree, boolean isLeaf) {
        this.degree = degree;
        this.isLeaf = isLeaf;
        this.parent = null;
    }
    
    public abstract boolean isFull();
    public abstract void insert(Value key, RID rid);
    public abstract BPlusTreeNode split();
    public abstract Value getFirstKey();
    
    // Getters and setters
    public boolean isLeaf() { return isLeaf; }
    public List<Value> getKeys() { return keys; }
    public BPlusTreeNode getParent() { return parent; }
    public void setParent(BPlusTreeNode parent) { this.parent = parent; }
    public int getDegree() { return degree; }
}
