package com.iohw.wtinylfu;

/**
 * @author: iohw
 * @date: 2024/10/24 20:54
 * @description: 补救措施：缓存中没有时可通过此接口获取数据，然后再存入缓存
 */
public interface ObtainMeasure {
    Object obtain(String key);
}