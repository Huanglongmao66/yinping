package com.yinyue.player.util;

import java.util.Arrays;
import java.util.List;

public class AudioUtils {
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "mp3", "wav", "flac", "ogg", "aac", "m4a", "wma", "ape"
    );

    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00";
        }
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static String formatTimeFull(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00:00";
        }
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    public static String formatBitRate(int bitRate) {
        if (bitRate <= 0) {
            return "--";
        }
        if (bitRate >= 1000) {
            return String.format("%d kbps", bitRate / 1000);
        }
        return String.format("%d bps", bitRate);
    }

    public static String formatSampleRate(int sampleRate) {
        if (sampleRate <= 0) {
            return "--";
        }
        if (sampleRate >= 1000) {
            return String.format("%d kHz", sampleRate / 1000);
        }
        return String.format("%d Hz", sampleRate);
    }

    public static String formatChannels(int channels) {
        if (channels <= 0) {
            return "--";
        }
        switch (channels) {
            case 1:
                return "单声道";
            case 2:
                return "立体声";
            case 4:
                return "四声道";
            case 6:
                return "5.1 环绕";
            default:
                return String.format("%d 声道", channels);
        }
    }

    public static boolean isSupportedAudioFormat(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return SUPPORTED_FORMATS.contains(extension);
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}