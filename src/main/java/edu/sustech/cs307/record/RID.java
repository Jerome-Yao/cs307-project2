package edu.sustech.cs307.record;

public class RID {
    public int pageNum;
    public int slotNum;

    public RID(int page_no, int slot_no) {
        this.pageNum = page_no;
        this.slotNum = slot_no;
    }

    public RID(RID rid) {
        this.pageNum = rid.pageNum;
        this.slotNum = rid.slotNum;
    }
    
    public RID() {
        this.pageNum = 0;
        this.slotNum = 0;
    }

    public int getPageNum() {
        return pageNum;
    }
    public int getSlotNum() {
        return slotNum;
    }

    public String toString() {
        return "RID{" +
                "pageNum=" + pageNum +
                ", slotNum=" + slotNum +
                '}';
    }
}
