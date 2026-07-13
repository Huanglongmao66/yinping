package com.yinyue.player.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import javax.sound.sampled.*;
import java.io.*;

public class AudioSplitter {
    public static Task<Void> splitAsync(String inputPath, String outputPath,
                                         long startMs, long endMs, SplitCallback callback) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("正在读取音频文件...");
                    File inputFile = new File(inputPath);
                    AudioInputStream sourceStream = AudioSystem.getAudioInputStream(inputFile);
                    AudioFormat format = sourceStream.getFormat();

                    float sampleRate = format.getSampleRate();
                    int frameSize = format.getFrameSize();
                    long startFrame = (long) (startMs / 1000.0 * sampleRate);
                    long endFrame = (long) (endMs / 1000.0 * sampleRate);
                    long totalFrames = endFrame - startFrame;

                    sourceStream.skip(startFrame * frameSize);

                    long bytesToRead = totalFrames * frameSize;
                    byte[] buffer = new byte[8192];
                    long totalRead = 0;

                    File outFile = new File(outputPath);
                    try (AudioInputStream subStream = new AudioInputStream(sourceStream, format, totalFrames)) {
                        AudioSystem.write(subStream, AudioFileFormat.Type.WAVE, outFile);
                    }

                    sourceStream.close();
                    updateMessage("分割完成");
                    if (callback != null) {
                        Platform.runLater(() -> callback.onSuccess(outputPath));
                    }
                } catch (Exception e) {
                    updateMessage("分割失败: " + e.getMessage());
                    if (callback != null) {
                        Platform.runLater(() -> callback.onError(e.getMessage()));
                    }
                }
                return null;
            }
        };
        return task;
    }

    public interface SplitCallback {
        void onSuccess(String outputPath);
        void onError(String error);
    }
}
