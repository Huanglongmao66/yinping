package com.yinyue.player.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yinyue.player.model.HistoryEntry;
import com.yinyue.player.model.Song;
import com.yinyue.player.util.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryService {
    private static HistoryService instance;
    private final List<HistoryEntry> history;
    private final Gson gson;
    private static final String HISTORY_FILE = FileUtils.getConfigDirectory() + "/history.json";
    private static final int MAX_HISTORY_SIZE = 500;

    public static HistoryService getInstance() {
        if (instance == null) {
            instance = new HistoryService();
        }
        return instance;
    }

    private HistoryService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        history = loadHistory();
    }

    private List<HistoryEntry> loadHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<HistoryEntry>>() {}.getType();
                List<HistoryEntry> loaded = gson.fromJson(reader, listType);
                return loaded != null ? loaded : new ArrayList<>();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    public synchronized void saveHistory() {
        try (Writer writer = new FileWriter(HISTORY_FILE)) {
            gson.toJson(history, writer);
        } catch (Exception e) {
            // ignore
        }
    }

    public synchronized void addToHistory(Song song) {
        if (song == null) return;

        // Remove duplicate if exists
        history.removeIf(entry -> entry.getFilePath() != null && entry.getFilePath().equals(song.getFilePath()));

        HistoryEntry entry = new HistoryEntry(song);
        history.add(0, entry);

        if (history.size() > MAX_HISTORY_SIZE) {
            history.subList(MAX_HISTORY_SIZE, history.size()).clear();
        }
        saveHistory();
    }

    public synchronized void updatePlayDuration(Song song, long durationPlayed) {
        if (song == null) return;
        for (HistoryEntry entry : history) {
            if (entry.getFilePath() != null && entry.getFilePath().equals(song.getFilePath())) {
                entry.setDurationPlayed(durationPlayed);
                entry.setPlayTime(System.currentTimeMillis());
                saveHistory();
                return;
            }
        }
    }

    public List<HistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }

    public List<HistoryEntry> getRecentHistory(int count) {
        int end = Math.min(count, history.size());
        return new ArrayList<>(history.subList(0, end));
    }

    public void clearHistory() {
        history.clear();
        saveHistory();
    }

    public void removeFromHistory(String filePath) {
        history.removeIf(entry -> entry.getFilePath() != null && entry.getFilePath().equals(filePath));
        saveHistory();
    }
}
