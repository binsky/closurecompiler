package org.smartjava;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ClosureCompiler {
    private static List<SourceFile> EXTERNS_LIST = new ArrayList<SourceFile>();
    private static final CompilerOptions OPTIONS = new CompilerOptions();
    private static final String ORIGINAL_PATH = "src/main/js/original/";
    private static final String CLOSURED_PATH = "src/main/js/closured/min-";
    static {
        try {
            EXTERNS_LIST = CommandLineRunner.getDefaultExterns();
        } catch (IOException e) {
            e.printStackTrace();
        }

        com.google.javascript.jscomp.Compiler.setLoggingLevel(Level.INFO);
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(OPTIONS);
        WarningLevel.VERBOSE.setOptionsForWarningLevel(OPTIONS);
    }
    static String compile(String fileName, String inputJS) throws IOException {
        final Compiler compiler = new Compiler();

        String inputFilename = ORIGINAL_PATH + fileName;
        String outputFilename = CLOSURED_PATH + fileName;
        FileWriter file = new FileWriter(inputFilename);
        FileWriter outputFile = new FileWriter(outputFilename);

        file.write(inputJS);
        file.close();

        final List<SourceFile> jsSourceFiles = new ArrayList<>();
        jsSourceFiles.add(SourceFile.fromFile(inputFilename));

        compiler.compile(EXTERNS_LIST, jsSourceFiles, OPTIONS);
        for (JSError message : compiler.getWarnings()) {
            System.err.println("Warning message: " + message.toString());
        }

        for (JSError message : compiler.getErrors()) {
            System.err.println("Error message: " + message.toString());
        }
        String outputJS = compiler.toSource();
        outputFile.write(outputJS);
        outputFile.close();
        return outputJS;
    }

    static String readFile(String fileName, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(CLOSURED_PATH + fileName));
        return new String(encoded, encoding);
    }
}
