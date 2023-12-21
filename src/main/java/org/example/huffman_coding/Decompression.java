package org.example.huffman_coding;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Decompression {
    public Decompression() {}

    private String readInt(List<Integer> bytes) {
        StringBuilder binaryString = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String bitsString = Integer.toBinaryString(bytes.get(i));
            bitsString = "0".repeat(8 - bitsString.length()) + bitsString;
            binaryString.append(bitsString);
        }
        return binaryString.toString();
    }

    private Map<String, String> extractOppositeDict(List<Integer> data, int dictSize, int n, int remaining) {
        Map<String, String> codes = new HashMap<>();
        for (int entryIdx = 0; entryIdx < dictSize; entryIdx++) {
            int entryStartIdx = 6 + entryIdx * (n + 5);

            int entryKeyEndIdx = entryStartIdx;
            if (entryIdx == dictSize - 1) entryKeyEndIdx += remaining;
            else entryKeyEndIdx += n;

            StringBuilder entryKey = new StringBuilder();
            for (int i = entryStartIdx; i < entryKeyEndIdx; i++) entryKey.append((char) data.get(i).intValue());

            int codeLength = data.get(entryKeyEndIdx);
            List<Integer> codeBytes = data.subList(entryKeyEndIdx + 1, entryKeyEndIdx + 5);
            String s = readInt(codeBytes);
            int binaryStringStartIdx = s.length() - codeLength;
            String binaryString = s.substring(binaryStringStartIdx);
            codes.put(binaryString, entryKey.toString());
        }
        return codes;
    }

    private StringBuilder extractCompressedData(List<Integer> data, int startIdx, int dataSize) {
        StringBuilder compressedData = new StringBuilder();
        for (int i = startIdx; i < dataSize; i++) {
            String byteString = Integer.toBinaryString(data.get(i));
            byteString = "0".repeat(8 - byteString.length()) + byteString;
            compressedData.append(byteString);
        }
        return compressedData;
    }

    private void writeDecompressedFile(String filePath, Map<String, String> dict, StringBuilder compressedData, int len)
            throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);

        StringBuilder decompressedData = new StringBuilder();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < len; i++) {
            current.append(compressedData.charAt(i));
            if (dict.containsKey(current.toString())) {
                decompressedData.append(dict.get(current.toString()));
                current = new StringBuilder();
            }
        }

        fileOutputStream.write(decompressedData.toString().getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    /**
     * This function takes a compressed file path and a decompressed file path, reads the compressed file, decompresses
     * it, and writes the decompressed file.
     * */
    private void decompressFile(String compressedFilePath, String decompressedFilePath) throws IOException {

        FileInputStream fileInputStream = new FileInputStream(compressedFilePath);

        int character;
        List<Integer> data = new ArrayList<>();
        while ((character = fileInputStream.read()) != -1) data.add(character);

        fileInputStream.close();

        // Extract the dictionary size, n, and remaining.
        List<Integer> dictSizeList = data.subList(0, 4);
        int dictSize = Integer.parseInt(readInt(dictSizeList), 2);
        int n = data.get(4);
        int remaining = data.get(5);

        // Extract the opposite dictionary (codes to data).
        Map<String, String> codesToData = extractOppositeDict(data, dictSize, n, remaining);
        int nextIdx = 6 + (dictSize - 1) * (n + 5) + remaining + 5;
        int compressedDataLength = data.get(nextIdx);
        nextIdx++;

        // Extract the compressed data.
        StringBuilder compressedData = extractCompressedData(data, nextIdx, data.size());

        // Write the decompressed file.
        writeDecompressedFile(decompressedFilePath, codesToData, compressedData, compressedDataLength);
    }

    /**
     * This function takes a compressed file path, decompresses it, and writes the decompressed file.
     * */
    public void decompress(String compressedFilePath) throws IOException {
        String fileName = compressedFilePath.substring(compressedFilePath.lastIndexOf("\\") + 1);
        if (!fileName.endsWith(".hc")) {
            System.out.println("Invalid file extension.");
            return;
        }
        String decompressedFilePath = compressedFilePath.replace(fileName, "extracted." +
                fileName.substring(0, fileName.lastIndexOf(".")));

        long startTime = System.currentTimeMillis();
        decompressFile(compressedFilePath, decompressedFilePath);
        System.out.println("Writing decompressed file: " + (System.currentTimeMillis() - startTime) + " ms");
    }
}
