package com.yinyue.player.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private static final String CONFIG_FILE = FileUtils.getConfigFilePath();
    
    private final Gson gson;
    private ConfigData configData;

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private ConfigManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        FileUtils.ensureDirectoryExists(FileUtils.getConfigDirectory());
        load();
    }

    private void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                configData = gson.fromJson(reader, ConfigData.class);
            } catch (Exception e) {
                configData = new ConfigData();
            }
        } else {
            configData = new ConfigData();
            save();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(configData, writer);
        } catch (Exception e) {
            // ignore
        }
    }

    public double getVolume() {
        return configData.player.volume;
    }

    public void setVolume(double volume) {
        configData.player.volume = volume;
        save();
    }

    public boolean isMute() {
        return configData.player.mute;
    }

    public void setMute(boolean mute) {
        configData.player.mute = mute;
        save();
    }

    public String getPlayMode() {
        return configData.player.playMode;
    }

    public void setPlayMode(String playMode) {
        configData.player.playMode = playMode;
        save();
    }

    public boolean isAutoPlay() {
        return configData.player.autoPlay;
    }

    public void setAutoPlay(boolean autoPlay) {
        configData.player.autoPlay = autoPlay;
        save();
    }

    public String getTheme() {
        return configData.ui.theme;
    }

    public void setTheme(String theme) {
        configData.ui.theme = theme;
        save();
    }

    public String getLanguage() {
        return configData.ui.language;
    }

    public void setLanguage(String language) {
        configData.ui.language = language;
        save();
    }

    public int getWindowWidth() {
        return configData.ui.windowWidth;
    }

    public void setWindowWidth(int width) {
        configData.ui.windowWidth = width;
        save();
    }

    public int getWindowHeight() {
        return configData.ui.windowHeight;
    }

    public void setWindowHeight(int height) {
        configData.ui.windowHeight = height;
        save();
    }

    public boolean isShowLyrics() {
        return configData.ui.showLyrics;
    }

    public void setShowLyrics(boolean showLyrics) {
        configData.ui.showLyrics = showLyrics;
        save();
    }

    public boolean isShowVisualization() {
        return configData.ui.showVisualizer;
    }

    public void setShowVisualization(boolean showVisualizer) {
        configData.ui.showVisualizer = showVisualizer;
        save();
    }

    public List<String> getScanPaths() {
        return configData.library.scanPaths;
    }

    public void setScanPaths(List<String> scanPaths) {
        configData.library.scanPaths = scanPaths;
        save();
    }

    public boolean isAutoScan() {
        return configData.library.autoScan;
    }

    public void setAutoScan(boolean autoScan) {
        configData.library.autoScan = autoScan;
        save();
    }

    public boolean isEqualizerEnabled() {
        return configData.equalizer.enabled;
    }

    public void setEqualizerEnabled(boolean enabled) {
        configData.equalizer.enabled = enabled;
        save();
    }

    public float[] getEqualizerBands() {
        return configData.equalizer.bands;
    }

    public void setEqualizerBands(float[] bands) {
        configData.equalizer.bands = bands;
        save();
    }

    public Map<String, String> getShortcuts() {
        return configData.shortcuts;
    }

    public void setShortcuts(Map<String, String> shortcuts) {
        configData.shortcuts = shortcuts;
        save();
    }

    private static class ConfigData {
        PlayerConfig player = new PlayerConfig();
        UiConfig ui = new UiConfig();
        LibraryConfig library = new LibraryConfig();
        EqualizerConfig equalizer = new EqualizerConfig();
        Map<String, String> shortcuts = new HashMap<>();

        static class PlayerConfig {
            double volume = 0.7;
            boolean mute = false;
            String playMode = "REPEAT_ALL";
            boolean autoPlay = true;
            boolean crossfade = false;
            int crossfadeDuration = 3000;
        }

        static class UiConfig {
            String theme = "dark";
            String language = "zh_CN";
            int windowWidth = 1200;
            int windowHeight = 800;
            boolean showLyrics = true;
            boolean showVisualizer = true;
        }

        static class LibraryConfig {
            List<String> scanPaths = new ArrayList<>();
            boolean autoScan = true;
        }

        static class EqualizerConfig {
            boolean enabled = false;
            float[] bands = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        }
    }
}