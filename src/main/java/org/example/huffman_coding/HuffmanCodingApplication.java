package org.example.huffman_coding;

import java.io.IOException;

public class HuffmanCodingApplication {
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
            compression.compress(inputFilePath, n);
        } else if (mode.equals("d")) {
            Decompression decompression = new Decompression();
            decompression.decompress(inputFilePath);
        } else {
            System.out.println("Invalid mode. Please enter either 'c' or 'd'.");
            System.exit(0);
        }
    }
}
