package org.example.huffman_coding.Hashing;

import java.util.AbstractMap;

public interface Hash {
    long preHash(String s);
    int size();
    boolean insert(long key);
    boolean delete(long key);
    AbstractMap.SimpleEntry<Boolean, Integer> search(long key);
}
