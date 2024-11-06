package com.iohw.wtinylfu;


/**
 * @author: iohw
 * @date: 2024/10/25 21:53
 * @description:
 */
class TinyLFU {
    private LFUCache probationCache;
    private LFUCache protectedCache;
    private int capacity;
    private CountMinSketch windowCms;
    private final int PROMOTION_FREQUENCY = 2;

    public TinyLFU(int capacity,CountMinSketch windowCms) {
        this.capacity = capacity;
        this.windowCms = windowCms;
        probationCache = new LFUCache((int)(capacity * 0.2));
        protectedCache = new LFUCache((int)(capacity * 0.8));
    }

    public Object get(String key) {
        // 1.依次尝试从probationCache与protectedCache中获取
        Object value = probationCache.get(key);
        if (value != null) {
            // 2. 判断是否能够晋升
            int freq = probationCache.getCountMinSketch().getEstimatedCount(key.getBytes());
            if(freq >= PROMOTION_FREQUENCY) {
                promotion(new Node(key,value));
            }
        } else {
            value = protectedCache.get(key);
        }
        return value;
    }

    /**
     * @param candidate 从WindowCache淘汰的candidate
     */
    public void put(Node candidate) {
        // 1. 检查probationCache是否已满，未满直接放入probationCache并返回
        // 2. 已满，就将WindowCache淘汰的candidate与probationCache中使用频率最低的victim进行pk。胜利留在probationCache，失败的淘汰。
        String key = candidate.getKey();
        Object value = candidate.getValue();
        int candidateFreq = windowCms.getEstimatedCount(key.getBytes());
        CountMinSketch probationCms = probationCache.getCountMinSketch();

        if (probationCache.isFull() && !probationCache.containsKey(key)) {
            // probationCache已满，进行pk
            if (probationCache.pk(candidateFreq)) {
                // candidate胜利才加入probationCache,否则淘汰
                probationCache.put(key, value);
            }
        } else {
            probationCache.put(key, value);
        }

        // 更新频率并检查晋升条件
        int freq = probationCms.getEstimatedCount(candidate.getKey().getBytes());
        // 3. node频率达到一定次数，从probationCache晋升至protectedCache
        if (freq >= PROMOTION_FREQUENCY) {
            promotion(new Node(key, value));
        }
        probationCms.setFrequency(key.getBytes(), candidateFreq);
    }
    public void put(String key, Object value) {
        put(new Node(key,value));
    }
    private boolean promotion(Node node) {
        CountMinSketch probationCms = probationCache.getCountMinSketch();
        boolean isSuccess = !protectedCache.isFull() || protectedCache.pk(probationCms.getEstimatedCount(node.getKey().getBytes()));
        String key = node.getKey();
        Object value = node.getValue();
        if(isSuccess) {
            probationCache.remove(node.getKey());
            protectedCache.put(key, value);

            int freq = probationCache.getCountMinSketch().getEstimatedCount(key.getBytes());
            protectedCache.getCountMinSketch().setFrequency(key.getBytes(),freq);
        }
        return isSuccess;
    }
    public void invalidate(String key) {
        if(probationCache.containsKey(key)) {
            probationCache.remove(key);
        }
        if(protectedCache.containsKey(key)) {
            protectedCache.remove(key);
        }
    }
    public boolean isProtected(String key) {
        return protectedCache.containsKey(key);
    }
    public boolean isProbation(String key) {
        return probationCache.containsKey(key);
    }
    public boolean isFull() {
        return probationCache.isFull() && protectedCache.isFull();
    }
    public boolean containsKey(String key) {
        return probationCache.containsKey(key) || protectedCache.containsKey(key);
    }
}
