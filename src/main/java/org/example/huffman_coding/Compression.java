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

    /**
     * This function takes a list of bytes, and writes an integer to the file. The integer is represented as a string
     * of bits (4 bytes).
     * */
    private void writeInt(FileOutputStream fileOutputStream, String binaryString) throws IOException {
        int leadingZerosCount = 32 - binaryString.length();
        binaryString = "0".repeat(leadingZerosCount) + binaryString;
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            String byteString = binaryString.substring(i * 8, (i + 1) * 8);
            int byteValue = Integer.parseInt(byteString, 2);
            bytes[i] = (byte) byteValue;
        }
        fileOutputStream.write(bytes);
        fileOutputStream.flush();
    }

    /**
     * This function takes a list of bytes, and returns a list of hex strings of n bytes. Every n bytes represented as
     * a hex string separated by a space. The last hex string may be less than n bytes.
     * */
    private String[] getHexStrings(byte[] data, int n) {
        int size = (data.length / n) + (data.length % n == 0 ? 0 : 1);
        String[] nBytes = new String[size];
        int i;
        for (i = 0; i < data.length - n + 1; i+= n) {
            StringBuilder key = new StringBuilder();
            for (int j = 0; j < n; j++) key.append(String.format("%02X", data[i + j]));
            nBytes[i / n] = key.toString();
        }

        StringBuilder key = new StringBuilder();
        for (int j = 0; j < n && i + j < data.length; j++) key.append(String.format("%02X", data[i + j]));
        if (!key.isEmpty()) nBytes[i / n] = key.toString();

        return nBytes;
    }

    /**
     * This function takes a list of n bytes hex strings, and returns a map of each hex string and its frequency.
     * */
    private Map<String, Integer> getFrequenciesForNBytes(String[] nBytes) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String nByte : nBytes) frequencies.put(nByte, frequencies.getOrDefault(nByte, 0) + 1);
        return frequencies;
    }

    /**
     * This function takes a map of each hex string and its frequency, and returns the root of the Huffman tree.
     * */
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

    /**
     * This function takes a file output stream, a dictionary, and the last entry key, and writes the dictionary to the
     * file.
     * */
    private void writeDict(FileOutputStream fileOutputStream, Map<String, String> codes) throws IOException {
        // Write the entries of the dictionary.
        for (Map.Entry<String, String> entry : codes.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            fileOutputStream.write(key.getBytes());
            fileOutputStream.write(val.length());
            writeInt(fileOutputStream, val);
        }
    }

    /**
     * This function takes a file output stream, a map of each hex string and its codeword, and a list of n bytes hex
     * strings, and writes the compressed data to the file.
     * */
    private void writeCompressedData(FileOutputStream fileOutputStream, Map<String, String> codes, String[] nBytes)
            throws IOException {
        // Convert the actual characters to their corresponding codewords.
        StringBuilder compressedData = new StringBuilder();
        for (String nByte : nBytes) compressedData.append(codes.get(nByte));

        // Write the length of the compressed data.
        int compressedDataLength = compressedData.length();
        writeInt(fileOutputStream, Integer.toBinaryString(compressedDataLength));

        // Add padding to make the length of the compressed data a multiple of 8.
        int padding = 8 - compressedData.length() % 8;

        // Add a padding of 0s to the compressed data.
        compressedData.append("0".repeat(padding));

        // Loop over the compressed data and write each byte to the file.
        byte[] bytes = new byte[compressedData.length() / 8];
        for (int i = 0; i < compressedData.length(); i+= 8) {
            String byteString = compressedData.substring(i, i + 8);
            int byteValue = Integer.parseInt(byteString, 2);
            bytes[i / 8] = (byte) byteValue;
        }
        fileOutputStream.write(bytes);
        fileOutputStream.flush();
    }

    /**
     * This function takes a file path, a map of each hex string and its codeword, and a list of n bytes hex strings,
     * and writes the compressed file.
     * */
    private void writeCompressedFile(FileOutputStream fileOutputStream, Map<String, String> codes, String[] nBytes, int n) throws IOException {
        // Write the number of entries in the dictionary.
        writeInt(fileOutputStream, Integer.toBinaryString(codes.size()));

        // Write the number of bytes taken together (n).
        fileOutputStream.write(n);

        // Write the dictionary.
        writeDict(fileOutputStream, codes);

        // Write the compressed data and its length.
        writeCompressedData(fileOutputStream, codes, nBytes);
    }

    /**
     * This function takes a file path and n, reads the file, compresses it, and writes the compressed file.
     * */
    public String compress(String filePath, int n) throws IOException {
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        String compressedFilePath = filePath.substring(0, filePath.lastIndexOf("\\") + 1) + "20011629." + n + "."
                + fileName + ".hc";

        FileInputStream fileInputStream = new FileInputStream(filePath);
        FileOutputStream fileOutputStream = new FileOutputStream(compressedFilePath);
        int chunkSize = 16384 - 16384 % n;
        int fileSize = fileInputStream.available();

        long readTime = 0;
        long frequenciesTime = 0;
        long huffmanCodingTime = 0;
        long codesTime = 0;
        long writeTime = 0;
        for (int i = 0; i < fileSize; i += chunkSize) {
            int available = Math.min(chunkSize, fileSize - i);
            long startTime = System.currentTimeMillis();
            byte[] data = fileInputStream.readNBytes(available);
            String[] nBytes = getHexStrings(data, n);
            readTime += System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            Map<String, Integer> frequencies = getFrequenciesForNBytes(nBytes);
            frequenciesTime += System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            Node root = huffmanCoding(frequencies);
            huffmanCodingTime += System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            Map<String, String> codes = new HashMap<>();
            getCodes(root, "", codes);
            codesTime += System.currentTimeMillis() - startTime;

            startTime = System.currentTimeMillis();
            writeCompressedFile(fileOutputStream, codes, nBytes, n);
            writeTime += System.currentTimeMillis() - startTime;
        }
        fileInputStream.close();
        fileOutputStream.close();

        System.out.println("Read time: " + readTime + " ms");
        System.out.println("Frequencies time: " + frequenciesTime + " ms");
        System.out.println("Huffman coding time: " + huffmanCodingTime + " ms");
        System.out.println("Codes time: " + codesTime + " ms");
        System.out.println("Write time: " + writeTime + " ms");
        return compressedFilePath;
    }
}