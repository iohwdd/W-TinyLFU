package com.iohw.wtinylfu;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: iohw
 * @date: 2024/10/25 21:28
 * @description: LRU缓存
 */
@Slf4j
class WindowCache {
    private Map<String, Node> cache ;
    private Node head;
    private Node tail;
    private int capacity;
    private int size;
    private ObtainMeasure obtainMeasure;
    private CountMinSketch countMinSketch;

    public WindowCache(int capacity, ObtainMeasure obtainMeasure) {
        this.capacity = capacity;
        this.obtainMeasure = obtainMeasure;
        cache = new HashMap<String, Node>(capacity);
        head = new Node();
        tail = new Node();
        head.next = tail;
        tail.pre = head;
        size = 0;
    }
    public WindowCache(int capacity,CountMinSketch countMinSketch) {
        this.capacity = capacity;
        this.cache = new HashMap<String, Node>(capacity);
        initLinkedList();
        this.countMinSketch = countMinSketch;
    }
    private void initLinkedList() {
        head = new Node();
        tail = new Node();
        head.next = tail;
        tail.pre = head;
        size = 0;
    }

    /**
     * @param key
     * @param value
     * @return put成功返回新加入的node，失败返回淘汰的candidate
     */
    public Node put(String key, Object value) {
        Node node = cache.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
            // 更新频率
            countMinSketch.set(key.getBytes());
            return node;
        }
        if(size == capacity) {
            Node lastNode = tail.pre;
            deleteNode(lastNode);
            cache.remove(lastNode.key);
            size --;
            return lastNode;
        }
        Node newNode = new Node(key,value);
        addNode(newNode);
        cache.put(key, newNode);
        size ++;

        // 更新频率
        countMinSketch.set(key.getBytes());
        return newNode;
    }
    public Object get(String key) {
        Node entry = cache.get(key);
        if(entry == null) {
            if(obtainMeasure != null) {
                log.debug("缓存中没有key为【{}】的数据，采取补救措施...",key);
                // 补救措施
                Object value = obtainMeasure.obtain(key);
                if(value == null) {
                    return null;
                }
                // 补救成功
                log.debug("补救成功,有新的数据加入缓存：【{} -> {}】",key,value);
                put(key, value);
                return value;
            }
            return null;
        }
        moveToHead(entry);

        // 更新频率
        countMinSketch.set(key.getBytes());
        return entry.value;
    }
    public boolean isFull() {
        return size == capacity;
    }
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }
    private void addNode(Node node) {
        node.pre = head;
        node.next = head.next;
        head.next.pre = node;
        head.next = node;
    }
    private void deleteNode(Node node) {
        node.pre.next = node.next;
        node.next.pre = node.pre;
    }
    private void moveToHead(Node node) {
        deleteNode(node);
        addNode(node);
    }
}
