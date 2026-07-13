package com.yinyue.player.service;

import com.yinyue.player.model.Song;

import java.util.*;
import java.util.stream.Collectors;

public class SmartLibraryService {
    private static SmartLibraryService instance;

    public static SmartLibraryService getInstance() {
        if (instance == null) instance = new SmartLibraryService();
        return instance;
    }

    private SmartLibraryService() {}

    public Map<String, List<Song>> groupByGenre(LibraryService library) {
        Map<String, List<Song>> result = new HashMap<>();
        for (Song song : library.getSongs()) {
            String genre = song.getFormat() != null ? song.getFormat().toUpperCase() : "未知";
            result.computeIfAbsent(genre, k -> new ArrayList<>()).add(song);
        }
        return result;
    }

    public Map<String, List<Song>> groupByFileSize(LibraryService library) {
        Map<String, List<Song>> result = new HashMap<>();
        for (Song song : library.getSongs()) {
            String category;
            long sizeMB = song.getFileSize() / (1024 * 1024);
            if (sizeMB < 5) category = "小型 (< 5MB)";
            else if (sizeMB < 20) category = "中型 (5-20MB)";
            else category = "大型 (> 20MB)";
            result.computeIfAbsent(category, k -> new ArrayList<>()).add(song);
        }
        return result;
    }

    public Map<String, List<Song>> groupByPlayCount(LibraryService library) {
        Map<String, List<Song>> result = new HashMap<>();
        for (Song song : library.getSongs()) {
            String category;
            int count = song.getPlayCount();
            if (count == 0) category = "从未播放";
            else if (count < 5) category = "偶尔播放 (1-4次)";
            else if (count < 20) category = "经常播放 (5-19次)";
            else category = "高频播放 (20+次)";
            result.computeIfAbsent(category, k -> new ArrayList<>()).add(song);
        }
        return result;
    }

    public Map<String, List<Song>> groupByDirectory(LibraryService library) {
        Map<String, List<Song>> result = new HashMap<>();
        for (Song song : library.getSongs()) {
            String path = song.getFilePath();
            if (path != null) {
                int lastSep = path.lastIndexOf(System.getProperty("file.separator"));
                String dir = lastSep > 0 ? path.substring(0, lastSep) : "根目录";
                result.computeIfAbsent(dir, k -> new ArrayList<>()).add(song);
            }
        }
        return result;
    }

    public List<Song> getRecentlyAdded(LibraryService library, int count) {
        return library.getSongs().stream()
                .sorted((a, b) -> {
                    if (a.getAddTime() == null) return 1;
                    if (b.getAddTime() == null) return -1;
                    return b.getAddTime().compareTo(a.getAddTime());
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    public List<Song> getMostPlayed(LibraryService library, int count) {
        return library.getSongs().stream()
                .sorted((a, b) -> Integer.compare(b.getPlayCount(), a.getPlayCount()))
                .limit(count)
                .collect(Collectors.toList());
    }

    public List<Song> getLongestSongs(LibraryService library, int count) {
        return library.getSongs().stream()
                .sorted((a, b) -> Long.compare(b.getDuration(), a.getDuration()))
                .limit(count)
                .collect(Collectors.toList());
    }
}
