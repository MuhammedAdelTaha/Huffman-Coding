package org.example.huffman_coding.Hashing;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Random;

public class NSquareSpaceHash implements Hash{
    //The number of inserted keys
    private int n;
    //The size of the hash table
    private final int m;
    //The hash table
    private final ArrayList<Long> hashTable = new ArrayList<>();
    //Array a list of hash functions used in insertions (storing them for search purpose)
    private final ArrayList<byte[][]> hashFunctions = new ArrayList<>();
    public NSquareSpaceHash(int size){
        //MAX power of 2
        final int MAXVALUE = 100_000_000;
        //Math.sqrt(MAXVALUE) = 10000
        if(size >= 10000)
            m = MAXVALUE;
        else
            m = size * size;
        setHashTable();
    }

    /**
     * Set the initial values of the hash table to be null
     * */
    private void setHashTable(){
        hashTable.clear();
        for (int i = 0; i < m; i++) {
            hashTable.add(i, null);
        }
    }

    /**
     * Return the hash table size
     * */
    public int size(){
        return n;
    }

    /**
     * takes a string and convert it to an integer
     * */
    public long preHash(String s){
        long code = 0, p = 1;
        for (int i = s.length() - 1; i >= 0; i--){
            code += s.charAt(i) * p;
            p *= 128;
        }
        return code;
    }

    /**
     * Log to the base 2
     * */
    private int lg(long num){
        return Long.SIZE - Long.numberOfLeadingZeros(num) - 1;
    }

    /**
     * Takes a randomly selected hash 2D 0/1 matrix h and the key to be inserted
     * Return an integer represents the index of the input key in the hash table using this h matrix
     * */
    private int hash(byte[][] h, long key){
        String binaryString = Long.toBinaryString(key);
        // number of bits in the given key
        int u = lg(key) + 1;
        // m = 2 ^ b, b is the number of rows in h
        int b = lg(m);
        byte[] x = new byte[u];

        // fill the actual bit representation of the given key
        for (int i = 0; i < u; i++) {
            x[i] = (byte) (binaryString.charAt(u - i - 1) - '0');
        }

        // calculate the hash index and return it
        int hx = 0;
        for (int i = 0; i < b; i++) {
            int bit = 0;
            for (int j = 0; j < u; j++) {
                bit += h[i][j] * x[j];
            }
            bit %= 2;
            hx += bit * (1 << i);
        }
        return hx;
    }

    /**
     * Takes a key and gets its appropriate hash function and index that gets a hash index that does not cause collision
     * */
    private AbstractMap.SimpleEntry<byte[][], Integer> hashFunction(long key){
        int u = lg(key) + 1;
        int b = lg(m);
        byte[][] h = new byte[b][u];
        Random random = new Random();
        int hashIdx;
        do {
            for (int i = 0; i < b; i++) {
                for (int j = 0; j < u; j++) {
                    h[i][j] = (byte) random.nextInt(2);
                }
            }
            hashIdx = hash(h, key);
        } while (hashTable.get(hashIdx) != null);
        return new AbstractMap.SimpleEntry<>(h, hashIdx);
    }

    /**
     * Takes a key to be inserted and update tha hash table and tha array list of used hash functions accordingly
     * */
    private void updateTables(long key){
        int hashIdx;
        for (byte[][] hashFunction : hashFunctions) {
            if (hashFunction[0].length != lg(key) + 1)
                continue;
            hashIdx = hash(hashFunction, key);
            if (hashTable.get(hashIdx) == null) {
                hashTable.set(hashIdx, key);
                return;
            }
        }
        AbstractMap.SimpleEntry<byte[][], Integer> function = hashFunction(key);
        hashFunctions.add(function.getKey());
        hashTable.set(function.getValue(), key);
    }

    /**
     * takes a key and inserts it in the hash table
     * */
    public boolean insert(long key){
        if(this.search(key).getKey()) return false;
        n++;
        updateTables(key);
        return true;
    }

    /**
     * takes a key and deletes it from the hash table
     * */
    public boolean delete(long key){
        AbstractMap.SimpleEntry<Boolean, Integer> searcher = search(key);
        boolean searchFlag = searcher.getKey();
        if (!searchFlag)
            return false;

        int searchIdx = searcher.getValue();
        n--;
        hashTable.set(searchIdx, null);
        return true;
    }

    /**
     * Takes a key and return true if found and false if not
     * */
    public AbstractMap.SimpleEntry<Boolean, Integer> search(long key){
        for (byte[][] hashFunction : hashFunctions){
            if(hashFunction[0].length != lg(key) + 1) continue;
            int hashIdx = hash(hashFunction, key);
            if (hashTable.get(hashIdx) != null && hashTable.get(hashIdx) == key) {
                return new AbstractMap.SimpleEntry<>(true, hashIdx);
            }
        }
        return new AbstractMap.SimpleEntry<>(false, null);
    }
}