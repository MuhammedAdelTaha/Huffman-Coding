package org.example.huffman_coding;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Decompression {
    public Decompression() {}

    private int[] readNBytes(FileInputStream fileInputStream) throws IOException {
        int[] bytes = new int[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = fileInputStream.read();
            if (bytes[i] < 0) bytes[i] += 256;
        }
        return bytes;
    }

    /**
     * This function takes a list of bytes, and returns a string of bits representing the integer.
     * */
    private String readInt(int[] bytes) {
        StringBuilder binaryString = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String bitsString = Integer.toBinaryString(bytes[i]);
            bitsString = "0".repeat(8 - bitsString.length()) + bitsString;
            binaryString.append(bitsString);
        }
        return binaryString.toString();
    }

    /**
     * This function takes a list of bytes, a dictionary size, n, and remaining, and returns a dictionary (data to
     * codes).
     * */
    private Map<String, String> extractOppositeDict(FileInputStream fileInputStream, int dictSize) throws IOException {
        Map<String, String> codes = new HashMap<>();
        for (int entryIdx = 0; entryIdx < dictSize; entryIdx++) {
            StringBuilder entryKey = new StringBuilder();
            char c = (char) fileInputStream.read();
            while (c != ':') {
                entryKey.append(c);
                c = (char) fileInputStream.read();
            }

            int codeLength = fileInputStream.read();
            int[] codeBytes = readNBytes(fileInputStream);

            String s = readInt(codeBytes);
            int binaryStringStartIdx = s.length() - codeLength;
            String binaryString = s.substring(binaryStringStartIdx);
            codes.put(binaryString, entryKey.toString());
        }
        return codes;
    }

    /**
     * This function takes a list of bytes, a start index, and a length, and returns a string of bits representing the
     * compressed data.
     * */
    private StringBuilder extractCompressedData(FileInputStream fileInputStream, int bytesCount, int len) throws IOException {
        StringBuilder compressedData = new StringBuilder();

        int i = 0;
        while (i++ < bytesCount) {
            String byteString = Integer.toBinaryString(fileInputStream.read());
            byteString = "0".repeat(8 - byteString.length()) + byteString;
            compressedData.append(byteString);
        }
        return new StringBuilder(compressedData.substring(0, len));
    }

    /**
     * This function takes a decompressed file path, a dictionary, a compressed data, and its length, and writes the
     * decompressed file.
     * */
    private void writeDecompressedFile(FileOutputStream fileOutputStream, Map<String, String> dict,
                                       StringBuilder compressedData, int len) throws IOException {

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < len; i++) {
            current.append(compressedData.charAt(i));
            if (dict.containsKey(current.toString())) {
                String[] byteStrings = dict.get(current.toString()).split(" ");
                for (String byteString : byteStrings)  {
                     fileOutputStream.write(Integer.parseInt(byteString));
                     fileOutputStream.flush();
                }
                current = new StringBuilder();
            }
        }
    }

    /**
     * This function takes a compressed file path and a decompressed file path, reads the compressed file, decompresses
     * it, and writes the decompressed file.
     * */
    private void decompressFile(FileInputStream fileInputStream, FileOutputStream fileOutputStream) throws IOException {

        // Extract the dictionary size, n, and remaining.
        int[] dictSizeList = readNBytes(fileInputStream);
        int dictSize = Integer.parseInt(readInt(dictSizeList), 2);

        // Extract the opposite dictionary (codes to data).
        Map<String, String> codesToData = extractOppositeDict(fileInputStream, dictSize);

        // Extract the length of the compressed data.
        int[] compressedDataLengthList = readNBytes(fileInputStream);
        int compressedDataLength = Integer.parseInt(readInt(compressedDataLengthList), 2);

        // Extract the number of bytes that the compressed data takes.
        int[] compressedDataBytesCountList = readNBytes(fileInputStream);
        int compressedDataBytesCount = Integer.parseInt(readInt(compressedDataBytesCountList), 2);

        // Extract the compressed data.
        StringBuilder compressedData = extractCompressedData(fileInputStream, compressedDataBytesCount, compressedDataLength);

        // Write the decompressed file.
        writeDecompressedFile(fileOutputStream, codesToData, compressedData, compressedDataLength);
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

        // Decompress the file.
        FileInputStream fileInputStream = new FileInputStream(compressedFilePath);
        FileOutputStream fileOutputStream = new FileOutputStream(decompressedFilePath);

        while (fileInputStream.available() > 0) decompressFile(fileInputStream, fileOutputStream);

        fileInputStream.close();
        fileOutputStream.close();
    }
}
