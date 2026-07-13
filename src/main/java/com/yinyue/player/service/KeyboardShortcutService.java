package com.yinyue.player.service;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

/**
 * 键盘快捷键配置服务
 * 管理自定义快捷键设置
 */
public class KeyboardShortcutService {
    private static KeyboardShortcutService instance;
    
    private Map<String, Shortcut> shortcuts;
    private Map<String, Runnable> actions;
    private Scene scene;
    private Gson gson;
    private String configFile;
    
    public static KeyboardShortcutService getInstance() {
        if (instance == null) {
            instance = new KeyboardShortcutService();
        }
        return instance;
    }
    
    private KeyboardShortcutService() {
        shortcuts = new HashMap<>();
        actions = new HashMap<>();
        gson = new Gson();
        configFile = System.getProperty("user.home") + "/.yinyue/shortcuts.json";
        
        initDefaultShortcuts();
        loadConfig();
    }
    
    private void initDefaultShortcuts() {
        shortcuts.put("play_pause", new Shortcut("播放/暂停", KeyCode.SPACE, false, false, false));
        shortcuts.put("stop", new Shortcut("停止", KeyCode.S, false, false, false));
        shortcuts.put("previous", new Shortcut("上一曲", KeyCode.LEFT, true, false, false));
        shortcuts.put("next", new Shortcut("下一曲", KeyCode.RIGHT, true, false, false));
        shortcuts.put("volume_up", new Shortcut("音量增加", KeyCode.UP, true, false, false));
        shortcuts.put("volume_down", new Shortcut("音量减少", KeyCode.DOWN, true, false, false));
        shortcuts.put("mute", new Shortcut("静音", KeyCode.M, false, false, false));
        shortcuts.put("toggle_lyrics", new Shortcut("显示/隐藏歌词", KeyCode.L, true, false, false));
        shortcuts.put("toggle_minimal", new Shortcut("迷你模式", KeyCode.F, true, false, false));
        shortcuts.put("fullscreen", new Shortcut("全屏", KeyCode.F11, false, false, false));
        shortcuts.put("equalizer", new Shortcut("均衡器", KeyCode.E, true, false, false));
        shortcuts.put("settings", new Shortcut("设置", KeyCode.COMMA, true, false, false));
        shortcuts.put("search", new Shortcut("搜索", KeyCode.F, true, true, false));
        shortcuts.put("add_favorite", new Shortcut("添加到收藏", KeyCode.F, false, true, false));
        shortcuts.put("show_history", new Shortcut("播放历史", KeyCode.H, true, false, false));
        shortcuts.put("show_playlists", new Shortcut("播放列表", KeyCode.P, true, false, false));
    }
    
    // 初始化场景监听
    public void initialize(Scene scene) {
        this.scene = scene;
        
        scene.setOnKeyPressed(event -> {
            handleKeyEvent(event);
        });
    }
    
    // 处理按键事件
    private void handleKeyEvent(KeyEvent event) {
        KeyCode code = event.getCode();
        boolean ctrl = event.isControlDown();
        boolean alt = event.isAltDown();
        boolean shift = event.isShiftDown();
        
        // 查找匹配的快捷键
        for (Map.Entry<String, Shortcut> entry : shortcuts.entrySet()) {
            Shortcut shortcut = entry.getValue();
            if (shortcut.matches(code, ctrl, alt, shift)) {
                event.consume();
                executeAction(entry.getKey());
                break;
            }
        }
    }
    
    // 执行动作
    public void executeAction(String actionId) {
        Runnable action = actions.get(actionId);
        if (action != null) {
            action.run();
        }
    }
    
    // 注册动作
    public void registerAction(String actionId, Runnable action) {
        actions.put(actionId, action);
    }
    
    // 取消注册动作
    public void unregisterAction(String actionId) {
        actions.remove(actionId);
    }
    
    // 获取所有快捷键
    public Map<String, Shortcut> getAllShortcuts() {
        return new HashMap<>(shortcuts);
    }
    
    // 获取指定快捷键
    public Shortcut getShortcut(String actionId) {
        return shortcuts.get(actionId);
    }
    
    // 设置快捷键
    public boolean setShortcut(String actionId, Shortcut shortcut) {
        // 检查是否已被占用
        for (Map.Entry<String, Shortcut> entry : shortcuts.entrySet()) {
            if (!entry.getKey().equals(actionId) && 
                entry.getValue().matches(shortcut.keyCode, shortcut.ctrl, shortcut.alt, shortcut.shift)) {
                return false; // 已被占用
            }
        }
        
        shortcuts.put(actionId, shortcut);
        saveConfig();
        return true;
    }
    
    // 获取可用的按键列表
    public List<String> getAvailableKeyCodes() {
        List<String> codes = new ArrayList<>();
        for (KeyCode code : KeyCode.values()) {
            codes.add(code.getName());
        }
        return codes;
    }
    
    // 重置为默认快捷键
    public void resetToDefaults() {
        shortcuts.clear();
        initDefaultShortcuts();
        saveConfig();
    }
    
    // 导出配置
    public void exportConfig(String filePath) {
        try {
            String json = gson.toJson(shortcuts);
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 导入配置
    public void importConfig(String filePath) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            Map<String, Shortcut> loaded = gson.fromJson(json, new TypeToken<Map<String, Shortcut>>(){}.getType());
            if (loaded != null) {
                shortcuts.putAll(loaded);
                saveConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 获取快捷键显示文本
    public String getShortcutDisplay(Shortcut shortcut) {
        if (shortcut == null) return "未设置";
        
        StringBuilder sb = new StringBuilder();
        if (shortcut.ctrl) sb.append("Ctrl+");
        if (shortcut.alt) sb.append("Alt+");
        if (shortcut.shift) sb.append("Shift+");
        sb.append(shortcut.keyCode.getName());
        
        return sb.toString();
    }
    
    // 快捷键类
    public static class Shortcut {
        public String name;
        public KeyCode keyCode;
        public boolean ctrl;
        public boolean alt;
        public boolean shift;
        
        public Shortcut() {}
        
        public Shortcut(String name, KeyCode keyCode, boolean ctrl, boolean alt, boolean shift) {
            this.name = name;
            this.keyCode = keyCode;
            this.ctrl = ctrl;
            this.alt = alt;
            this.shift = shift;
        }
        
        public boolean matches(KeyCode code, boolean ctrl, boolean alt, boolean shift) {
            return this.keyCode == code && 
                   this.ctrl == ctrl && 
                   this.alt == alt && 
                   this.shift == shift;
        }
        
        public String getName() { return name; }
        public KeyCode getKeyCode() { return keyCode; }
        public boolean isCtrl() { return ctrl; }
        public boolean isAlt() { return alt; }
        public boolean isShift() { return shift; }
    }
    
    private void saveConfig() {
        try {
            String json = gson.toJson(shortcuts);
            File file = new File(configFile);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadConfig() {
        try {
            File file = new File(configFile);
            if (!file.exists()) return;
            
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            Map<String, Shortcut> loaded = gson.fromJson(json, new TypeToken<Map<String, Shortcut>>(){}.getType());
            if (loaded != null) {
                shortcuts.putAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}