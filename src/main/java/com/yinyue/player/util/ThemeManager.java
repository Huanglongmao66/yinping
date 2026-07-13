package com.yinyue.player.util;

import javafx.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private static ThemeManager instance;
    private String currentTheme = "dark";
    private final Map<String, String> themes = new HashMap<>();

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private ThemeManager() {
        themes.put("dark", "/css/dark_theme.css");
        themes.put("light", "/css/light_theme.css");
        themes.put("blue", "/css/blue_theme.css");
        themes.put("purple", "/css/purple_theme.css");
        themes.put("green", "/css/green_theme.css");
    }

    public void applyTheme(Scene scene, String themeName) {
        if (!themes.containsKey(themeName)) {
            themeName = "dark";
        }
        scene.getStylesheets().clear();
        String cssPath = getClass().getResource(themes.get(themeName)).toExternalForm();
        scene.getStylesheets().add(cssPath);
        currentTheme = themeName;
        ConfigManager.getInstance().setTheme(themeName);
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public Map<String, String> getAvailableThemes() {
        return new HashMap<>(themes);
    }

    public String getThemeDisplayName(String themeName) {
        switch (themeName) {
            case "dark": return "暗色主题";
            case "light": return "亮色主题";
            case "blue": return "蓝色主题";
            case "purple": return "紫色主题";
            case "green": return "绿色主题";
            default: return themeName;
        }
    }
}
