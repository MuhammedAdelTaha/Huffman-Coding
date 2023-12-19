package org.example.huffman_coding;

import java.io.*;
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

    /**
     * This function takes a file path, reads all the file bytes, groups every n bytes together, and returns a list of
     * the hex representation of each group.
     * */
    private static List<String> readFileToNBytes(String filePath, int n) {
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            byte[] data = fileInputStream.readAllBytes();
            List<String> nBytes = new ArrayList<>();
            int i;
            for (i = 0; i < data.length - n + 1; i+= n) {
                StringBuilder key = new StringBuilder();
                for (int j = 0; j < n; j++) key.append(String.format("%x ", data[i + j]));
                nBytes.add(key.toString());
            }

            StringBuilder key = new StringBuilder();
            for (int j = 0; j < n && i + j < data.length; j++) key.append(String.format("%x ", data[i + j]));
            if (!key.isEmpty()) nBytes.add(key.toString());

            fileInputStream.close();
            return nBytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This function takes a list of hex strings and returns a map of each hex string and its frequency.
     * */
    private static Map<String, Integer> getFrequenciesForNBytes(List<String> nBytes) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String nByte : nBytes) frequencies.put(nByte, frequencies.getOrDefault(nByte, 0) + 1);
        return frequencies;
    }

    /**
     * This function takes a map of each hex string and its frequency and returns the root of the Huffman tree.
     * */
    private static node huffmanCoding(Map<String, Integer> frequencies) {
        PriorityQueue<node> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(o -> o.frequency));
        for (Map.Entry<String, Integer> entry : frequencies.entrySet())
            priorityQueue.add(new node(entry.getKey(), entry.getValue()));

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

    /**
     * This function takes the root of the Huffman tree and returns a map of each hex string and its codeword.
     * */
    private static void getCodes(node root, String code, Map<String, String> codes) {
        if (root.left == null && root.right == null) {
            codes.put(root.key, code);
            return;
        }

        assert root.left != null;
        getCodes(root.left, code + "0", codes);
        getCodes(root.right, code + "1", codes);
    }

    /**
     * This function takes a file path, a map of each hex string and its codeword, and a list of hex strings, and
     * writes the compressed file.
     * */
    private static void writeCompressedFile(String filePath, Map<String, String> codes, List<String> nBytes) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            int i = 0;
            StringBuilder dictionary = new StringBuilder();
            for (Map.Entry<String, String> entry : codes.entrySet()) {
                if (i == 0) dictionary.append("{");
                dictionary.append(entry.getKey()).append(":").append(entry.getValue());
                if (i == codes.size() - 1)
                    dictionary.append("}");
                else
                    dictionary.append(",");
                i++;
            }
            fileOutputStream.write(dictionary.toString().getBytes());

            // Convert the actual characters to their corresponding codewords.
            StringBuilder compressedData = new StringBuilder();
            for (String nByte : nBytes) compressedData.append(codes.get(nByte));

            // Write the length of the compressed data.
            String compressedDataLength = "{" + compressedData.length() + "}";
            fileOutputStream.write(compressedDataLength.getBytes());

            // Add padding to make the length of the compressed data a multiple of 8.
            int padding = 8 - compressedData.length() % 8;

            // Add a padding of 0s to the compressed data.
            compressedData.append("0".repeat(padding));

            // Loop over the compressed data and write each byte to the file.
            for (i = 0; i < compressedData.length(); i+= 8) {
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

        List<String> nBytes = readFileToNBytes(filePath, n);
        Map<String, Integer> frequencies = getFrequenciesForNBytes(nBytes);
        node root = huffmanCoding(frequencies);
        Map<String, String> codes = new HashMap<>();
        getCodes(root, "", codes);

        writeCompressedFile(compressedFilePath, codes, nBytes);
    }

    /**
     * This function takes a compressed file path and a decompressed file path, reads the compressed file, decompresses
     * it, and writes the decompressed file.
     * */
    public static void writeDecompressedFile(String compressedFilePath, String decompressedFilePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(compressedFilePath);
            FileOutputStream fileOutputStream = new FileOutputStream(decompressedFilePath);

            // Read the dictionary.
            StringBuilder codesString = new StringBuilder();
            int i;
            while ((i = fileInputStream.read()) != '}') {
                if (i == '{') continue;
                codesString.append((char) i);
            }

            // Convert the dictionary to a map of each codeword and its corresponding hex string.
            Map<String, String> codes = new HashMap<>();
            String[] codesArray = codesString.toString().split(",");
            for (String code : codesArray) {
                String[] codeArray = code.split(":");
                codes.put(codeArray[1], codeArray[0]);
            }

            // Read the length of the compressed data. This is used to know when to stop reading the compressed data.
            StringBuilder compressedDataLengthString = new StringBuilder();
            while ((i = fileInputStream.read()) != '}') {
                if (i == '{') continue;
                compressedDataLengthString.append((char) i);
            }
            int compressedDataLength = Integer.parseInt(compressedDataLengthString.toString());

            // Read the compressed data as a binary string.
            StringBuilder compressedDataBinaryString = new StringBuilder();
            while ((i = fileInputStream.read()) != -1) {
                String byteString = String.format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
                compressedDataBinaryString.append(byteString);
            }

            // Remove the padding from the compressed data.
            compressedDataBinaryString = new StringBuilder(compressedDataBinaryString.substring(0, compressedDataLength));

            // Loop over the compressed data and write each hex string to the file.
            for (i = 0; i < compressedDataBinaryString.length(); i++) {
                String code = "";
                while (!codes.containsKey(code) && i < compressedDataBinaryString.length()) {
                    code += compressedDataBinaryString.charAt(i);
                    i++;
                }
                if (code.isEmpty()) break;

                String[] characters = codes.get(code).split(" ");
                for (String character : characters) {
                    int characterValue = Integer.parseInt(character, 16);
                    fileOutputStream.write(characterValue);
                    fileOutputStream.flush();
                }
                i--;
            }

            fileInputStream.close();
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

        writeDecompressedFile(compressedFilePath, decompressedFilePath);
    }

    public static void main(String[] args) {
        int n = 2;
        String fileName = "test2.pdf";
        String filePath = "C:\\Users\\Mohamed Adel\\IdeaProjects\\huffman_coding\\test_cases\\" + fileName;
        String compressedFilePath = "C:\\Users\\Mohamed Adel\\IdeaProjects\\huffman_coding\\test_cases\\20011629." + n
                + "." + fileName + ".hc";
        compress(filePath, n);
        decompress(compressedFilePath);
    }

}
