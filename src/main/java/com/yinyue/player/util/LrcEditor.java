package com.yinyue.player.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LrcEditor {
    private List<LrcLine> lines = new ArrayList<>();
    private String filePath;

    public static class LrcLine {
        public long timeMs;
        public String text;
        public LrcLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    public void load(String path) {
        this.filePath = path;
        lines.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[") && !line.startsWith("[ti:") && !line.startsWith("[ar:")
                        && !line.startsWith("[al:") && !line.startsWith("[by:")) {
                    parseLine(line);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void parseLine(String line) {
        int closeBracket = line.indexOf("]");
        if (closeBracket > 0) {
            String timeStr = line.substring(1, closeBracket);
            String text = line.substring(closeBracket + 1).trim();
            long timeMs = parseTime(timeStr);
            lines.add(new LrcLine(timeMs, text));
        }
    }

    private long parseTime(String time) {
        try {
            String[] parts = time.split("[:]");
            int min = Integer.parseInt(parts[0]);
            String[] secParts = parts[1].split("[.]");
            int sec = Integer.parseInt(secParts[0]);
            int ms = secParts.length > 1 ? Integer.parseInt(secParts[1]) * 10 : 0;
            return min * 60000 + sec * 1000 + ms;
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatTime(long ms) {
        int min = (int) (ms / 60000);
        int sec = (int) ((ms % 60000) / 1000);
        int cs = (int) ((ms % 1000) / 10);
        return String.format("%02d:%02d.%02d", min, sec, cs);
    }

    public void addLine(long timeMs, String text) {
        lines.add(new LrcLine(timeMs, text));
        sortByTime();
    }

    public void updateLine(int index, long timeMs, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.get(index).timeMs = timeMs;
            lines.get(index).text = text;
            sortByTime();
        }
    }

    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
        }
    }

    private void sortByTime() {
        lines.sort((a, b) -> Long.compare(a.timeMs, b.timeMs));
    }

    public void save() {
        saveAs(filePath);
    }

    public void saveAs(String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("[ti:Edited by YinYue Player]\n");
            for (LrcLine line : lines) {
                writer.write("[" + formatTime(line.timeMs) + "]" + line.text + "\n");
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public List<LrcLine> getLines() { return lines; }
}