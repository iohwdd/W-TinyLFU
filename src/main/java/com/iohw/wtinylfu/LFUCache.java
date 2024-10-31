package com.iohw.wtinylfu;


import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: iohw
 * @date: 2024/10/24 21:52
 * @description:
 */
@Data
class LFUCache {
    private Map<String, Node> key_table;
    private int capacity;
    private CountMinSketch countMinSketch;

    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.countMinSketch = new CountMinSketch();
        key_table = new HashMap<>();
    }

    public Node put(String key, Object value) {
        if(key_table.size() == capacity && !key_table.containsKey(key)) {
            // 找出频率最低的victim
            String minFreqKey = findMinFreqKey();
            Node victim = key_table.remove(minFreqKey);
            // 传入的key为candidate，与victim进行pk
            int victimFreq = countMinSketch.getEstimatedCount(victim.getKey().getBytes());
            int candidateFreq = countMinSketch.getEstimatedCount(key.getBytes());
            if(victimFreq < candidateFreq) {
                // candidate上位，victim淘汰
                key_table.remove(victim.getKey());
                key_table.put(key,new Node(key,value));
                countMinSketch.set(key.getBytes());
            }
            return victim;
        }
        Node newNode = new Node(key, value);
        key_table.put(key, newNode);
        countMinSketch.set(key.getBytes());
        return newNode;
    }
    public Object get(String key) {
        if(capacity == 0 || !key_table.containsKey(key)) {
            return null;
        }
        // 拿到元素
        Node node = key_table.get(key);
        // 更新频率
        countMinSketch.set(key.getBytes());
        return node.getValue();
    }
    public void remove(String key) {
        key_table.remove(key);
    }

    /**
     *
     * @param freq candidate的频率
     * @return candidate胜利返回true
     */
    public boolean pk(int freq) {
        String victim = findMinFreqKey();
        return freq > countMinSketch.getEstimatedCount(victim.getBytes());
    }
    public boolean containsKey(String key) {
        return key_table.containsKey(key);
    }
    public boolean isFull() {
        return key_table.size() == capacity;
    }
    private String findMinFreqKey() {
        int minFreq = Integer.MAX_VALUE;
        String minFreqKey = null;
        for (String key : key_table.keySet()) {
            int estimatedCount = countMinSketch.getEstimatedCount(key.getBytes());
            if(minFreq > estimatedCount) {
                minFreq = estimatedCount;
                minFreqKey = key;
            }
        }
        return minFreqKey;
    }
}
