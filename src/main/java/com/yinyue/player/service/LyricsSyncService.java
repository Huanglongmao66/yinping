package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.LrcParser;
import java.util.*;

/**
 * 歌词同步服务
 * 提供精确时间标签同步、歌词滚动和高亮
 */
public class LyricsSyncService {
    private static LyricsSyncService instance;
    
    private List<LyricLine> lyrics;
    private int currentLineIndex;
    private long currentTime;
    private String currentSongId;
    private boolean isSyncEnabled;
    
    private LyricsSyncCallback callback;
    
    public static LyricsSyncService getInstance() {
        if (instance == null) {
            instance = new LyricsSyncService();
        }
        return instance;
    }
    
    private LyricsSyncService() {
        lyrics = new ArrayList<>();
        currentLineIndex = -1;
        isSyncEnabled = true;
    }
    
    // 加载歌词
    public void loadLyrics(Song song) {
        if (song == null) {
            clearLyrics();
            return;
        }
        
        currentSongId = song.getId();
        lyrics = parseLyricsFromFile(song.getFilePath());
            
            if (lyrics.isEmpty()) {
                String lrcPath = getLrcFilePath(song.getFilePath());
                java.io.File lrcFile = new java.io.File(lrcPath);
                if (lrcFile.exists()) {
                    try {
                        java.util.List<String> lines = java.nio.file.Files.readAllLines(lrcFile.toPath());
                        for (String line : lines) {
                            line = line.trim();
                            if (line.startsWith("[") && line.contains("]")) {
                                int endBracket = line.indexOf("]");
                                String timeStr = line.substring(1, endBracket);
                                String text = line.substring(endBracket + 1);
                                long time = parseTime(timeStr);
                                if (time >= 0) {
                                    lyrics.add(new LyricLine(time, text));
                                }
                            }
                        }
                        java.util.Collections.sort(lyrics, (a, b) -> Long.compare(a.time, b.time));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        
        currentLineIndex = -1;
        
        if (callback != null) {
            callback.onLyricsLoaded(lyrics);
        }
    }
    
    // 解析歌词文件
    private List<LyricLine> parseLyricsFromFile(String filePath) {
        List<LyricLine> result = new ArrayList<>();
        
        if (filePath == null) return result;
        
        String lrcPath = filePath.replaceAll("\\.[^.]+$", ".lrc");
        try {
            java.io.File file = new java.io.File(lrcPath);
            if (file.exists()) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("[") && line.contains("]")) {
                        int endBracket = line.indexOf("]");
                        String timeStr = line.substring(1, endBracket);
                        String text = line.substring(endBracket + 1);
                        long time = parseTime(timeStr);
                        if (time >= 0) {
                            result.add(new LyricLine(time, text));
                        }
                    }
                }
                java.util.Collections.sort(result, (a, b) -> Long.compare(a.time, b.time));
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    private String getLrcFilePath(String filePath) {
        if (filePath == null) return null;
        return filePath.replaceAll("\\.[^.]+$", ".lrc");
    }
    
    private long parseTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int minutes = Integer.parseInt(parts[0]);
            String[] secondsParts = parts[1].split("\\.");
            int seconds = Integer.parseInt(secondsParts[0]);
            int milliseconds = secondsParts.length > 1 ? Integer.parseInt(secondsParts[1]) * 10 : 0;
            return minutes * 60000 + seconds * 1000 + milliseconds;
        } catch (Exception e) {
            return -1;
        }
    }
    
    // 更新播放时间
    public void updateTime(long timeMs) {
        if (!isSyncEnabled || lyrics.isEmpty()) return;
        
        currentTime = timeMs;
        
        // 查找当前歌词行
        int newIndex = findCurrentLine(timeMs);
        if (newIndex != currentLineIndex) {
            currentLineIndex = newIndex;
            if (callback != null) {
                callback.onLineChanged(currentLineIndex);
            }
        }
    }
    
    // 查找当前歌词行
    private int findCurrentLine(long timeMs) {
        for (int i = lyrics.size() - 1; i >= 0; i--) {
            if (lyrics.get(i).getTime() <= timeMs) {
                return i;
            }
        }
        return -1;
    }
    
    // 获取当前歌词行
    public LyricLine getCurrentLine() {
        if (currentLineIndex >= 0 && currentLineIndex < lyrics.size()) {
            return lyrics.get(currentLineIndex);
        }
        return null;
    }
    
    // 获取指定行歌词
    public LyricLine getLine(int index) {
        if (index >= 0 && index < lyrics.size()) {
            return lyrics.get(index);
        }
        return null;
    }
    
    // 获取歌词列表
    public List<LyricLine> getLyrics() {
        return new ArrayList<>(lyrics);
    }
    
    // 获取歌词行数
    public int getLyricsCount() {
        return lyrics.size();
    }
    
    // 获取当前行索引
    public int getCurrentLineIndex() {
        return currentLineIndex;
    }
    
    // 清除歌词
    public void clearLyrics() {
        lyrics.clear();
        currentLineIndex = -1;
        currentSongId = null;
    }
    
    // 启用/禁用同步
    public void setSyncEnabled(boolean enabled) {
        this.isSyncEnabled = enabled;
    }
    
    public boolean isSyncEnabled() {
        return isSyncEnabled;
    }
    
    // 设置回调
    public void setCallback(LyricsSyncCallback callback) {
        this.callback = callback;
    }
    
    // 手动跳转歌词
    public void jumpToLine(int index) {
        if (index >= 0 && index < lyrics.size()) {
            currentLineIndex = index;
            if (callback != null) {
                callback.onLineChanged(index);
            }
        }
    }
    
    // 搜索歌词
    public int searchLyrics(String keyword) {
        if (keyword == null || keyword.isEmpty()) return -1;
        
        keyword = keyword.toLowerCase();
        for (int i = 0; i < lyrics.size(); i++) {
            if (lyrics.get(i).getText().toLowerCase().contains(keyword)) {
                return i;
            }
        }
        return -1;
    }
    
    // 获取进度百分比(当前行在整首歌中的位置)
    public double getProgressPercentage() {
        if (lyrics.isEmpty() || currentLineIndex < 0) return 0;
        return (currentLineIndex + 1.0) / lyrics.size() * 100;
    }
    
    // 导出歌词
    public String exportLyrics() {
        StringBuilder sb = new StringBuilder();
        for (LyricLine line : lyrics) {
            sb.append(line.toLrcFormat()).append("\n");
        }
        return sb.toString();
    }
    
    // 歌词行类
    public static class LyricLine {
        private long time; // 毫秒
        private String text;
        private String originalText;
        
        public LyricLine(long time, String text) {
            this.time = time;
            this.text = text;
            this.originalText = text;
        }
        
        public long getTime() { return time; }
        public String getText() { return text; }
        public String getOriginalText() { return originalText; }
        
        public void setTime(long time) { this.time = time; }
        public void setText(String text) { 
            this.text = text;
            this.originalText = text;
        }
        
        public String toLrcFormat() {
            int minutes = (int)(time / 60000);
            int seconds = (int)((time % 60000) / 1000);
            int milliseconds = (int)(time % 1000);
            return String.format("[%02d:%02d.%02d]%s", minutes, seconds, milliseconds / 10, text);
        }
        
        public String getTimeDisplay() {
            int minutes = (int)(time / 60000);
            int seconds = (int)((time % 60000) / 1000);
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    // 歌词同步回调接口
    public interface LyricsSyncCallback {
        void onLyricsLoaded(List<LyricLine> lyrics);
        void onLineChanged(int lineIndex);
    }
}