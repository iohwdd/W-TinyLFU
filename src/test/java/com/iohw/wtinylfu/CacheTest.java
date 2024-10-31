package com.iohw.wtinylfu;


import org.junit.jupiter.api.Test;

/**
 * @author: iohw
 * @date: 2024/10/30 19:34
 * @description:
 */
public class CacheTest {
    // 各分区容量
    // windowCache 1
    // probationCache 19
    // protectedCache 79
    // 以下测试基于该设定  访问频率>=2 晋升至protected
    @Test
    public void testPartition() {
        WTinyLFU cache = new WTinyLFU(100);
        // 20在window 1~19在probation
        for(int i =1 ;i <= 20; i++){
            cache.put(i+"",i);
        }
        for(int i =1 ;i <= 20; i++){
            System.out.println("cache.isProtected("+ i +")) = " + cache.isProtected(i+""));
            System.out.println("cache.isProbation("+ i +")) = " + cache.isProbation(i+""));
            System.out.println("cache.isWindow("+ i +")) = " + cache.isWindow(i+""));
            System.out.println();
        }
        System.out.println("=================================");
        for(int i =1 ;i <= 20; i++){
            cache.get(i+"");
        }
        // 20在window 1~19在protect
        for(int i =1 ;i <= 20; i++){
            System.out.println("cache.isProtected("+ i +")) = " + cache.isProtected(i+""));
            System.out.println("cache.isProbation("+ i +")) = " + cache.isProbation(i+""));
            System.out.println("cache.isWindow("+ i +")) = " + cache.isWindow(i+""));
            System.out.println();
        }
    }

    @Test
    public void testPartition2() {
        WTinyLFU cache = new WTinyLFU(100);
        // 20在window 1~19在protect
        for(int i =1 ;i <= 20; i++){
            cache.put(i+"",i);
        }
        for(int i =1 ;i <= 20; i++){
            cache.get(i+"");
        }
        // 此时probation还有19个位置,下面的数据全在probation
        for(int i = 21; i <= 39 ; i++){
            cache.put(i+"",i);
        }
        // 现在，window，probation都已满。新缓存进来，则会对probation中的victim进行pk来决定是否保留
        cache.put("40",40);
        for(int i =1 ;i <= 40; i++){
            System.out.println("cache.isProtected("+ i +")) = " + cache.isProtected(i+""));
            System.out.println("cache.isProbation("+ i +")) = " + cache.isProbation(i+""));
            System.out.println("cache.isWindow("+ i +")) = " + cache.isWindow(i+""));
            System.out.println();
        }
    }
}
