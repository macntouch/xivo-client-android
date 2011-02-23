package com.proformatique.android.xivoclient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

public class FileDumpExceptionHandler implements UncaughtExceptionHandler {
    
    private final static String path = "/sdcard/pf";
    private final static String file = "stacktrace.txt";
    private UncaughtExceptionHandler dueh;
    
    public FileDumpExceptionHandler() {
        dueh = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        writeToFile(stacktrace, path + "/" + file);
        dueh.uncaughtException(thread, ex);
    }
    
    private void writeToFile(String stacktrace, String file) {
        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(file));
            bos.append(stacktrace);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
