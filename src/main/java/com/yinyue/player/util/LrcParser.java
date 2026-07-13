package com.yinyue.player.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcParser {
    private static final Pattern TIME_TAG_PATTERN = Pattern.compile("\\[([0-9]{2}):([0-9]{2})\\.([0-9]{2,3})\\]");
    private static final Pattern METADATA_PATTERN = Pattern.compile("\\[([a-zA-Z]+):(.+)\\]");

    private List<LyricLine> lyrics;
    private String title;
    private String artist;
    private String album;

    public LrcParser() {
        this.lyrics = new ArrayList<>();
    }

    public void parse(File file) {
        lyrics.clear();
        title = null;
        artist = null;
        album = null;

        if (file == null || !file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line);
            }
        } catch (Exception e) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parseLine(line);
                }
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    private void parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }

        Matcher metadataMatcher = METADATA_PATTERN.matcher(line);
        if (metadataMatcher.find()) {
            String key = metadataMatcher.group(1).toLowerCase();
            String value = metadataMatcher.group(2).trim();
            switch (key) {
                case "ti":
                    title = value;
                    break;
                case "ar":
                    artist = value;
                    break;
                case "al":
                    album = value;
                    break;
            }
            return;
        }

        Matcher timeMatcher = TIME_TAG_PATTERN.matcher(line);
        List<Long> times = new ArrayList<>();
        
        while (timeMatcher.find()) {
            int minutes = Integer.parseInt(timeMatcher.group(1));
            int seconds = Integer.parseInt(timeMatcher.group(2));
            int milliseconds = Integer.parseInt(timeMatcher.group(3));
            if (milliseconds < 10) {
                milliseconds *= 100;
            } else if (milliseconds < 100) {
                milliseconds *= 10;
            }
            times.add((long) (minutes * 60 * 1000 + seconds * 1000 + milliseconds));
        }

        if (times.isEmpty()) {
            return;
        }

        String text = TIME_TAG_PATTERN.matcher(line).replaceAll("").trim();
        if (text.isEmpty()) {
            return;
        }

        for (Long time : times) {
            lyrics.add(new LyricLine(time, text));
        }
    }

    public String getLyricAt(long time) {
        if (lyrics.isEmpty()) {
            return null;
        }

        int low = 0;
        int high = lyrics.size() - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (lyrics.get(mid).getTime() <= time) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        if (result >= 0) {
            return lyrics.get(result).getText();
        }
        return null;
    }

    public int getLyricIndexAt(long time) {
        if (lyrics.isEmpty()) {
            return -1;
        }

        int low = 0;
        int high = lyrics.size() - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (lyrics.get(mid).getTime() <= time) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result;
    }

    public List<LyricLine> getLyrics() {
        return lyrics;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public boolean hasLyrics() {
        return !lyrics.isEmpty();
    }

    public static class LyricLine {
        private final long time;
        private final String text;

        public LyricLine(long time, String text) {
            this.time = time;
            this.text = text;
        }

        public long getTime() {
            return time;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", AudioUtils.formatTime(time), text);
        }
    }
}