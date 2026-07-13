package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

/**
 * 音频书签服务
 * 管理音频文件的章节标记和书签
 */
public class AudioBookmarkService {
    private static AudioBookmarkService instance;
    
    private Map<String, List<Bookmark>> bookmarks; // songId -> bookmarks
    private Map<String, List<Chapter>> chapters; // songId -> chapters
    private Gson gson;
    private String dataFile;
    
    public static AudioBookmarkService getInstance() {
        if (instance == null) {
            instance = new AudioBookmarkService();
        }
        return instance;
    }
    
    private AudioBookmarkService() {
        bookmarks = new HashMap<>();
        chapters = new HashMap<>();
        gson = new Gson();
        dataFile = System.getProperty("user.home") + "/.yinyue/bookmarks.json";
        loadFromFile();
    }
    
    // 添加书签
    public void addBookmark(String songId, long timeMs, String label) {
        if (songId == null) return;
        
        List<Bookmark> songBookmarks = bookmarks.computeIfAbsent(songId, k -> new ArrayList<>());
        
        // 检查是否已存在相同时间的书签
        for (Bookmark b : songBookmarks) {
            if (Math.abs(b.time - timeMs) < 500) {
                // 更新标签
                b.label = label;
                saveToFile();
                return;
            }
        }
        
        songBookmarks.add(new Bookmark(timeMs, label));
        // 按时间排序
        songBookmarks.sort(Comparator.comparingLong(b -> b.time));
        
        saveToFile();
    }
    
    // 删除书签
    public void removeBookmark(String songId, long timeMs) {
        List<Bookmark> songBookmarks = bookmarks.get(songId);
        if (songBookmarks != null) {
            songBookmarks.removeIf(b -> Math.abs(b.time - timeMs) < 500);
            saveToFile();
        }
    }
    
    // 获取歌曲的书签列表
    public List<Bookmark> getBookmarks(String songId) {
        List<Bookmark> songBookmarks = bookmarks.get(songId);
        return songBookmarks != null ? new ArrayList<>(songBookmarks) : new ArrayList<>();
    }
    
    // 获取书签数量
    public int getBookmarkCount(String songId) {
        List<Bookmark> songBookmarks = bookmarks.get(songId);
        return songBookmarks != null ? songBookmarks.size() : 0;
    }
    
    // 跳转到书签
    public long goToBookmark(String songId, int index) {
        List<Bookmark> songBookmarks = bookmarks.get(songId);
        if (songBookmarks != null && index >= 0 && index < songBookmarks.size()) {
            return songBookmarks.get(index).time;
        }
        return -1;
    }
    
    // 清除所有书签
    public void clearBookmarks(String songId) {
        bookmarks.remove(songId);
        saveToFile();
    }
    
    // 添加章节
    public void addChapter(String songId, long startMs, long endMs, String title) {
        if (songId == null) return;
        
        List<Chapter> songChapters = chapters.computeIfAbsent(songId, k -> new ArrayList<>());
        songChapters.add(new Chapter(startMs, endMs, title));
        songChapters.sort(Comparator.comparingLong(c -> c.start));
        
        saveToFile();
    }
    
    // 删除章节
    public void removeChapter(String songId, int index) {
        List<Chapter> songChapters = chapters.get(songId);
        if (songChapters != null && index >= 0 && index < songChapters.size()) {
            songChapters.remove(index);
            saveToFile();
        }
    }
    
    // 获取歌曲的章节列表
    public List<Chapter> getChapters(String songId) {
        List<Chapter> songChapters = chapters.get(songId);
        return songChapters != null ? new ArrayList<>(songChapters) : new ArrayList<>();
    }
    
    // 获取章节数量
    public int getChapterCount(String songId) {
        List<Chapter> songChapters = chapters.get(songId);
        return songChapters != null ? songChapters.size() : 0;
    }
    
    // 获取当前时间所在的章节
    public Chapter getCurrentChapter(String songId, long currentTime) {
        List<Chapter> songChapters = chapters.get(songId);
        if (songChapters == null) return null;
        
        for (Chapter chapter : songChapters) {
            if (currentTime >= chapter.start && currentTime < chapter.end) {
                return chapter;
            }
        }
        
        return null;
    }
    
    // 获取章节索引
    public int getChapterIndex(String songId, long currentTime) {
        List<Chapter> songChapters = chapters.get(songId);
        if (songChapters == null) return -1;
        
        for (int i = 0; i < songChapters.size(); i++) {
            Chapter chapter = songChapters.get(i);
            if (currentTime >= chapter.start && currentTime < chapter.end) {
                return i;
            }
        }
        
        return -1;
    }
    
    // 跳转到章节
    public long goToChapter(String songId, int index) {
        List<Chapter> songChapters = chapters.get(songId);
        if (songChapters != null && index >= 0 && index < songChapters.size()) {
            return songChapters.get(index).start;
        }
        return -1;
    }
    
    // 清除所有章节
    public void clearChapters(String songId) {
        chapters.remove(songId);
        saveToFile();
    }
    
    // 自动生成章节（基于静音检测）
    public void autoGenerateChapters(String songId, Song song) {
        if (songId == null || song == null) return;
        
        List<Chapter> newChapters = new ArrayList<>();
        long duration = song.getDuration();
        
        // 简单的等间隔分割
        int chapterCount = 5;
        long interval = duration / chapterCount;
        
        for (int i = 0; i < chapterCount; i++) {
            long start = i * interval;
            long end = (i + 1) * interval;
            String title = "章节 " + (i + 1);
            newChapters.add(new Chapter(start, end, title));
        }
        
        chapters.put(songId, newChapters);
        saveToFile();
    }
    
    // 导出书签
    public void exportBookmarks(String songId, String filePath) {
        List<Bookmark> songBookmarks = bookmarks.get(songId);
        if (songBookmarks == null) return;
        
        try {
            String json = gson.toJson(songBookmarks);
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 导入书签
    public void importBookmarks(String songId, String filePath) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            List<Bookmark> loaded = gson.fromJson(json, new TypeToken<List<Bookmark>>(){}.getType());
            if (loaded != null) {
                bookmarks.put(songId, loaded);
                saveToFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 获取所有有书签的歌曲ID
    public List<String> getSongsWithBookmarks() {
        return new ArrayList<>(bookmarks.keySet());
    }
    
    // 获取所有有章节的歌曲ID
    public List<String> getSongsWithChapters() {
        return new ArrayList<>(chapters.keySet());
    }
    
    // 删除歌曲的所有数据
    public void removeSongData(String songId) {
        bookmarks.remove(songId);
        chapters.remove(songId);
        saveToFile();
    }
    
    // 书签类
    public static class Bookmark {
        public long time;
        public String label;
        public long createTime;
        
        public Bookmark() {}
        
        public Bookmark(long time, String label) {
            this.time = time;
            this.label = label;
            this.createTime = System.currentTimeMillis();
        }
        
        public String getTimeDisplay() {
            long seconds = time / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            
            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            }
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    // 章节类
    public static class Chapter {
        public long start;
        public long end;
        public String title;
        
        public Chapter() {}
        
        public Chapter(long start, long end, String title) {
            this.start = start;
            this.end = end;
            this.title = title;
        }
        
        public String getStartTimeDisplay() {
            return formatTime(start);
        }
        
        public String getEndTimeDisplay() {
            return formatTime(end);
        }
        
        public String getDurationDisplay() {
            return formatTime(end - start);
        }
        
        private String formatTime(long time) {
            long seconds = time / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            
            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            }
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    private void saveToFile() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("bookmarks", bookmarks);
            data.put("chapters", chapters);
            
            File file = new File(dataFile);
            file.getParentFile().mkdirs();
            
            String json = gson.toJson(data);
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadFromFile() {
        try {
            File file = new File(dataFile);
            if (!file.exists()) return;
            
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            Map<String, Object> data = gson.fromJson(json, Map.class);
            
            if (data.containsKey("bookmarks")) {
                Map<String, List<Bookmark>> bookmarkMap = 
                    gson.fromJson(gson.toJson(data.get("bookmarks")), 
                                 new TypeToken<Map<String, List<Bookmark>>>(){}.getType());
                if (bookmarkMap != null) {
                    bookmarks.putAll(bookmarkMap);
                }
            }
            
            if (data.containsKey("chapters")) {
                Map<String, List<Chapter>> chapterMap = 
                    gson.fromJson(gson.toJson(data.get("chapters")), 
                                 new TypeToken<Map<String, List<Chapter>>>(){}.getType());
                if (chapterMap != null) {
                    chapters.putAll(chapterMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}