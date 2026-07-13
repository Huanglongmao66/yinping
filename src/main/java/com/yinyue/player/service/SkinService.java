package com.yinyue.player.service;

import com.yinyue.player.util.ThemeManager;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.*;

/**
 * 界面皮肤服务
 * 提供自定义皮肤、配色方案、界面布局管理
 */
public class SkinService {
    private static SkinService instance;
    
    private Map<String, SkinProfile> skins;
    private SkinProfile currentSkin;
    private String currentSkinName;
    private ThemeManager themeManager;
    
    public static SkinService getInstance() {
        if (instance == null) {
            instance = new SkinService();
        }
        return instance;
    }
    
    private SkinService() {
        skins = new HashMap<>();
        themeManager = ThemeManager.getInstance();
        
        // 初始化默认皮肤
        initDefaultSkins();
        currentSkin = skins.get("经典蓝");
        currentSkinName = "经典蓝";
    }
    
    private void initDefaultSkins() {
        // 经典蓝皮肤
        SkinProfile classicBlue = new SkinProfile("经典蓝");
        classicBlue.backgroundColor = Color.rgb(30, 40, 60);
        classicBlue.primaryColor = Color.rgb(50, 150, 220);
        classicBlue.secondaryColor = Color.rgb(100, 180, 255);
        classicBlue.textColor = Color.WHITE;
        classicBlue.accentColor = Color.rgb(255, 100, 150);
        classicBlue.fontSize = 14;
        classicBlue.fontFamily = "Microsoft YaHei";
        skins.put("经典蓝", classicBlue);
        
        // 暗黑皮肤
        SkinProfile darkMode = new SkinProfile("暗黑");
        darkMode.backgroundColor = Color.rgb(20, 20, 25);
        darkMode.primaryColor = Color.rgb(40, 40, 50);
        darkMode.secondaryColor = Color.rgb(60, 60, 70);
        darkMode.textColor = Color.rgb(200, 200, 200);
        darkMode.accentColor = Color.rgb(100, 200, 255);
        darkMode.fontSize = 14;
        darkMode.fontFamily = "Microsoft YaHei";
        skins.put("暗黑", darkMode);
        
        // 浪漫粉皮肤
        SkinProfile romanticPink = new SkinProfile("浪漫粉");
        romanticPink.backgroundColor = Color.rgb(45, 35, 50);
        romanticPink.primaryColor = Color.rgb(180, 100, 150);
        romanticPink.secondaryColor = Color.rgb(220, 150, 180);
        romanticPink.textColor = Color.WHITE;
        romanticPink.accentColor = Color.rgb(255, 200, 220);
        romanticPink.fontSize = 14;
        romanticPink.fontFamily = "Microsoft YaHei";
        skins.put("浪漫粉", romanticPink);
        
        // 清新绿皮肤
        SkinProfile freshGreen = new SkinProfile("清新绿");
        freshGreen.backgroundColor = Color.rgb(25, 45, 35);
        freshGreen.primaryColor = Color.rgb(80, 180, 100);
        freshGreen.secondaryColor = Color.rgb(120, 220, 140);
        freshGreen.textColor = Color.WHITE;
        freshGreen.accentColor = Color.rgb(200, 255, 150);
        freshGreen.fontSize = 14;
        freshGreen.fontFamily = "Microsoft YaHei";
        skins.put("清新绿", freshGreen);
        
        // 高对比皮肤
        SkinProfile highContrast = new SkinProfile("高对比");
        highContrast.backgroundColor = Color.BLACK;
        highContrast.primaryColor = Color.WHITE;
        highContrast.secondaryColor = Color.rgb(220, 220, 220);
        highContrast.textColor = Color.WHITE;
        highContrast.accentColor = Color.YELLOW;
        highContrast.fontSize = 16;
        highContrast.fontFamily = "Microsoft YaHei";
        skins.put("高对比", highContrast);
        
        // 简约皮肤
        SkinProfile minimal = new SkinProfile("简约");
        minimal.backgroundColor = Color.rgb(245, 245, 250);
        minimal.primaryColor = Color.rgb(50, 50, 60);
        minimal.secondaryColor = Color.rgb(100, 100, 110);
        minimal.textColor = Color.rgb(30, 30, 40);
        minimal.accentColor = Color.rgb(100, 150, 200);
        minimal.fontSize = 13;
        minimal.fontFamily = "Microsoft YaHei";
        skins.put("简约", minimal);
    }
    
    public void applySkin(Scene scene, String skinName) {
        SkinProfile skin = skins.get(skinName);
        if (skin == null || scene == null) return;
        
        currentSkin = skin;
        currentSkinName = skinName;
        
        // 应用皮肤样式
        String css = generateSkinCSS(skin);
        
        // 清除旧样式，应用新样式
        scene.getStylesheets().clear();
        scene.getStylesheets().add("data:text/css;base64," + 
            Base64.getEncoder().encodeToString(css.getBytes()));
        
        // 同时应用主题
        themeManager.applyTheme(scene, skinName);
    }
    
    private String generateSkinCSS(SkinProfile skin) {
        StringBuilder css = new StringBuilder();
        
        css.append(".root {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.backgroundColor) + ";\n");
        css.append("    -fx-text-fill: " + colorToCSS(skin.textColor) + ";\n");
        css.append("    -fx-font-size: " + skin.fontSize + "px;\n");
        css.append("    -fx-font-family: '" + skin.fontFamily + "';\n");
        css.append("}\n");
        
        css.append(".control-button {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.primaryColor) + ";\n");
        css.append("    -fx-text-fill: " + colorToCSS(skin.textColor) + ";\n");
        css.append("    -fx-background-radius: 5;\n");
        css.append("}\n");
        
        css.append(".control-button:hover {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.secondaryColor) + ";\n");
        css.append("}\n");
        
        css.append(".sidebar {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.primaryColor.darker()) + ";\n");
        css.append("}\n");
        
        css.append(".sidebar-title {\n");
        css.append("    -fx-text-fill: " + colorToCSS(skin.textColor) + ";\n");
        css.append("    -fx-font-weight: bold;\n");
        css.append("}\n");
        
        css.append(".player-bar {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.primaryColor) + ";\n");
        css.append("}\n");
        
        css.append(".song-title {\n");
        css.append("    -fx-text-fill: " + colorToCSS(skin.accentColor) + ";\n");
        css.append("    -fx-font-weight: bold;\n");
        css.append("}\n");
        
        css.append(".song-artist {\n");
        css.append("    -fx-text-fill: " + colorToCSS(skin.textColor) + ";\n");
        css.append("}\n");
        
        css.append(".progress-slider .track {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.secondaryColor) + ";\n");
        css.append("}\n");
        
        css.append(".progress-slider .thumb {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.accentColor) + ";\n");
        css.append("}\n");
        
        css.append(".volume-slider .track {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.secondaryColor) + ";\n");
        css.append("}\n");
        
        css.append(".volume-slider .thumb {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.accentColor) + ";\n");
        css.append("}\n");
        
        css.append(".menu-bar {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.primaryColor) + ";\n");
        css.append("}\n");
        
        css.append(".menu-item:focused {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.secondaryColor) + ";\n");
        css.append("}\n");
        
        css.append(".list-cell:focused {\n");
        css.append("    -fx-background-color: " + colorToCSS(skin.accentColor) + ";\n");
        css.append("}\n");
        
        css.append(".visualizer {\n");
        css.append("    -fx-background-color: transparent;\n");
        css.append("}\n");
        
        return css.toString();
    }
    
    private String colorToCSS(Color color) {
        return String.format("rgb(%d, %d, %d)", 
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    public List<String> getAvailableSkins() {
        return new ArrayList<>(skins.keySet());
    }
    
    public SkinProfile getCurrentSkin() {
        return currentSkin;
    }
    
    public String getCurrentSkinName() {
        return currentSkinName;
    }
    
    public SkinProfile getSkin(String name) {
        return skins.get(name);
    }
    
    // 创建自定义皮肤
    public void createCustomSkin(String name, SkinProfile skin) {
        skins.put(name, skin);
    }
    
    // 删除自定义皮肤
    public void deleteCustomSkin(String name) {
        // 不能删除默认皮肤
        if (name.equals("经典蓝") || name.equals("暗黑") || 
            name.equals("浪漫粉") || name.equals("清新绿") ||
            name.equals("高对比") || name.equals("简约")) {
            return;
        }
        skins.remove(name);
    }
    
    // 皮肤配置类
    public static class SkinProfile {
        public String name;
        public Color backgroundColor;
        public Color primaryColor;
        public Color secondaryColor;
        public Color textColor;
        public Color accentColor;
        public int fontSize;
        public String fontFamily;
        
        public SkinProfile(String name) {
            this.name = name;
            this.fontSize = 14;
            this.fontFamily = "Microsoft YaHei";
        }
        
        public SkinProfile copy() {
            SkinProfile copy = new SkinProfile(name);
            copy.backgroundColor = backgroundColor;
            copy.primaryColor = primaryColor;
            copy.secondaryColor = secondaryColor;
            copy.textColor = textColor;
            copy.accentColor = accentColor;
            copy.fontSize = fontSize;
            copy.fontFamily = fontFamily;
            return copy;
        }
    }
}