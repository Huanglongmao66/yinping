package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.model.HistoryEntry;
import java.util.*;

/**
 * 音乐发现服务
 * 基于相似度推荐新音乐
 */
public class MusicDiscoveryService {
    private static MusicDiscoveryService instance;
    
    private LibraryService libraryService;
    private MusicRatingService ratingService;
    private HistoryService historyService;
    
    public static MusicDiscoveryService getInstance() {
        if (instance == null) {
            instance = new MusicDiscoveryService();
        }
        return instance;
    }
    
    private MusicDiscoveryService() {
        libraryService = LibraryService.getInstance();
        ratingService = MusicRatingService.getInstance();
        historyService = HistoryService.getInstance();
    }
    
    // 基于当前歌曲推荐相似歌曲
    public List<Song> recommendSimilar(Song targetSong, int count) {
        if (targetSong == null) return new ArrayList<>();
        
        List<Song> allSongs = libraryService.getSongs();
        List<ScoredSong> scored = new ArrayList<>();
        
        for (Song song : allSongs) {
            if (song.getId().equals(targetSong.getId())) continue;
            
            double score = calculateSimilarity(targetSong, song);
            if (score > 0.3) {
                scored.add(new ScoredSong(song, score));
            }
        }
        
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        
        List<Song> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, scored.size()); i++) {
            result.add(scored.get(i).song);
        }
        
        return result;
    }
    
    // 基于播放历史推荐
    public List<Song> recommendBasedOnHistory(int count) {
        List<Song> history = getRecentlyPlayedSongs(20);
        if (history.isEmpty()) return getRandomSongs(count);
        
        // 分析最常播放的艺术家和类型
        Map<String, Integer> artistCount = new HashMap<>();
        Map<String, Integer> genreCount = new HashMap<>();
        
        for (Song song : history) {
            if (song.getArtist() != null) {
                artistCount.merge(song.getArtist(), 1, Integer::sum);
            }
        }
        
        // 根据偏好推荐
        List<Song> allSongs = libraryService.getSongs();
        List<ScoredSong> scored = new ArrayList<>();
        
        for (Song song : allSongs) {
            if (isInHistory(song)) continue;
            
            double score = 0;
            
            // 艺术家匹配
            if (song.getArtist() != null && artistCount.containsKey(song.getArtist())) {
                score += artistCount.get(song.getArtist()) * 10;
            }
            
            // 评分加成
            int rating = ratingService.getRating(song.getId());
            score += rating * 3;
            
            if (score > 0) {
                scored.add(new ScoredSong(song, score));
            }
        }
        
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        
        List<Song> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, scored.size()); i++) {
            result.add(scored.get(i).song);
        }
        
        // 如果推荐不够，补充随机歌曲
        if (result.size() < count) {
            List<Song> random = getRandomSongs(count - result.size());
            for (Song s : random) {
                if (!result.contains(s)) {
                    result.add(s);
                }
            }
        }
        
        return result;
    }
    
    // 获取随机歌曲
    public List<Song> getRandomSongs(int count) {
        List<Song> allSongs = libraryService.getSongs();
        List<Song> result = new ArrayList<>(allSongs);
        Collections.shuffle(result);
        
        if (result.size() > count) {
            return result.subList(0, count);
        }
        return result;
    }
    
    // 发现新音乐（从未播放的歌曲）
    public List<Song> discoverNewMusic(int count) {
        List<Song> allSongs = libraryService.getSongs();
        List<Song> neverPlayed = new ArrayList<>();
        
        for (Song song : allSongs) {
            if (ratingService.getPlayCount(song.getId()) == 0) {
                neverPlayed.add(song);
            }
        }
        
        Collections.shuffle(neverPlayed);
        
        if (neverPlayed.size() > count) {
            return neverPlayed.subList(0, count);
        }
        return neverPlayed;
    }
    
    // 每日推荐
    public List<Song> getDailyRecommendation(int count) {
        // 基于评分和播放历史混合推荐
        List<Song> allSongs = libraryService.getSongs();
        List<ScoredSong> scored = new ArrayList<>();
        
        for (Song song : allSongs) {
            double score = 0;
            
            // 评分权重
            int rating = ratingService.getRating(song.getId());
            score += rating * 15;
            
            // 播放次数权重（越少播放越优先推荐）
            int playCount = ratingService.getPlayCount(song.getId());
            score += Math.max(0, 50 - playCount * 2);
            
            // 随机因素
            score += Math.random() * 20;
            
            scored.add(new ScoredSong(song, score));
        }
        
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        
        List<Song> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, scored.size()); i++) {
            result.add(scored.get(i).song);
        }
        
        return result;
    }
    
    // 根据艺术家推荐
    public List<Song> recommendByArtist(String artist, int count) {
        List<Song> result = new ArrayList<>();
        List<Song> allSongs = libraryService.getSongs();
        
        for (Song song : allSongs) {
            if (artist.equals(song.getArtist())) {
                result.add(song);
            }
        }
        
        // 添加相似艺术家
        for (Song song : allSongs) {
            if (!artist.equals(song.getArtist()) && song.getArtist() != null) {
                if (isSimilarArtist(artist, song.getArtist())) {
                    result.add(song);
                }
            }
        }
        
        if (result.size() > count) {
            return result.subList(0, count);
        }
        return result;
    }
    
    // 根据类型推荐
    public List<Song> recommendByGenre(String genre, int count) {
        // 简化实现，返回随机歌曲
        return getRandomSongs(count);
    }
    
    // 计算两首歌曲的相似度
    private double calculateSimilarity(Song song1, Song song2) {
        double score = 0;
        double totalWeight = 0;
        
        // 艺术家相似度
        if (song1.getArtist() != null && song2.getArtist() != null) {
            if (song1.getArtist().equals(song2.getArtist())) {
                score += 40;
            } else if (isSimilarArtist(song1.getArtist(), song2.getArtist())) {
                score += 15;
            }
            totalWeight += 40;
        }
        
        // 专辑相似度
        if (song1.getAlbum() != null && song2.getAlbum() != null) {
            if (song1.getAlbum().equals(song2.getAlbum())) {
                score += 30;
            }
            totalWeight += 30;
        }
        
        // 时长相似度
        if (song1.getDuration() > 0 && song2.getDuration() > 0) {
            double durationDiff = Math.abs(song1.getDuration() - song2.getDuration());
            double durationScore = Math.max(0, 10 - durationDiff / 30000.0);
            score += durationScore;
            totalWeight += 10;
        }
        
        return totalWeight > 0 ? score / totalWeight : 0;
    }
    
    private boolean isSimilarArtist(String artist1, String artist2) {
        // 简单的字符串相似度检查
        String a1 = artist1.toLowerCase();
        String a2 = artist2.toLowerCase();
        
        return a1.contains(a2) || a2.contains(a1) || 
               calculateLevenshteinDistance(a1, a2) <= 3;
    }
    
    // 编辑距离
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    private List<Song> getRecentlyPlayedSongs(int count) {
        List<Song> result = new ArrayList<>();
        List<HistoryEntry> history = historyService.getRecentHistory(count);
        
        for (HistoryEntry entry : history) {
            for (Song song : libraryService.getSongs()) {
                if (song.getId().equals(entry.getSongId())) {
                    result.add(song);
                    break;
                }
            }
        }
        
        return result;
    }
    
    private boolean isInHistory(Song song) {
        List<HistoryEntry> history = historyService.getRecentHistory(100);
        for (HistoryEntry entry : history) {
            if (entry.getSongId().equals(song.getId())) {
                return true;
            }
        }
        return false;
    }
    
    // 评分歌曲类
    private static class ScoredSong {
        Song song;
        double score;
        
        ScoredSong(Song song, double score) {
            this.song = song;
            this.score = score;
        }
    }
}