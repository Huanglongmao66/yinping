package com.yinyue.player.service;

import com.yinyue.player.model.HistoryEntry;
import com.yinyue.player.model.Song;

import java.util.*;
import java.util.stream.Collectors;

public class RecommendationEngine {
    private static RecommendationEngine instance;

    public static RecommendationEngine getInstance() {
        if (instance == null) instance = new RecommendationEngine();
        return instance;
    }

    private RecommendationEngine() {}

    public List<Song> recommend(LibraryService library, HistoryService history, int count) {
        List<HistoryEntry> recentHistory = history.getRecentHistory(20);
        if (recentHistory.isEmpty()) {
            return library.getSongs().stream().limit(count).collect(Collectors.toList());
        }

        // Analyze listening patterns
        Map<String, Integer> artistWeights = new HashMap<>();
        Map<String, Integer> formatWeights = new HashMap<>();

        for (HistoryEntry entry : recentHistory) {
            if (entry.getArtist() != null) {
                artistWeights.merge(entry.getArtist(), 1, Integer::sum);
            }
        }

        // Find current song to get format preference
        Song current = AudioPlayerService.getInstance().getCurrentSong();
        if (current != null && current.getFormat() != null) {
            formatWeights.put(current.getFormat().toLowerCase(), 5);
        }

        // Score all songs
        List<Song> allSongs = new ArrayList<>(library.getSongs());
        List<SongScore> scores = new ArrayList<>();
        Set<String> historyPaths = recentHistory.stream()
                .map(HistoryEntry::getFilePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Song song : allSongs) {
            if (historyPaths.contains(song.getFilePath())) continue; // Skip recently played

            double score = 0;
            // Artist match
            if (song.getArtist() != null && artistWeights.containsKey(song.getArtist())) {
                score += artistWeights.get(song.getArtist()) * 3.0;
            }
            // Format match
            if (song.getFormat() != null && formatWeights.containsKey(song.getFormat().toLowerCase())) {
                score += formatWeights.get(song.getFormat().toLowerCase());
            }
            // Play count bonus
            score += song.getPlayCount() * 0.5;
            // Random factor for discovery
            score += Math.random() * 2;

            scores.add(new SongScore(song, score));
        }

        scores.sort((a, b) -> Double.compare(b.score, a.score));
        return scores.stream().limit(count).map(s -> s.song).collect(Collectors.toList());
    }

    public List<Song> discoverNew(LibraryService library, HistoryService history, int count) {
        Set<String> playedPaths = history.getHistory().stream()
                .map(HistoryEntry::getFilePath)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Song> unplayed = library.getSongs().stream()
                .filter(s -> !playedPaths.contains(s.getFilePath()))
                .collect(Collectors.toList());

        Collections.shuffle(unplayed);
        return unplayed.stream().limit(count).collect(Collectors.toList());
    }

    private static class SongScore {
        Song song;
        double score;
        SongScore(Song song, double score) {
            this.song = song;
            this.score = score;
        }
    }
}
