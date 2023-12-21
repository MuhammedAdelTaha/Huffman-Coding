package org.example.huffman_coding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * I acknowledge that I am aware of the academic integrity guidelines of this course, and that I worked on this
 * assignment independently without any unauthorized help.
 * */
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
        if (args.length != 2 && args.length != 3) {
            System.out.println("If you want to compress a file, run the program with the following arguments:");
            System.out.println("java -jar huffman_20011629.jar <c> <absolute_path_to_input_file> <n>");
            System.out.println("If you want to decompress a file, run the program with the following arguments:");
            System.out.println("java -jar huffman_20011629.jar <d> <absolute_path_to_input_file>");
            System.exit(0);
        }

        String mode = args[0];
        String inputFilePath = args[1];

        if (mode.equals("c")) {
            if (args.length != 3) {
                System.out.println("Please enter the value of n.");
                System.exit(0);
            }
            int n = Integer.parseInt(args[2]);
            Compression compression = new Compression();
            long startTime = System.currentTimeMillis();
            String compressedFilePath = compression.compress(inputFilePath, n);
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