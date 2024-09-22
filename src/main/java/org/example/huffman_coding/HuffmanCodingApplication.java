package org.example.huffman_coding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class HuffmanCodingApplication {
    private static float getCompressionRatio(String inputFilePath, String outputFilePath) throws IOException {
        Path uncompressedFilePath = Paths.get(inputFilePath);
        Path compressedFilePath = Paths.get(outputFilePath);
        if (!Files.exists(uncompressedFilePath)) {
            System.out.println("The input file does not exist.");
            System.exit(0);
        }
        if (!Files.exists(compressedFilePath)) {
            System.out.println("The output file does not exist.");
            System.exit(0);
        }
        long uncompressedFileSize = Files.size(uncompressedFilePath);
        long compressedFileSize = Files.size(compressedFilePath);
        return (float) compressedFileSize / uncompressedFileSize;
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Absolute file path: ");
        String inputFilePath = scanner.nextLine();

        System.out.println("Mode: ");
        String mode = scanner.nextLine();

        if (mode.equals("c")) {
            Compression compression = new Compression();
            long startTime = System.currentTimeMillis();
            String compressedFilePath = compression.compress(inputFilePath);
            System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + " ms");
            System.out.println("Compression ratio: " + getCompressionRatio(inputFilePath, compressedFilePath));
        } else if (mode.equals("d")) {
            Decompression decompression = new Decompression();
            long startTime = System.currentTimeMillis();
            decompression.decompress(inputFilePath);
            System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            System.out.println("Invalid mode. Please enter either 'c' or 'd'.");
            System.exit(0);
        }
    }
}