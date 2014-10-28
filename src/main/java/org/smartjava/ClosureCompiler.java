package org.smartjava;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ClosureCompiler {
    private static List<SourceFile> EXTERNS_LIST = new ArrayList<SourceFile>();
    private static final CompilerOptions OPTIONS = new CompilerOptions();

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

    static String compile(List<SourceFile> jsSourceFiles) {
        final Compiler compiler = new Compiler();

        compiler.compile(EXTERNS_LIST, jsSourceFiles, OPTIONS);
        for (JSError message : compiler.getWarnings()) {
            System.err.println("Warning message: " + message.toString());
        }

        for (JSError message : compiler.getErrors()) {
            System.err.println("Error message: " + message.toString());
        }
        return compiler.toSource();
    }
}
