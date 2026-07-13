package com.yinyue.player.util;

import javafx.concurrent.Task;

import javax.sound.sampled.*;
import java.io.*;

public class FormatConverter {

    public static Task<Void> convertAsync(String inputPath, String outputPath, String targetFormat, ConversionCallback callback) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("正在读取音频文件...");
                    convert(inputPath, outputPath, targetFormat);
                    updateMessage("转换完成");
                    if (callback != null) {
                        javafx.application.Platform.runLater(() -> callback.onSuccess(outputPath));
                    }
                } catch (Exception e) {
                    updateMessage("转换失败: " + e.getMessage());
                    if (callback != null) {
                        javafx.application.Platform.runLater(() -> callback.onError(e.getMessage()));
                    }
                }
                return null;
            }
        };
        return task;
    }

    public static void convert(String inputPath, String outputPath, String targetFormat) throws Exception {
        File inputFile = new File(inputPath);
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat sourceFormat = sourceStream.getFormat();

        AudioFormat targetAudioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false
        );

        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetAudioFormat, sourceStream);

        // For WAV output, write directly
        if (targetFormat.equalsIgnoreCase("wav")) {
            AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, new File(outputPath));
        } else {
            // For other formats, write as WAV first (simplified)
            AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, new File(outputPath));
        }

        convertedStream.close();
        sourceStream.close();
    }

    public static String[] getSupportedInputFormats() {
        return new String[]{"wav", "au", "aiff"};
    }

    public static String[] getSupportedOutputFormats() {
        return new String[]{"wav"};
    }

    public interface ConversionCallback {
        void onSuccess(String outputPath);
        void onError(String error);
    }
}
