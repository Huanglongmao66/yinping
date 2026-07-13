package com.yinyue.player.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yinyue.player.util.ConfigManager;
import com.yinyue.player.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CloudSyncService {
    private static CloudSyncService instance;
    private final Gson gson;
    private static final String SYNC_DIR = FileUtils.getConfigDirectory() + "/sync";

    public static CloudSyncService getInstance() {
        if (instance == null) {
            instance = new CloudSyncService();
        }
        return instance;
    }

    private CloudSyncService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        new File(SYNC_DIR).mkdirs();
    }

    public boolean exportConfig(String exportPath) {
        try {
            SyncData data = new SyncData();
            data.config = readFileAsString(FileUtils.getConfigFilePath());
            data.history = readFileAsString(FileUtils.getConfigDirectory() + "/history.json");
            data.playlists = readFileAsString(FileUtils.getPlaylistsDirectory() + "/playlists.json");

            try (Writer writer = new FileWriter(exportPath)) {
                gson.toJson(data, writer);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean importConfig(String importPath) {
        try (Reader reader = new FileReader(importPath)) {
            SyncData data = gson.fromJson(reader, SyncData.class);
            if (data == null) return false;

            if (data.config != null) {
                writeStringToFile(FileUtils.getConfigFilePath(), data.config);
            }
            if (data.history != null) {
                writeStringToFile(FileUtils.getConfigDirectory() + "/history.json", data.history);
            }
            if (data.playlists != null) {
                writeStringToFile(FileUtils.getPlaylistsDirectory() + "/playlists.json", data.playlists);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean backupAll(String backupDir) {
        try {
            File dir = new File(backupDir);
            dir.mkdirs();
            copyFile(FileUtils.getConfigFilePath(), backupDir + "/config.json");
            copyFile(FileUtils.getConfigDirectory() + "/history.json", backupDir + "/history.json");
            copyFile(FileUtils.getPlaylistsDirectory() + "/playlists.json", backupDir + "/playlists.json");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String readFileAsString(String path) {
        try {
            return new String(Files.readAllBytes(new File(path).toPath()));
        } catch (Exception e) {
            return null;
        }
    }

    private void writeStringToFile(String path, String content) {
        try (Writer writer = new FileWriter(path)) {
            writer.write(content);
        } catch (Exception e) {
            // ignore
        }
    }

    private void copyFile(String source, String dest) throws IOException {
        File src = new File(source);
        if (src.exists()) {
            Files.copy(src.toPath(), new File(dest).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static class SyncData {
        String config;
        String history;
        String playlists;
    }
}
