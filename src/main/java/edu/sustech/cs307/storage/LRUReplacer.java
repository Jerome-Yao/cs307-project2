package edu.sustech.cs307.storage;

import java.util.*;

public class LRUReplacer {

    private final int maxSize;
    private final Set<Integer> pinnedFrames = new HashSet<>();
    private final Set<Integer> LRUHash = new HashSet<>();
    private final LinkedList<Integer> LRUList = new LinkedList<>();

    public LRUReplacer(int numPages) {
        this.maxSize = numPages;
    }

    public int Victim() {
        if (LRUList.isEmpty()) {
            return -1;
        }
        int victim = LRUList.remove();
        LRUHash.remove(victim);
        return victim;
    }

    public void Pin(int frameId) {
        if (pinnedFrames.contains(frameId)) {
            return;
        }
        if (this.size()>= maxSize) {
            throw new RuntimeException("REPLACER IS FULL");
        }
        pinnedFrames.add(frameId);
        if (LRUHash.contains(frameId)) {
            LRUHash.remove(frameId);
        }
        if (LRUList.contains(frameId)) {
            LRUList.remove(LRUList.indexOf(frameId));
        }
    }


    public void Unpin(int frameId) {
        if (LRUHash.contains(frameId)) {
            return;
        }
        if (pinnedFrames.contains(frameId)) {
            pinnedFrames.remove(frameId);
            LRUHash.add(frameId);
            LRUList.add(frameId);
        }else {
            throw new RuntimeException("UNPIN PAGE NOT FOUND");
        }
    }


    public int size() {
        return LRUList.size() + pinnedFrames.size();
    }
}