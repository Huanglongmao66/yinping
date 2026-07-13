package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

/**
 * 播放统计服务
 * 提供详细的播放数据分析和报告
 */
public class PlaybackStatisticsService {
    private static PlaybackStatisticsService instance;
    
    private Map<String, PlayStats> songStats; // songId -> stats
    private Map<String, AlbumStats> albumStats;
    private Map<String, ArtistStats> artistStats;
    private Map<String, DailyStats> dailyStats; // date -> stats
    
    private long totalPlayTime;
    private int totalPlayCount;
    private String mostPlayedDay;
    private String currentDay;
    
    private Gson gson;
    private String dataFile;
    
    public static PlaybackStatisticsService getInstance() {
        if (instance == null) {
            instance = new PlaybackStatisticsService();
        }
        return instance;
    }
    
    private PlaybackStatisticsService() {
        songStats = new HashMap<>();
        albumStats = new HashMap<>();
        artistStats = new HashMap<>();
        dailyStats = new HashMap<>();
        gson = new Gson();
        dataFile = System.getProperty("user.home") + "/.yinyue/playback_stats.json";
        currentDay = getTodayDate();
        loadFromFile();
    }
    
    private String getTodayDate() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return today.toString();
    }
    
    // 记录播放开始
    public void onPlayStart(Song song) {
        if (song == null || song.getId() == null) return;
        
        PlayStats ps = songStats.computeIfAbsent(song.getId(), k -> new PlayStats());
        ps.lastStartTime = System.currentTimeMillis();
        
        // 更新每日统计
        DailyStats ds = dailyStats.computeIfAbsent(currentDay, k -> new DailyStats());
        ds.playCount++;
        
        // 更新专辑统计
        if (song.getAlbum() != null) {
            AlbumStats as = albumStats.computeIfAbsent(song.getAlbum(), k -> new AlbumStats());
            as.playCount++;
        }
        
        // 更新艺术家统计
        if (song.getArtist() != null) {
            ArtistStats ars = artistStats.computeIfAbsent(song.getArtist(), k -> new ArtistStats());
            ars.playCount++;
        }
        
        saveToFile();
    }
    
    // 记录播放停止
    public void onPlayStop(Song song, long durationPlayed) {
        if (song == null || song.getId() == null) return;
        
        PlayStats ps = songStats.get(song.getId());
        if (ps != null) {
            ps.totalPlayCount++;
            ps.totalPlayTime += durationPlayed;
            ps.lastPlayTime = System.currentTimeMillis();
            
            // 计算完成百分比
            if (song.getDuration() > 0) {
                double percent = (durationPlayed * 100.0) / song.getDuration();
                if (percent >= 80) {
                    ps.completeCount++;
                }
            }
        }
        
        // 更新总统计
        totalPlayCount++;
        totalPlayTime += durationPlayed;
        
        // 更新每日统计
        DailyStats ds = dailyStats.get(currentDay);
        if (ds != null) {
            ds.totalPlayTime += durationPlayed;
            ds.uniqueSongs.add(song.getId());
        }
        
        // 更新专辑统计
        if (song.getAlbum() != null) {
            AlbumStats as = albumStats.get(song.getAlbum());
            if (as != null) {
                as.totalPlayTime += durationPlayed;
            }
        }
        
        // 更新艺术家统计
        if (song.getArtist() != null) {
            ArtistStats ars = artistStats.get(song.getArtist());
            if (ars != null) {
                ars.totalPlayTime += durationPlayed;
            }
        }
        
        saveToFile();
    }
    
    // 获取歌曲统计
    public PlayStats getSongStats(String songId) {
        return songStats.get(songId);
    }
    
    // 获取专辑统计
    public AlbumStats getAlbumStats(String albumName) {
        return albumStats.get(albumName);
    }
    
    // 获取艺术家统计
    public ArtistStats getArtistStats(String artistName) {
        return artistStats.get(artistName);
    }
    
    // 获取每日统计
    public DailyStats getDailyStats(String date) {
        return dailyStats.get(date);
    }
    
    // 获取今日统计
    public DailyStats getTodayStats() {
        return dailyStats.get(currentDay);
    }
    
    // 获取本周统计
    public WeeklyStats getWeeklyStats() {
        WeeklyStats ws = new WeeklyStats();
        java.time.LocalDate today = java.time.LocalDate.now();
        
        for (int i = 0; i < 7; i++) {
            String date = today.minusDays(i).toString();
            DailyStats ds = dailyStats.get(date);
            if (ds != null) {
                ws.totalPlayCount += ds.playCount;
                ws.totalPlayTime += ds.totalPlayTime;
                ws.daysWithPlay++;
            }
        }
        
        return ws;
    }
    
    // 获取本月统计
    public MonthlyStats getMonthlyStats() {
        MonthlyStats ms = new MonthlyStats();
        java.time.LocalDate today = java.time.LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        
        for (Map.Entry<String, DailyStats> entry : dailyStats.entrySet()) {
            String date = entry.getKey();
            java.time.LocalDate d = java.time.LocalDate.parse(date);
            if (d.getYear() == year && d.getMonthValue() == month) {
                ms.totalPlayCount += entry.getValue().playCount;
                ms.totalPlayTime += entry.getValue().totalPlayTime;
                ms.daysWithPlay++;
            }
        }
        
        return ms;
    }
    
    // 获取年度统计
    public YearlyStats getYearlyStats() {
        YearlyStats ys = new YearlyStats();
        java.time.LocalDate today = java.time.LocalDate.now();
        int year = today.getYear();
        
        for (Map.Entry<String, DailyStats> entry : dailyStats.entrySet()) {
            String date = entry.getKey();
            java.time.LocalDate d = java.time.LocalDate.parse(date);
            if (d.getYear() == year) {
                ys.totalPlayCount += entry.getValue().playCount;
                ys.totalPlayTime += entry.getValue().totalPlayTime;
                ys.monthsWithPlay++;
            }
        }
        
        return ys;
    }
    
    // 获取总统计
    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPlayCount", totalPlayCount);
        stats.put("totalPlayTime", formatDuration(totalPlayTime));
        stats.put("totalPlayTimeMs", totalPlayTime);
        stats.put("uniqueSongs", songStats.size());
        stats.put("uniqueAlbums", albumStats.size());
        stats.put("uniqueArtists", artistStats.size());
        
        // 最常播放歌曲
        String topSong = songStats.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.totalPlayCount, b.totalPlayCount)))
            .map(Map.Entry::getKey)
            .orElse("");
        stats.put("topSong", topSong);
        
        // 最常播放专辑
        String topAlbum = albumStats.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.playCount, b.playCount)))
            .map(Map.Entry::getKey)
            .orElse("");
        stats.put("topAlbum", topAlbum);
        
        // 最常播放艺术家
        String topArtist = artistStats.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.playCount, b.playCount)))
            .map(Map.Entry::getKey)
            .orElse("");
        stats.put("topArtist", topArtist);
        
        // 最活跃的一天
        String mostActiveDay = dailyStats.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.playCount, b.playCount)))
            .map(Map.Entry::getKey)
            .orElse("");
        stats.put("mostActiveDay", mostActiveDay);
        
        return stats;
    }
    
    // 获取播放趋势数据（最近30天）
    public List<DailyStats> getTrendData(int days) {
        List<DailyStats> trend = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            DailyStats ds = dailyStats.get(date);
            if (ds == null) {
                ds = new DailyStats();
                ds.date = date;
            }
            trend.add(ds);
        }
        
        return trend;
    }
    
    // 格式化时长
    public String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天 %d小时", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟", minutes);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    // 清除所有统计数据
    public void clearAllStats() {
        songStats.clear();
        albumStats.clear();
        artistStats.clear();
        dailyStats.clear();
        totalPlayTime = 0;
        totalPlayCount = 0;
        saveToFile();
    }
    
    // 导出统计数据
    public void exportStats(String filePath) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("songStats", songStats);
            data.put("albumStats", albumStats);
            data.put("artistStats", artistStats);
            data.put("dailyStats", dailyStats);
            data.put("totalPlayTime", totalPlayTime);
            data.put("totalPlayCount", totalPlayCount);
            
            String json = gson.toJson(data);
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 统计类定义
    public static class PlayStats {
        public int totalPlayCount;
        public long totalPlayTime;
        public int completeCount;
        public long lastPlayTime;
        public long lastStartTime;
    }
    
    public static class AlbumStats {
        public int playCount;
        public long totalPlayTime;
    }
    
    public static class ArtistStats {
        public int playCount;
        public long totalPlayTime;
    }
    
    public static class DailyStats {
        public String date;
        public int playCount;
        public long totalPlayTime;
        public Set<String> uniqueSongs = new HashSet<>();
        
        public int getUniqueSongCount() {
            return uniqueSongs.size();
        }
    }
    
    public static class WeeklyStats {
        public int totalPlayCount;
        public long totalPlayTime;
        public int daysWithPlay;
    }
    
    public static class MonthlyStats {
        public int totalPlayCount;
        public long totalPlayTime;
        public int daysWithPlay;
    }
    
    public static class YearlyStats {
        public int totalPlayCount;
        public long totalPlayTime;
        public int monthsWithPlay;
    }
    
    private void saveToFile() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("songStats", songStats);
            data.put("albumStats", albumStats);
            data.put("artistStats", artistStats);
            data.put("dailyStats", dailyStats);
            data.put("totalPlayTime", totalPlayTime);
            data.put("totalPlayCount", totalPlayCount);
            
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
            
            if (data.containsKey("totalPlayTime")) {
                totalPlayTime = ((Number) data.get("totalPlayTime")).longValue();
            }
            if (data.containsKey("totalPlayCount")) {
                totalPlayCount = ((Number) data.get("totalPlayCount")).intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}