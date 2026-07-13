package com.yinyue.player.util;

import com.yinyue.player.model.Song;
import com.yinyue.player.service.LibraryService;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

public class BatchConverter {
    private final ExecutorService executor;
    private int completed = 0;
    private int total = 0;
    private volatile boolean cancelled = false;

    public BatchConverter() {
        executor = Executors.newFixedThreadPool(Math.min(4, Runtime.getRuntime().availableProcessors()));
    }

    public void convertAll(List<String> inputPaths, String outputDir, String outputFormat, ProgressCallback callback) {
        total = inputPaths.size();
        completed = 0;
        cancelled = false;

        for (String inputPath : inputPaths) {
            if (cancelled) break;
            executor.submit(() -> {
                try {
                    File inFile = new File(inputPath);
                    String outName = inFile.getName().replaceAll("\\.[^.]+$", "." + outputFormat.toLowerCase());
                    File outFile = new File(outputDir, outName);
                    FormatConverter.convert(inputPath, outFile.getAbsolutePath(), outputFormat);
                    completed++;
                    if (callback != null) {
                        javafx.application.Platform.runLater(() -> 
                            callback.onProgress(completed, total, inFile.getName())
                        );
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        javafx.application.Platform.runLater(() -> 
                            callback.onError(inputPath, e.getMessage())
                        );
                    }
                }
            });
        }
    }

    public void cancel() {
        cancelled = true;
        executor.shutdownNow();
    }

    public void shutdown() {
        executor.shutdown();
    }

    public interface ProgressCallback {
        void onProgress(int completed, int total, String currentFile);
        void onError(String file, String error);
    }
}