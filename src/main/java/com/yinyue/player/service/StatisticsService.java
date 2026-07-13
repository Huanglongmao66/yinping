package com.yinyue.player.service;

import com.yinyue.player.model.HistoryEntry;
import com.yinyue.player.model.Song;
import com.yinyue.player.util.AudioUtils;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsService {
    private static StatisticsService instance;

    public static StatisticsService getInstance() {
        if (instance == null) instance = new StatisticsService();
        return instance;
    }

    private StatisticsService() {}

    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();
        LibraryService library = LibraryService.getInstance();
        HistoryService history = HistoryService.getInstance();

        List<Song> songs = library.getSongs();
        List<HistoryEntry> historyList = history.getHistory();

        stats.put("totalSongs", songs.size());
        stats.put("totalArtists", library.getAllArtists().size());
        stats.put("totalAlbums", library.getAllAlbums().size());
        stats.put("totalPlayCount", songs.stream().mapToLong(Song::getPlayCount).sum());
        stats.put("totalDuration", AudioUtils.formatTime(songs.stream().mapToLong(Song::getDuration).sum()));
        stats.put("historySize", historyList.size());

        long totalFileSize = songs.stream().mapToLong(Song::getFileSize).sum();
        stats.put("totalFileSize", AudioUtils.formatFileSize(totalFileSize));

        return stats;
    }

    public List<Map.Entry<String, Integer>> getTopArtists(int count) {
        Map<String, Integer> artistPlays = new HashMap<>();
        LibraryService library = LibraryService.getInstance();
        for (Song song : library.getSongs()) {
            String artist = song.getArtist() != null ? song.getArtist() : "未知";
            artistPlays.merge(artist, song.getPlayCount(), Integer::sum);
        }
        return artistPlays.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Integer>> getTopSongs(int count) {
        Map<String, Integer> songPlays = new HashMap<>();
        LibraryService library = LibraryService.getInstance();
        for (Song song : library.getSongs()) {
            songPlays.put(song.getDisplayTitle(), song.getPlayCount());
        }
        return songPlays.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getPlaysByFormat() {
        Map<String, Integer> result = new HashMap<>();
        for (Song song : LibraryService.getInstance().getSongs()) {
            String fmt = song.getFormat() != null ? song.getFormat().toUpperCase() : "未知";
            result.merge(fmt, 1, Integer::sum);
        }
        return result;
    }

    public Map<String, Integer> getPlaysByHour() {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            result.put(String.format("%02d:00", i), 0);
        }
        for (HistoryEntry entry : HistoryService.getInstance().getHistory()) {
            if (entry.getPlayedAt() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(entry.getPlayedAt());
                String hour = String.format("%02d:00", cal.get(Calendar.HOUR_OF_DAY));
                result.merge(hour, 1, Integer::sum);
            }
        }
        return result;
    }
}
