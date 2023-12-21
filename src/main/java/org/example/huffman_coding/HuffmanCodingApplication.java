package org.example.huffman_coding;

import java.io.IOException;

public class HuffmanCodingApplication {
    public static void main(String[] args) throws IOException {
        int n = 1;
        String fileName = "test.txt";
        String filePath = "C:\\Users\\Mohamed Adel\\IdeaProjects\\huffman_coding\\test_cases\\" + fileName;
        String compressedFilePath = "C:\\Users\\Mohamed Adel\\IdeaProjects\\huffman_coding\\test_cases\\20011629." + n
                + "." + fileName + ".hc";

        Compression compression = new Compression();
        Decompression decompression = new Decompression();

        System.out.println("------------------ Compression ------------------");
        compression.compress(filePath, n);

        System.out.println("------------------ Decompression ------------------");
        decompression.decompress(compressedFilePath);
    }
}
