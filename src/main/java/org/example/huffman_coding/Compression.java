package org.example.huffman_coding;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Compression {
    public Compression() {}

    private static class Node {
        byte key;
        int frequency;
        Node left;
        Node right;

        public Node(byte key, int frequency) {
            this.key = key;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }
    }

    /**
     * This function takes a list of bytes and writes an integer to the file.
     * The integer is represented as a string
     * of bits (4-bytes).
     * */
    private void writeInt(FileOutputStream fileOutputStream, String binaryString) throws IOException {
        binaryString = String.format("%32s", binaryString).replace(' ', '0');
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
     * This function takes a list of n bytes hex strings and returns a map of each hex string and its frequency.
     * */
    private Map<Byte, Integer> getFrequenciesForNBytes(byte[] nBytes) {
        int[] frequencies = new int[256];
        for (byte nByte : nBytes)
            frequencies[nByte & 0xFF]++;

        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (int i = 0; i < 256; i++)
            if (frequencies[i] > 0)
                frequencyMap.put((byte) i, frequencies[i]);

        return frequencyMap;
    }

    /**
     * This function takes a map of each hex string and its frequency and returns the root of the Huffman tree.
     * */
    private Node huffmanCoding(Map<Byte, Integer> frequencies) {
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.frequency));
        for (Map.Entry<Byte, Integer> entry : frequencies.entrySet())
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
            Node parent = new Node((byte)(left.key | right.key), left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            priorityQueue.add(parent);
        }

        return priorityQueue.poll();
    }

    /**
     * This function takes the root of the Huffman tree and returns a Map of each hex string and its codeword.
     * */
    private void getCodes(Node root, String code, Map<Byte, String> codes) {
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
     * This function takes a file output stream, a dictionary, and the last entry key and writes the dictionary to the
     * file.
     * */
    private void writeDict(FileOutputStream fileOutputStream, Map<Byte, String> codes) throws IOException {

        for (Map.Entry<Byte, String> entry : codes.entrySet()) {
            Byte key = entry.getKey();
            String val = entry.getValue();
            fileOutputStream.write(key);
            fileOutputStream.write(":".getBytes());
            fileOutputStream.write(val.length());
            writeInt(fileOutputStream, val);
        }
    }

    /**
     * This function takes a file output stream, a map of each hex string and its codeword, and a list of n bytes hex
     * strings, and writes the compressed data to the file.
     * */
    private void writeCompressedData(FileOutputStream fileOutputStream, Map<Byte, String> codes, byte[] nBytes)
            throws IOException {
        // Convert the actual characters to their corresponding codewords.
        StringBuilder compressedData = new StringBuilder();
        for (byte nByte : nBytes) compressedData.append(codes.get(nByte));

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

        // Write the number of bytes that the compressed data takes.
        writeInt(fileOutputStream, Integer.toBinaryString(bytes.length));

        // Write the compressed data to the file.
        fileOutputStream.write(bytes);
        fileOutputStream.flush();
    }

    /**
     * This function takes a file path, a map of each hex string and its codeword, and a list of n bytes hex strings,
     * and writes the compressed file.
     * */
    private void writeCompressedFile(FileOutputStream fileOutputStream, Map<Byte, String> codes, byte[] nBytes)
            throws IOException {
        // Write the number of entries in the dictionary.
        writeInt(fileOutputStream, Integer.toBinaryString(codes.size()));

        // Write the dictionary.
        writeDict(fileOutputStream, codes);

        // Write the compressed data and its length.
        writeCompressedData(fileOutputStream, codes, nBytes);
    }

    /**
     * This function takes a file path and n, reads the file, compresses it, and writes the compressed file.
     * */
    public String compress(String filePath) throws IOException {
        String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
        String compressedFilePath = filePath.substring(0, filePath.lastIndexOf("\\") + 1) + fileName + ".hc";

        FileInputStream fileInputStream = new FileInputStream(filePath);
        FileOutputStream fileOutputStream = new FileOutputStream(compressedFilePath);

        // Chunk size is 22680 bytes.
        int chunkSize = 22680;
        // Get the size of the file.
        int fileSize = fileInputStream.available();

        for (int i = 0; i < fileSize; i += chunkSize) {
            int available = Math.min(chunkSize, fileSize - i);
            // Read the file in chunks of size chunkSize.
            byte[] data = fileInputStream.readNBytes(available);

            // Get the frequencies of the n bytes hex strings.
            Map<Byte, Integer> frequencies = getFrequenciesForNBytes(data);

            // Get the root of the Huffman tree.
            Node root = huffmanCoding(frequencies);

            // Get the codewords of the n bytes hex strings.
            Map<Byte, String> codes = new HashMap<>();
            getCodes(root, "", codes);

            // Write the compressed file.
            writeCompressedFile(fileOutputStream, codes, data);
        }
        fileInputStream.close();
        fileOutputStream.close();

        return compressedFilePath;
    }
}