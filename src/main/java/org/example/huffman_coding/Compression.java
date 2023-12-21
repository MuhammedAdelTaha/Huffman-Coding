package org.example.huffman_coding;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Compression {
    public Compression() {}

    private static class Node {
        String key;
        int frequency;
        Node left;
        Node right;

        public Node(String key, int frequency) {
            this.key = key;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }
    }

    private String[] getStrings(byte[] data, int n) {
        int size = (data.length / n) + (data.length % n == 0 ? 0 : 1);
        String[] nBytes = new String[size];
        int i;
        for (i = 0; i < data.length - n + 1; i+= n) {
            StringBuilder key = new StringBuilder();
            for (int j = 0; j < n; j++) key.append(String.format("%02X ", data[i + j]));
            nBytes[i / n] = key.toString();
        }

        StringBuilder key = new StringBuilder();
        for (int j = 0; j < n && i + j < data.length; j++) key.append(String.format("%02X ", data[i + j]));
        if (!key.isEmpty()) nBytes[i / n] = key.toString();

        return nBytes;
    }

    private String[] readFileToNBytes(String filePath, int n) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        byte[] data = fileInputStream.readAllBytes();

        fileInputStream.close();
        return getStrings(data, n);
    }

    private Map<String, Integer> getFrequenciesForNBytes(String[] nBytes) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String nByte : nBytes) frequencies.put(nByte, frequencies.getOrDefault(nByte, 0) + 1);
        return frequencies;
    }

    private Node huffmanCoding(Map<String, Integer> frequencies) {
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.frequency));
        for (Map.Entry<String, Integer> entry : frequencies.entrySet())
            priorityQueue.add(new Node(entry.getKey(), entry.getValue()));

        if (priorityQueue.size() == 1) {
            Node peek = priorityQueue.poll();
            Node root = new Node(peek.key, peek.frequency);
            root.left = peek;
            return root;
        }

        while (priorityQueue.size() > 1) {
            Node left = priorityQueue.poll();
            Node right = priorityQueue.poll();
            assert right != null;
            Node parent = new Node(left.key + right.key, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            priorityQueue.add(parent);
        }

        return priorityQueue.poll();
    }

    /**
     * This function takes the root of the Huffman tree and returns a Map of each hex string and its codeword.
     * */
    private void getCodes(Node root, String code, Map<String, String> codes) {
        if (root == null) return;

        if (root.left == null && root.right == null) {
            codes.put(root.key, code);
            return;
        }

        assert root.left != null;
        getCodes(root.left, code + "0", codes);
        getCodes(root.right, code + "1", codes);
    }

    private void writeInt(FileOutputStream fileOutputStream, String binaryString) throws IOException {
        int leadingZerosCount = 32 - binaryString.length();
        binaryString = "0".repeat(leadingZerosCount) + binaryString;
        for (int i = 0; i < 4; i++) {
            String byteString = binaryString.substring(i * 8, (i + 1) * 8);
            int byteValue = Integer.parseInt(byteString, 2);
            fileOutputStream.write(byteValue);
            fileOutputStream.flush();
        }
    }

    private void writeEntry(FileOutputStream fileOutputStream, String key, String val) throws IOException {
        fileOutputStream.write(key.getBytes());
        fileOutputStream.write(val.length());
        writeInt(fileOutputStream, val);
    }

    private void writeCompressedFile(String filePath, Map<String, String> codes, String[] nBytes) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);

        // Write the number of entries in the dictionary.
        writeInt(fileOutputStream, Integer.toBinaryString(codes.size()));

        // Write the normal length of the characters (n).
        byte n = (byte) nBytes[0].length();
        fileOutputStream.write(n);

        // Write the length of the remaining characters after taking each n character together. The remaining
        // characters are the characters that are not a multiple of n. The remaining characters are the characters
        // on the last entry of the dictionary.
        byte remaining = (byte) nBytes[nBytes.length - 1].length();
        fileOutputStream.write(remaining);

        // Write the entries of the dictionary.
        String lastEntryKey = nBytes[nBytes.length - 1];
        String lastEntryVal = null;
        for (Map.Entry<String, String> entry : codes.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();

            if (key.equals(lastEntryKey)) {
                lastEntryVal = val;
                continue;
            }

            writeEntry(fileOutputStream, key, val);
        }

        // Write the last entry of the dictionary.
        assert lastEntryVal != null;
        writeEntry(fileOutputStream, lastEntryKey, lastEntryVal);

        // Convert the actual characters to their corresponding codewords.
        StringBuilder compressedData = new StringBuilder();
        for (String nByte : nBytes) compressedData.append(codes.get(nByte));

        // Write the length of the compressed data.
        fileOutputStream.write(compressedData.length());

        // Add padding to make the length of the compressed data a multiple of 8.
        int padding = 8 - compressedData.length() % 8;

        // Add a padding of 0s to the compressed data.
        compressedData.append("0".repeat(padding));

        // Loop over the compressed data and write each byte to the file.
        for (int i = 0; i < compressedData.length(); i+= 8) {
            String byteString = compressedData.substring(i, i + 8);
            int byteValue = Integer.parseInt(byteString, 2);
            fileOutputStream.write(byteValue);
            fileOutputStream.flush();
        }

        fileOutputStream.close();
    }

    /**
     * This function takes a file path and n, reads the file, compresses it, and writes the compressed file.
     * */
    public void compress(String filePath, int n) throws IOException {
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        String compressedFilePath = filePath.substring(0, filePath.lastIndexOf("\\") + 1) + "20011629." + n + "."
                + fileName + ".hc";

        long[] times = new long[5];

        long startTime = System.currentTimeMillis();
        String[] nBytes = readFileToNBytes(filePath, n);
        times[0] = System.currentTimeMillis() - startTime;
        System.out.println("Reading file: " + times[0] + " ms");

        startTime = System.currentTimeMillis();
        Map<String, Integer> frequencies = getFrequenciesForNBytes(nBytes);
        times[1] = System.currentTimeMillis() - startTime;
        System.out.println("Getting frequencies: " + times[1] + " ms");

        startTime = System.currentTimeMillis();
        Node root = huffmanCoding(frequencies);
        times[2] = System.currentTimeMillis() - startTime;
        System.out.println("Huffman coding: " + times[2] + " ms");

        startTime = System.currentTimeMillis();
        Map<String, String> codes = new HashMap<>();
        getCodes(root, "", codes);

        times[3] = System.currentTimeMillis() - startTime;
        System.out.println("Getting codes: " + times[3] + " ms");

        startTime = System.currentTimeMillis();
        writeCompressedFile(compressedFilePath, codes, nBytes);
        times[4] = System.currentTimeMillis() - startTime;
        System.out.println("Writing compressed file: " + times[4] + " ms");

        System.out.println("Total time: " + (times[0] + times[1] + times[2] + times[3] + times[4]) + " ms");
    }
}
