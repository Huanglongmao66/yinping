package com.yinyue.player.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WaveformVisualizer extends Canvas {
    private float[] samples;
    private int sampleRate;
    private int channels;

    public WaveformVisualizer() {
        super(600, 120);
    }

    public void loadAudioFile(String filePath) {
        try {
            File file = new File(filePath);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioInputStream.getFormat();
            this.sampleRate = (int) format.getSampleRate();
            this.channels = format.getChannels();

            byte[] audioBytes = audioInputStream.readAllBytes();
            audioInputStream.close();

            samples = convertBytesToSamples(audioBytes, format);
            drawWaveform();
        } catch (Exception e) {
            drawPlaceholder();
        }
    }

    private float[] convertBytesToSamples(byte[] audioBytes, AudioFormat format) {
        int sampleSize = format.getSampleSizeInBits() / 8;
        boolean bigEndian = format.isBigEndian();
        int numSamples = audioBytes.length / (sampleSize * channels);
        float[] result = new float[numSamples];

        ByteBuffer buffer = ByteBuffer.wrap(audioBytes);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            float sample = 0;
            for (int ch = 0; ch < channels; ch++) {
                if (sampleSize == 2) {
                    sample += buffer.getShort() / 32768.0f;
                } else if (sampleSize == 4) {
                    sample += buffer.getInt() / 2147483648.0f;
                } else {
                    sample += (buffer.get() & 0xFF) / 128.0f - 1.0f;
                }
            }
            result[i] = sample / channels;
        }
        return result;
    }

    public void drawWaveform() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        gc.clearRect(0, 0, width, height);

        if (samples == null || samples.length == 0) {
            drawPlaceholder();
            return;
        }

        gc.setStroke(Color.web("#e94560"));
        gc.setLineWidth(1.5);

        int samplesPerPixel = Math.max(1, samples.length / (int) width);
        double centerY = height / 2;

        gc.beginPath();
        gc.moveTo(0, centerY);

        for (int x = 0; x < width; x++) {
            int startIdx = x * samplesPerPixel;
            int endIdx = Math.min(startIdx + samplesPerPixel, samples.length);

            float max = 0;
            for (int i = startIdx; i < endIdx; i++) {
                if (Math.abs(samples[i]) > max) {
                    max = Math.abs(samples[i]);
                }
            }

            double y = centerY - max * centerY * 0.9;
            gc.lineTo(x, y);
        }

        gc.stroke();

        // Draw mirrored bottom
        gc.beginPath();
        gc.moveTo(0, centerY);
        for (int x = 0; x < width; x++) {
            int startIdx = x * samplesPerPixel;
            int endIdx = Math.min(startIdx + samplesPerPixel, samples.length);
            float max = 0;
            for (int i = startIdx; i < endIdx; i++) {
                if (Math.abs(samples[i]) > max) {
                    max = Math.abs(samples[i]);
                }
            }
            double y = centerY + max * centerY * 0.9;
            gc.lineTo(x, y);
        }
        gc.stroke();
    }

    public void drawPlaceholder() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setStroke(Color.web("#333333"));
        gc.setLineWidth(1);
        gc.strokeLine(0, height / 2, width, height / 2);
    }

    public void clear() {
        samples = null;
        drawPlaceholder();
    }
}
