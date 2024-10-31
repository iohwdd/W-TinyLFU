package com.iohw.wtinylfu;



/**
 * @author: iohw
 * @date: 2024/10/25 21:26
 * @description:
 */
public class WTinyLFU {
    private WindowCache windowCache;
    private TinyLFU tinyLFU;
    private int capacity;

    public WTinyLFU(int capacity) {
        this.capacity = capacity;
        CountMinSketch cms = new CountMinSketch();
        windowCache = new WindowCache((int)(capacity * 0.01),cms);
        tinyLFU = new TinyLFU((int)(capacity * 0.99),cms);
    }

    public Object get(String key) {
        // 依次从windowCache，tinyLFU中获取
        return windowCache.get(key) != null ? windowCache.get(key) : tinyLFU.get(key);
    }

    public void put(String key, Object value) {
        if(tinyLFU.containsKey(key)) {
            //存在缓存 -> 刷新缓存
            tinyLFU.put(key,value);
            return;
        }
        // windowCache已满且不存在旧缓存，从windowCache淘汰一位candidate，尝试向tinyLFU缓存put这个candidate
        if(windowCache.isFull() && !windowCache.containsKey(key)) {
            Node candidate = windowCache.put(key,value);
            tinyLFU.put(candidate);
        }
        windowCache.put(key, value);
    }
    public boolean isProtected(String key) {
        return tinyLFU.isProtected(key);
    }
    public boolean isWindow(String key) {
        return windowCache.containsKey(key);
    }
    public boolean isProbation(String key) {
        return tinyLFU.isProbation(key);
    }
}
