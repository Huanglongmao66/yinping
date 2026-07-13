package com.yinyue.player.util;

import com.yinyue.player.model.Song;
import com.yinyue.player.service.LibraryService;

import javax.sound.sampled.*;
import java.io.*;
import java.util.List;

public class ReplayGainUtil {
    private static final double TARGET_LOUDNESS = -14.0; // LUFS

    public static double analyzeReplayGain(String filePath) {
        try {
            File file = new File(filePath);
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            AudioFormat format = ais.getFormat();

            long frameLength = ais.getFrameLength();
            int frameSize = format.getFrameSize();
            if (frameLength <= 0 || frameSize <= 0) return 0;

            byte[] buffer = new byte[8192];
            double sumSquares = 0;
            long sampleCount = 0;

            int channels = format.getChannels();
            int sampleSize = format.getSampleSizeInBits() / 8;
            boolean bigEndian = format.isBigEndian();

            int read;
            while ((read = ais.read(buffer)) > 0) {
                int samples = read / (sampleSize * channels);
                for (int i = 0; i < samples; i++) {
                    int offset = i * sampleSize * channels;
                    double sample = 0;
                    for (int ch = 0; ch < channels; ch++) {
                        int idx = offset + ch * sampleSize;
                        if (idx + sampleSize <= read) {
                            short val;
                            if (sampleSize == 2) {
                                if (bigEndian) {
                                    val = (short) ((buffer[idx] << 8) | (buffer[idx + 1] & 0xFF));
                                } else {
                                    val = (short) ((buffer[idx + 1] << 8) | (buffer[idx] & 0xFF));
                                }
                                sample += val / 32768.0;
                            }
                        }
                    }
                    sample /= channels;
                    sumSquares += sample * sample;
                    sampleCount++;
                }
            }
            ais.close();

            if (sampleCount == 0) return 0;
            double rms = Math.sqrt(sumSquares / sampleCount);
            double rmsDb = 20 * Math.log10(Math.max(rms, 1e-10));
            return TARGET_LOUDNESS - rmsDb;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void normalizeLibrary() {
        LibraryService library = LibraryService.getInstance();
        List<Song> songs = library.getSongs();
        for (Song song : songs) {
            if (song.getFilePath() != null) {
                double gain = analyzeReplayGain(song.getFilePath());
                song.setReplayGain(gain);
            }
        }
    }
}
