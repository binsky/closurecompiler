package org.smartjava;

import com.google.javascript.jscomp.SourceFile;
import org.vertx.java.platform.Verticle;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileManager extends Verticle {
    private static final String ORIGINAL_PATH = "src/main/js/original/";
    private static final String CLOSURED_PATH = "src/main/js/closured/min-";

    static List<SourceFile> createSourceFile(String fileName, String inputJS) throws IOException {
        String inputFilename = ORIGINAL_PATH + fileName;
        FileWriter file = new FileWriter(inputFilename);

        file.write(inputJS);
        file.close();

        final List<SourceFile> jsSourceFiles = new ArrayList<>();
        jsSourceFiles.add(SourceFile.fromFile(inputFilename));
        return jsSourceFiles;
    }

    static void createClosuredFile(String fileName, String closuredJS) throws IOException {
        String outputFilename = CLOSURED_PATH + fileName;
        FileWriter outputFile = new FileWriter(outputFilename);
        outputFile.write(closuredJS);
        outputFile.close();
    }

    static String getJSCode(String fileName, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(CLOSURED_PATH + fileName));
        return new String(encoded, encoding);
    }

    static boolean deleteFiles(String fileName) {
        Path originalFile = Paths.get(ORIGINAL_PATH + fileName);
        Path closuredFile = Paths.get(CLOSURED_PATH + fileName);
        try {
            Files.deleteIfExists(originalFile);
            Files.deleteIfExists(closuredFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
