package com.iohw.wtinylfu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Node {
    String key;
    Object value;
    Node pre;
    Node next;

    Node(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}