package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 收藏夹服务
 * 管理用户收藏的歌曲，支持分类和快速访问
 */
public class FavoritesService {
    private static FavoritesService instance;
    
    private Map<String, Song> favorites; // songId -> Song
    private Map<String, Set<String>> categories; // categoryName -> songIds
    private Map<String, Long> favoriteTime; // songId -> timestamp
    private Gson gson;
    private String dataFile;
    
    public static FavoritesService getInstance() {
        if (instance == null) {
            instance = new FavoritesService();
        }
        return instance;
    }
    
    private FavoritesService() {
        favorites = new ConcurrentHashMap<>();
        categories = new ConcurrentHashMap<>();
        favoriteTime = new ConcurrentHashMap<>();
        gson = new Gson();
        dataFile = System.getProperty("user.home") + "/.yinyue/favorites.json";
        
        // 默认分类
        categories.put("默认收藏", new HashSet<>());
        categories.put("最爱歌曲", new HashSet<>());
        categories.put("最近添加", new HashSet<>());
        
        loadFromFile();
    }
    
    public boolean addFavorite(Song song) {
        if (song == null || song.getId() == null) return false;
        
        favorites.put(song.getId(), song);
        favoriteTime.put(song.getId(), System.currentTimeMillis());
        categories.get("默认收藏").add(song.getId());
        
        saveToFile();
        return true;
    }
    
    public boolean removeFavorite(String songId) {
        if (songId == null) return false;
        
        favorites.remove(songId);
        favoriteTime.remove(songId);
        
        // 从所有分类中移除
        for (Set<String> catSongs : categories.values()) {
            catSongs.remove(songId);
        }
        
        saveToFile();
        return true;
    }
    
    public boolean isFavorite(String songId) {
        return favorites.containsKey(songId);
    }
    
    public Song getFavorite(String songId) {
        return favorites.get(songId);
    }
    
    public List<Song> getAllFavorites() {
        List<Song> result = new ArrayList<>(favorites.values());
        // 按收藏时间排序（最新的在前）
        result.sort((a, b) -> {
            Long ta = favoriteTime.getOrDefault(a.getId(), 0L);
            Long tb = favoriteTime.getOrDefault(b.getId(), 0L);
            return tb.compareTo(ta);
        });
        return result;
    }
    
    public List<Song> getFavoritesByCategory(String categoryName) {
        Set<String> songIds = categories.get(categoryName);
        if (songIds == null) return new ArrayList<>();
        
        List<Song> result = new ArrayList<>();
        for (String id : songIds) {
            Song song = favorites.get(id);
            if (song != null) result.add(song);
        }
        return result;
    }
    
    public void addToCategory(String songId, String categoryName) {
        if (!favorites.containsKey(songId)) return;
        
        Set<String> catSongs = categories.get(categoryName);
        if (catSongs == null) {
            catSongs = new HashSet<>();
            categories.put(categoryName, catSongs);
        }
        catSongs.add(songId);
        
        // 从"最近添加"分类移除旧条目，保持大小
        Set<String> recent = categories.get("最近添加");
        if (recent.size() > 50) {
            // 移除最旧的
            String oldest = null;
            long oldestTime = Long.MAX_VALUE;
            for (String id : recent) {
                Long time = favoriteTime.get(id);
                if (time != null && time < oldestTime) {
                    oldestTime = time;
                    oldest = id;
                }
            }
            if (oldest != null) recent.remove(oldest);
        }
        recent.add(songId);
        
        saveToFile();
    }
    
    public void removeFromCategory(String songId, String categoryName) {
        Set<String> catSongs = categories.get(categoryName);
        if (catSongs != null) {
            catSongs.remove(songId);
            saveToFile();
        }
    }
    
    public void createCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return;
        categories.putIfAbsent(categoryName, new HashSet<>());
        saveToFile();
    }
    
    public void deleteCategory(String categoryName) {
        if (categoryName.equals("默认收藏") || 
            categoryName.equals("最爱歌曲") || 
            categoryName.equals("最近添加")) {
            return; // 不能删除默认分类
        }
        categories.remove(categoryName);
        saveToFile();
    }
    
    public List<String> getCategories() {
        return new ArrayList<>(categories.keySet());
    }
    
    public int getFavoriteCount() {
        return favorites.size();
    }
    
    public int getCategoryCount(String categoryName) {
        Set<String> catSongs = categories.get(categoryName);
        return catSongs != null ? catSongs.size() : 0;
    }
    
    public void toggleFavorite(Song song) {
        if (isFavorite(song.getId())) {
            removeFavorite(song.getId());
        } else {
            addFavorite(song);
        }
    }
    
    public List<Song> getMostFavorites(int limit) {
        List<Song> all = getAllFavorites();
        if (all.size() <= limit) return all;
        return all.subList(0, limit);
    }
    
    public List<Song> getRecentlyFavorites(int limit) {
        List<Song> all = getAllFavorites();
        if (all.size() <= limit) return all;
        return all.subList(0, limit);
    }
    
    // 批量操作
    public void addFavoritesBatch(List<Song> songs) {
        for (Song song : songs) {
            addFavorite(song);
        }
    }
    
    public void removeFavoritesBatch(List<String> songIds) {
        for (String id : songIds) {
            removeFavorite(id);
        }
    }
    
    // 导入导出
    public void exportFavorites(String filePath) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("favorites", favorites);
            data.put("categories", categories);
            data.put("favoriteTime", favoriteTime);
            
            String json = gson.toJson(data);
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void importFavorites(String filePath) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(json, type);
            
            favorites.clear();
            categories.clear();
            favoriteTime.clear();
            
            // 解析数据
            // ... 简化处理，直接覆盖
            
            saveToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveToFile() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("favorites", favorites);
            data.put("categories", categories);
            data.put("favoriteTime", favoriteTime);
            
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
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(json, type);
            
            // 解析 favorites
            if (data.containsKey("favorites")) {
                Map<String, Object> favMap = (Map<String, Object>) data.get("favorites");
                for (Map.Entry<String, Object> entry : favMap.entrySet()) {
                    Song song = gson.fromJson(gson.toJson(entry.getValue()), Song.class);
                    favorites.put(entry.getKey(), song);
                }
            }
            
            // 解析 categories
            if (data.containsKey("categories")) {
                Map<String, Object> catMap = (Map<String, Object>) data.get("categories");
                for (Map.Entry<String, Object> entry : catMap.entrySet()) {
                    Set<String> ids = new HashSet<>();
                    if (entry.getValue() instanceof List) {
                        ids.addAll((List<String>) entry.getValue());
                    }
                    categories.put(entry.getKey(), ids);
                }
            }
            
            // 解析 favoriteTime
            if (data.containsKey("favoriteTime")) {
                Map<String, Object> timeMap = (Map<String, Object>) data.get("favoriteTime");
                for (Map.Entry<String, Object> entry : timeMap.entrySet()) {
                    favoriteTime.put(entry.getKey(), 
                                    entry.getValue() instanceof Number ? 
                                    ((Number) entry.getValue()).longValue() : 
                                    Long.parseLong(entry.getValue().toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}