package com.yinyue.player.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import javax.sound.sampled.*;
import java.io.*;

public class AudioRecorder {
    private TargetDataLine line;
    private boolean recording = false;
    private Thread recordingThread;
    private File outputFile;
    private RecordingCallback callback;

    public interface RecordingCallback {
        void onStarted();
        void onStopped(File file);
        void onError(String error);
    }

    public void startRecording(File file, RecordingCallback cb) {
        if (recording) return;
        this.outputFile = file;
        this.callback = cb;

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    line = (TargetDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    line.start();
                    recording = true;

                    Platform.runLater(() -> { if (callback != null) callback.onStarted(); });

                    try (AudioInputStream ais = new AudioInputStream(line)) {
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> { if (callback != null) callback.onError(e.getMessage()); });
                }
                return null;
            }
        };

        recordingThread = new Thread(task);
        recordingThread.start();
    }

    public void stopRecording() {
        if (!recording || line == null) return;
        recording = false;
        line.stop();
        line.close();
        line = null;
        if (callback != null) {
            Platform.runLater(() -> callback.onStopped(outputFile));
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public static boolean isMicrophoneAvailable() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            return false;
        }
    }
}
