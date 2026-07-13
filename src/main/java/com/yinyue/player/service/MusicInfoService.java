package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 音乐信息获取服务
 * 在线查询音乐信息、封面、歌词等
 */
public class MusicInfoService {
    private static MusicInfoService instance;
    
    private Gson gson;
    private String lyricsCacheDir;
    private String coverCacheDir;
    
    public static MusicInfoService getInstance() {
        if (instance == null) {
            instance = new MusicInfoService();
        }
        return instance;
    }
    
    private MusicInfoService() {
        gson = new Gson();
        lyricsCacheDir = System.getProperty("user.home") + "/.yinyue/cache/lyrics";
        coverCacheDir = System.getProperty("user.home") + "/.yinyue/cache/covers";
        
        // 创建缓存目录
        new File(lyricsCacheDir).mkdirs();
        new File(coverCacheDir).mkdirs();
    }
    
    // 搜索歌词
    public String searchLyrics(String title, String artist) {
        if (title == null || title.isEmpty()) return null;
        
        // 检查缓存
        String cacheKey = generateCacheKey(title, artist);
        String cachedLyrics = loadFromCache(lyricsCacheDir, cacheKey);
        if (cachedLyrics != null) {
            return cachedLyrics;
        }
        
        // 在线搜索（简化模拟）
        String lyrics = fetchLyricsOnline(title, artist);
        
        // 保存到缓存
        if (lyrics != null) {
            saveToCache(lyricsCacheDir, cacheKey, lyrics);
        }
        
        return lyrics;
    }
    
    // 搜索封面
    public byte[] searchCover(String title, String artist, String album) {
        if (title == null || title.isEmpty()) return null;
        
        String cacheKey = generateCacheKey(title, artist, album);
        byte[] cachedCover = loadBinaryFromCache(coverCacheDir, cacheKey + ".jpg");
        if (cachedCover != null) {
            return cachedCover;
        }
        
        byte[] cover = fetchCoverOnline(title, artist, album);
        
        if (cover != null) {
            saveBinaryToCache(coverCacheDir, cacheKey + ".jpg", cover);
        }
        
        return cover;
    }
    
    // 获取歌曲详细信息
    public SongInfo getSongInfo(String title, String artist) {
        SongInfo info = new SongInfo();
        info.title = title;
        info.artist = artist;
        
        // 模拟在线查询
        info.genre = fetchGenre(title, artist);
        info.album = fetchAlbum(title, artist);
        info.year = fetchYear(title, artist);
        info.duration = fetchDuration(title, artist);
        info.trackNumber = fetchTrackNumber(title, artist);
        
        return info;
    }
    
    // 获取专辑信息
    public AlbumInfo getAlbumInfo(String album, String artist) {
        AlbumInfo info = new AlbumInfo();
        info.name = album;
        info.artist = artist;
        
        // 模拟查询
        info.year = fetchYear(null, artist);
        info.trackCount = 10; // 默认值
        
        return info;
    }
    
    // 获取艺术家信息
    public ArtistInfo getArtistInfo(String artist) {
        ArtistInfo info = new ArtistInfo();
        info.name = artist;
        
        // 模拟查询
        info.genres = fetchArtistGenres(artist);
        info.albumCount = 5; // 默认值
        
        return info;
    }
    
    // 批量获取信息
    public List<SongInfo> batchGetSongInfo(List<Song> songs) {
        List<SongInfo> results = new ArrayList<>();
        for (Song song : songs) {
            SongInfo info = getSongInfo(song.getTitle(), song.getArtist());
            results.add(info);
        }
        return results;
    }
    
    // 更新歌曲信息
    public void updateSongInfo(Song song) {
        SongInfo info = getSongInfo(song.getTitle(), song.getArtist());
        
        if (info.album != null) song.setAlbum(info.album);
        if (info.duration > 0) song.setDuration(info.duration);
    }
    
    // 清除缓存
    public void clearCache() {
        deleteDirectory(new File(lyricsCacheDir));
        deleteDirectory(new File(coverCacheDir));
        
        // 重建目录
        new File(lyricsCacheDir).mkdirs();
        new File(coverCacheDir).mkdirs();
    }
    
    // 获取缓存大小
    public long getCacheSize() {
        return getDirectorySize(new File(lyricsCacheDir)) + 
               getDirectorySize(new File(coverCacheDir));
    }
    
    // 生成缓存键
    private String generateCacheKey(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part.toLowerCase().replaceAll("\\s+", ""));
            }
        }
        return sb.toString().hashCode() + "";
    }
    
    // 从缓存加载
    private String loadFromCache(String dir, String key) {
        try {
            File file = new File(dir, key + ".txt");
            if (file.exists()) {
                return new String(java.nio.file.Files.readAllBytes(file.toPath()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // 保存到缓存
    private void saveToCache(String dir, String key, String content) {
        try {
            File file = new File(dir, key + ".txt");
            java.nio.file.Files.write(file.toPath(), content.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 从缓存加载二进制数据
    private byte[] loadBinaryFromCache(String dir, String key) {
        try {
            File file = new File(dir, key);
            if (file.exists()) {
                return java.nio.file.Files.readAllBytes(file.toPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    // 保存二进制数据到缓存
    private void saveBinaryToCache(String dir, String key, byte[] data) {
        try {
            File file = new File(dir, key);
            java.nio.file.Files.write(file.toPath(), data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 模拟在线获取歌词
    private String fetchLyricsOnline(String title, String artist) {
        // 模拟返回歌词
        return generateMockLyrics(title);
    }
    
    // 模拟在线获取封面
    private byte[] fetchCoverOnline(String title, String artist, String album) {
        // 模拟返回封面数据（空数据）
        return new byte[0];
    }
    
    // 模拟获取类型
    private String fetchGenre(String title, String artist) {
        return "流行";
    }
    
    // 模拟获取专辑
    private String fetchAlbum(String title, String artist) {
        return "未知专辑";
    }
    
    // 模拟获取年份
    private int fetchYear(String title, String artist) {
        return 2024;
    }
    
    // 模拟获取时长
    private long fetchDuration(String title, String artist) {
        return 0;
    }
    
    // 模拟获取曲目编号
    private int fetchTrackNumber(String title, String artist) {
        return 0;
    }
    
    // 模拟获取艺术家类型
    private List<String> fetchArtistGenres(String artist) {
        return Arrays.asList("流行", "摇滚");
    }
    
    // 生成模拟歌词
    private String generateMockLyrics(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("[00:00.00]").append(title).append("\n");
        sb.append("[00:05.00]作词：佚名\n");
        sb.append("[00:10.00]作曲：佚名\n");
        sb.append("[00:15.00]\n");
        sb.append("[00:20.00]这是一首美妙的歌曲\n");
        sb.append("[00:25.00]让我们一起聆听\n");
        sb.append("[00:30.00]感受音乐的魅力\n");
        sb.append("[00:35.00]享受这美好的时刻\n");
        sb.append("[00:40.00]\n");
        sb.append("[00:45.00]歌词正在加载中...\n");
        sb.append("[00:50.00]请稍候...\n");
        return sb.toString();
    }
    
    // 删除目录
    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        dir.delete();
    }
    
    // 获取目录大小
    private long getDirectorySize(File dir) {
        if (!dir.exists()) return 0;
        
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                size += file.length();
            }
        }
        return size;
    }
    
    // 歌曲信息类
    public static class SongInfo {
        public String title;
        public String artist;
        public String album;
        public String genre;
        public int year;
        public long duration;
        public int trackNumber;
    }
    
    // 专辑信息类
    public static class AlbumInfo {
        public String name;
        public String artist;
        public int year;
        public int trackCount;
    }
    
    // 艺术家信息类
    public static class ArtistInfo {
        public String name;
        public List<String> genres;
        public int albumCount;
    }
}