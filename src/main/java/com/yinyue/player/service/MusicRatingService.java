package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

/**
 * 音乐评分服务
 * 管理用户评分、标签和收藏偏好
 */
public class MusicRatingService {
    private static MusicRatingService instance;
    
    private Map<String, SongRating> ratings; // songId -> rating
    private Map<String, List<String>> tags; // songId -> tags
    private Map<String, Integer> playCounts; // songId -> count
    private Map<String, Long> lastPlayTime; // songId -> timestamp
    
    private Gson gson;
    private String dataFile;
    
    public static MusicRatingService getInstance() {
        if (instance == null) {
            instance = new MusicRatingService();
        }
        return instance;
    }
    
    private MusicRatingService() {
        ratings = new HashMap<>();
        tags = new HashMap<>();
        playCounts = new HashMap<>();
        lastPlayTime = new HashMap<>();
        gson = new Gson();
        dataFile = System.getProperty("user.home") + "/.yinyue/ratings.json";
        loadFromFile();
    }
    
    // 设置评分 (1-5星)
    public void setRating(String songId, int rating) {
        rating = Math.max(1, Math.min(5, rating));
        
        SongRating sr = ratings.get(songId);
        if (sr == null) {
            sr = new SongRating();
            ratings.put(songId, sr);
        }
        sr.rating = rating;
        saveToFile();
    }
    
    // 获取评分
    public int getRating(String songId) {
        SongRating sr = ratings.get(songId);
        return sr != null ? sr.rating : 0;
    }
    
    // 添加标签
    public void addTag(String songId, String tag) {
        if (tag == null || tag.isEmpty()) return;
        
        List<String> songTags = tags.computeIfAbsent(songId, k -> new ArrayList<>());
        if (!songTags.contains(tag)) {
            songTags.add(tag);
            saveToFile();
        }
    }
    
    // 批量添加标签
    public void addTags(String songId, List<String> tagList) {
        for (String tag : tagList) {
            addTag(songId, tag);
        }
    }
    
    // 移除标签
    public void removeTag(String songId, String tag) {
        List<String> songTags = tags.get(songId);
        if (songTags != null) {
            songTags.remove(tag);
            saveToFile();
        }
    }
    
    // 获取标签
    public List<String> getTags(String songId) {
        List<String> songTags = tags.get(songId);
        return songTags != null ? new ArrayList<>(songTags) : new ArrayList<>();
    }
    
    // 获取所有标签
    public Set<String> getAllTags() {
        Set<String> allTags = new HashSet<>();
        for (List<String> tagList : tags.values()) {
            allTags.addAll(tagList);
        }
        return allTags;
    }
    
    // 获取带指定标签的歌曲
    public List<String> getSongsByTag(String tag) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
            if (entry.getValue().contains(tag)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    // 增加播放次数
    public void incrementPlayCount(String songId) {
        playCounts.merge(songId, 1, Integer::sum);
        lastPlayTime.put(songId, System.currentTimeMillis());
        saveToFile();
    }
    
    // 获取播放次数
    public int getPlayCount(String songId) {
        return playCounts.getOrDefault(songId, 0);
    }
    
    // 获取最后播放时间
    public long getLastPlayTime(String songId) {
        return lastPlayTime.getOrDefault(songId, 0L);
    }
    
    // 获取评分最高的歌曲
    public List<String> getTopRatedSongs(int limit) {
        List<String> songIds = new ArrayList<>(ratings.keySet());
        songIds.sort((a, b) -> {
            int ra = ratings.get(a).rating;
            int rb = ratings.get(b).rating;
            return Integer.compare(rb, ra);
        });
        
        if (songIds.size() > limit) {
            return songIds.subList(0, limit);
        }
        return songIds;
    }
    
    // 获取最常播放的歌曲
    public List<String> getMostPlayedSongs(int limit) {
        List<String> songIds = new ArrayList<>(playCounts.keySet());
        songIds.sort((a, b) -> {
            int ca = playCounts.get(a);
            int cb = playCounts.get(b);
            return Integer.compare(cb, ca);
        });
        
        if (songIds.size() > limit) {
            return songIds.subList(0, limit);
        }
        return songIds;
    }
    
    // 获取最近播放的歌曲
    public List<String> getRecentlyPlayedSongs(int limit) {
        List<String> songIds = new ArrayList<>(lastPlayTime.keySet());
        songIds.sort((a, b) -> {
            long ta = lastPlayTime.get(a);
            long tb = lastPlayTime.get(b);
            return Long.compare(tb, ta);
        });
        
        if (songIds.size() > limit) {
            return songIds.subList(0, limit);
        }
        return songIds;
    }
    
    // 获取歌曲综合评分
    public double getCompositeScore(String songId) {
        SongRating sr = ratings.get(songId);
        int rating = sr != null ? sr.rating : 0;
        int playCount = playCounts.getOrDefault(songId, 0);
        
        // 综合评分 = 评分 * 0.5 + 播放次数分数 * 0.5
        double ratingScore = rating / 5.0;
        double playScore = Math.min(playCount / 100.0, 1.0);
        
        return ratingScore * 0.5 + playScore * 0.5;
    }
    
    // 获取带标签的歌曲数量
    public int getSongsWithTagCount(String tag) {
        return getSongsByTag(tag).size();
    }
    
    // 删除歌曲的所有评分数据
    public void removeSongData(String songId) {
        ratings.remove(songId);
        tags.remove(songId);
        playCounts.remove(songId);
        lastPlayTime.remove(songId);
        saveToFile();
    }
    
    // 导出评分数据
    public void exportData(String filePath) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("ratings", ratings);
            data.put("tags", tags);
            data.put("playCounts", playCounts);
            data.put("lastPlayTime", lastPlayTime);
            
            String json = gson.toJson(data);
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 导入评分数据
    public void importData(String filePath) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            Map<String, Object> data = gson.fromJson(json, Map.class);
            
            // 合并数据
            if (data.containsKey("ratings")) {
                // 简化处理
            }
            saveToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 统计信息
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRated", ratings.size());
        stats.put("totalTagged", tags.size());
        stats.put("totalPlayed", playCounts.size());
        
        // 平均评分
        double avgRating = ratings.values().stream()
            .mapToInt(r -> r.rating)
            .average()
            .orElse(0);
        stats.put("averageRating", avgRating);
        
        // 最常用标签
        Map<String, Integer> tagCounts = new HashMap<>();
        for (List<String> tagList : tags.values()) {
            for (String tag : tagList) {
                tagCounts.merge(tag, 1, Integer::sum);
            }
        }
        String topTag = tagCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("无");
        stats.put("topTag", topTag);
        
        return stats;
    }
    
    // 歌曲评分类
    private static class SongRating {
        int rating;
        
        SongRating() {
            rating = 0;
        }
    }
    
    private void saveToFile() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("ratings", ratings);
            data.put("tags", tags);
            data.put("playCounts", playCounts);
            data.put("lastPlayTime", lastPlayTime);
            
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
            
            if (data.containsKey("ratings")) {
                Map<String, Integer> ratingMap = (Map<String, Integer>) data.get("ratings");
                for (Map.Entry<String, Integer> entry : ratingMap.entrySet()) {
                    SongRating sr = new SongRating();
                    sr.rating = entry.getValue();
                    ratings.put(entry.getKey(), sr);
                }
            }
            
            if (data.containsKey("tags")) {
                Map<String, List<String>> tagMap = (Map<String, List<String>>) data.get("tags");
                tags.putAll(tagMap);
            }
            
            if (data.containsKey("playCounts")) {
                Map<String, Integer> playMap = (Map<String, Integer>) data.get("playCounts");
                playCounts.putAll(playMap);
            }
            
            if (data.containsKey("lastPlayTime")) {
                Map<String, Long> timeMap = (Map<String, Long>) data.get("lastPlayTime");
                lastPlayTime.putAll(timeMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}