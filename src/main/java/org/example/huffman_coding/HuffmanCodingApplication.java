package org.example.huffman_coding;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class HuffmanCodingApplication {

    private static class node {
        String key;
        int frequency;
        node left;
        node right;

        public node(String key, int frequency) {
            this.key = key;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }
    }

    // Input: byte[77, 111, 65, 100, 101, 108], n = 4
    private static String[] getStrings(byte[] data, int n) {
        int size = (data.length / n) + (data.length % n == 0 ? 0 : 1);
        String[] nBytes = new String[size];
        int i;
        for (i = 0; i < data.length - n + 1; i+= n) {
            StringBuilder key = new StringBuilder();
            for (int j = 0; j < n; j++) key.append((char) data[i + j]);
            nBytes[i / n] = key.toString();
        }

        StringBuilder key = new StringBuilder();
        for (int j = 0; j < n && i + j < data.length; j++) key.append((char) data[i + j]);
        if (!key.isEmpty()) nBytes[i / n] = key.toString();

        return nBytes;
    }
    // Output: [MoAd, el]

    // Input: test.txt with Content: MoAdel, n = 4
    private static String[] readFileToNBytes(String filePath, int n) {
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            byte[] data = fileInputStream.readAllBytes();

            fileInputStream.close();
            return getStrings(data, n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // Output: [MoAd, el]

    // Input: [MoAd, el]
    private static Map<String, Integer> getFrequenciesForNBytes(String[] nBytes) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String nByte : nBytes) frequencies.put(nByte, frequencies.getOrDefault(nByte, 0) + 1);
        return frequencies;
    }
    // Output: {MoAd=1, el=1}

    // Input: {MoAd=1, el=1}
    private static node huffmanCoding(Map<String, Integer> frequencies) {
        PriorityQueue<node> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.frequency));
        for (Map.Entry<String, Integer> entry : frequencies.entrySet())
            priorityQueue.add(new node(entry.getKey(), entry.getValue()));

        if (priorityQueue.size() == 1) {
            node peek = priorityQueue.poll();
            node root = new node(peek.key, peek.frequency);
            root.left = peek;
            return root;
        }

        while (priorityQueue.size() > 1) {
            node left = priorityQueue.poll();
            node right = priorityQueue.poll();
            assert right != null;
            node parent = new node(left.key + right.key, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            priorityQueue.add(parent);
        }

        return priorityQueue.poll();
    }
    // Output:
    // node{key='MoAdel', frequency=2,
    // left=node{key='MoAd', frequency=1, left=null, right=null},
    // right=node{key='el', frequency=1, left=null, right=null}}

    /**
     * This function takes the root of the Huffman tree and returns a Map of each hex string and its codeword.
     * */
    // Input:
    // node{key='MoAdel', frequency=2,
    // left=node{key='MoAd', frequency=1, left=null, right=null},
    // right=node{key='el', frequency=1, left=null, right=null}}
    // "", {}
    private static void getCodes(node root, String code, Map<String, String> codes) {
        if (root == null) return;

        if (root.left == null && root.right == null) {
            codes.put(root.key, code);
            return;
        }

        assert root.left != null;
        getCodes(root.left, code + "0", codes);
        getCodes(root.right, code + "1", codes);
    }
    // Output: {MoAd=0, el=1}

    private static void writeEntry(FileOutputStream fileOutputStream, String key, int leadingZerosCount, int decimalValue) {
        try {
            fileOutputStream.write(key.getBytes());
            fileOutputStream.write(leadingZerosCount);
            fileOutputStream.write(decimalValue);
            fileOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Input: filePath, {MoAd=0, el=1}, [MoAd, el]
    private static void writeCompressedFile(String filePath, Map<String, String> codes, String[] nBytes) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);

            // Write the number of entries in the dictionary.
            System.out.println(codes.size());
            String dictSizeBinaryStr = Integer.toBinaryString(codes.size());
            int dictSizeLeadingZerosCount = 32 - dictSizeBinaryStr.length();
            dictSizeBinaryStr = "0".repeat(dictSizeLeadingZerosCount) + dictSizeBinaryStr;
            System.out.println(dictSizeBinaryStr);
            for (int i = 0; i < 4; i++) {
                String byteString = dictSizeBinaryStr.substring(i * 8, (i + 1) * 8);
                int byteValue = Integer.parseInt(byteString, 2);
                fileOutputStream.write(byteValue);
                fileOutputStream.flush();
            }

            // Write the normal length of the characters (n).
            int n = nBytes[0].length();
            fileOutputStream.write(n);

            // Write the length of the remaining characters after taking each n character together. The remaining
            // characters are the characters that are not a multiple of n. The remaining characters are the characters
            // on the last entry of the dictionary.
            int remaining = nBytes[nBytes.length - 1].length();
            fileOutputStream.write(remaining);

            // Write the entries of the dictionary.
            String lastEntryKey = nBytes[nBytes.length - 1];
            int lastEntryLeadingZerosCount = 0;
            int lastEntryDecimalValue = 0;
            for (Map.Entry<String, String> entry : codes.entrySet()) {
                String key = entry.getKey();
                String binaryString = entry.getValue();
                int decimalValue = Integer.parseInt(binaryString, 2);
                int leadingZerosCount;
                for (leadingZerosCount = 0; leadingZerosCount < binaryString.length(); leadingZerosCount++)
                    if (binaryString.charAt(leadingZerosCount) == '1') break;

                if (decimalValue == 0) leadingZerosCount--;

                if (key.equals(lastEntryKey)) {
                    lastEntryLeadingZerosCount = leadingZerosCount;
                    lastEntryDecimalValue = decimalValue;
                    continue;
                }

                writeEntry(fileOutputStream, key, leadingZerosCount, decimalValue);
            }

            // Write the last entry of the dictionary.
            writeEntry(fileOutputStream, lastEntryKey, lastEntryLeadingZerosCount, lastEntryDecimalValue);

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This function takes a file path and n, reads the file, compresses it, and writes the compressed file.
     * */
    public static void compress(String filePath, int n) {
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
        node root = huffmanCoding(frequencies);
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

    public static Map<String, String> extractDict(List<Integer> data, int dictSize, int n, int remaining) {
        Map<String, String> codes = new HashMap<>();
        for (int entryIdx = 0; entryIdx < dictSize; entryIdx++) {
            int entryStartIdx = 3 + entryIdx * (n + 2);

            int entryKeyEndIdx = entryStartIdx;
            if (entryIdx == dictSize - 1) entryKeyEndIdx += remaining;
            else entryKeyEndIdx += n;

            StringBuilder entryKey = new StringBuilder();
            for (int i = entryStartIdx; i < entryKeyEndIdx; i++) entryKey.append((char) data.get(i).intValue());

            int leadingZerosCount = data.get(entryKeyEndIdx);
            int decimalValue = data.get(entryKeyEndIdx + 1);
            String binaryString = Integer.toBinaryString(decimalValue);
            binaryString = "0".repeat(leadingZerosCount) + binaryString;
            codes.put(binaryString, entryKey.toString());
        }
        return codes;
    }

    public static StringBuilder extractCompressedData(List<Integer> data, int startIdx, int dataSize) {
        StringBuilder compressedData = new StringBuilder();
        for (int i = startIdx; i < dataSize; i++) {
            String byteString = Integer.toBinaryString(data.get(i));
            byteString = "0".repeat(8 - byteString.length()) + byteString;
            compressedData.append(byteString);
        }
        return compressedData;
    }

    /**
     * This function takes a compressed file path and a decompressed file path, reads the compressed file, decompresses
     * it, and writes the decompressed file.
     * */
    public static void writeDecompressedFile(String compressedFilePath, String decompressedFilePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(compressedFilePath);

            int character;
            List<Integer> data = new ArrayList<>();
            while ((character = fileInputStream.read()) != -1) data.add(character);

            fileInputStream.close();

            List<Integer> dictSizeList = data.subList(0, 4);
            StringBuilder dictSizeBinaryStr = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                String byteString = Integer.toBinaryString(dictSizeList.get(i));
                byteString = "0".repeat(8 - byteString.length()) + byteString;
                dictSizeBinaryStr.append(byteString);
            }
            int dictSize = Integer.parseInt(dictSizeBinaryStr.toString(), 2);
            int n = data.get(4);
            int remaining = data.get(5);

            System.out.println(dictSize);
            System.out.println(n);
            System.out.println(remaining);

            Map<String, String> codes = extractDict(data, dictSize, n, remaining);
            int nextIdx = 3 + (dictSize - 1) * (n + 2) + remaining + 2;
            int compressedDataLength = data.get(nextIdx);
            nextIdx++;

            StringBuilder compressedData = extractCompressedData(data, nextIdx, data.size());

            FileOutputStream fileOutputStream = new FileOutputStream(decompressedFilePath);

            StringBuilder decompressedData = new StringBuilder();
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < compressedDataLength; i++) {
                current.append(compressedData.charAt(i));
                if (codes.containsKey(current.toString())) {
                    decompressedData.append(codes.get(current.toString()));
                    current = new StringBuilder();
                }
            }

            fileOutputStream.write(decompressedData.toString().getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This function takes a compressed file path, decompresses it, and writes the decompressed file.
     * */
    public static void decompress(String compressedFilePath) {
        String fileName = compressedFilePath.substring(compressedFilePath.lastIndexOf("\\") + 1);
        if (!fileName.endsWith(".hc")) {
            System.out.println("Invalid file extension.");
            return;
        }
        String decompressedFilePath = compressedFilePath.replace(fileName, "extracted." +
                fileName.substring(0, fileName.lastIndexOf(".")));

        long startTime = System.currentTimeMillis();
        writeDecompressedFile(compressedFilePath, decompressedFilePath);
        System.out.println("Writing decompressed file: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    public static void main(String[] args) {
        int n = 1;
        String fileName = "Algorithms - Lecture 7 and 8 (Greedy algorithms).pdf";
        String filePath = "C:\\Users\\Mohamed Adel\\IdeaProjects\\huffman_coding\\test_cases\\" + fileName;
        String compressedFilePath = "C:\\Users\\Mohamed Adel\\IdeaProjects\\huffman_coding\\test_cases\\20011629." + n
                + "." + fileName + ".hc";
        compress(filePath, n);
        decompress(compressedFilePath);
    }

}
