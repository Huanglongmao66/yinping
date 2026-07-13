package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.model.Playlist;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级搜索服务
 * 提供模糊搜索、高级筛选、相似歌曲查找等功能
 */
public class SearchService {
    private static SearchService instance;
    
    private LibraryService libraryService;
    private PlaylistService playlistService;
    private HistoryService historyService;
    private FavoritesService favoritesService;
    
    public static SearchService getInstance() {
        if (instance == null) {
            instance = new SearchService();
        }
        return instance;
    }
    
    private SearchService() {
        libraryService = LibraryService.getInstance();
        playlistService = PlaylistService.getInstance();
        historyService = HistoryService.getInstance();
        favoritesService = FavoritesService.getInstance();
    }
    
    // 基础搜索
    public List<Song> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return libraryService.getSongs();
        }
        return libraryService.searchSongs(keyword);
    }
    
    // 模糊搜索（容错）
    public List<Song> fuzzySearch(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return libraryService.getSongs();
        }
        
        final String kw = keyword.toLowerCase();
        List<Song> results = new ArrayList<>();
        
        for (Song song : libraryService.getSongs()) {
            String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
            String artist = song.getArtist() != null ? song.getArtist().toLowerCase() : "";
            String album = song.getAlbum() != null ? song.getAlbum().toLowerCase() : "";
            
            // 检查是否包含关键词（允许部分匹配）
            if (fuzzyMatch(kw, title) || 
                fuzzyMatch(kw, artist) || 
                fuzzyMatch(kw, album)) {
                results.add(song);
            }
        }
        
        // 按匹配度排序
        results.sort((a, b) -> {
            double scoreA = calculateMatchScore(kw, a);
            double scoreB = calculateMatchScore(kw, b);
            return Double.compare(scoreB, scoreA);
        });
        
        return results;
    }
    
    private boolean fuzzyMatch(String keyword, String target) {
        // 简单模糊匹配：允许1个字符差异
        if (target.contains(keyword)) return true;
        
        // 检查关键词的每个字符是否在目标中
        int matches = 0;
        for (char c : keyword.toCharArray()) {
            if (target.indexOf(c) >= 0) matches++;
        }
        
        // 至少80%的字符匹配
        return matches >= keyword.length() * 0.8;
    }
    
    private double calculateMatchScore(String keyword, Song song) {
        double score = 0;
        String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
        String artist = song.getArtist() != null ? song.getArtist().toLowerCase() : "";
        
        // 标题完全匹配得分最高
        if (title.equals(keyword)) score += 100;
        else if (title.contains(keyword)) score += 50;
        else score += fuzzyMatchScore(keyword, title) * 30;
        
        // 艺术家匹配
        if (artist.equals(keyword)) score += 80;
        else if (artist.contains(keyword)) score += 40;
        else score += fuzzyMatchScore(keyword, artist) * 20;
        
        return score;
    }
    
    private double fuzzyMatchScore(String keyword, String target) {
        int matches = 0;
        for (char c : keyword.toCharArray()) {
            if (target.indexOf(c) >= 0) matches++;
        }
        return matches / (double) keyword.length();
    }
    
    // 高级筛选
    public List<Song> advancedSearch(SearchCriteria criteria) {
        List<Song> allSongs = libraryService.getSongs();
        
        return allSongs.stream()
            .filter(song -> matchesCriteria(song, criteria))
            .collect(Collectors.toList());
    }
    
    private boolean matchesCriteria(Song song, SearchCriteria criteria) {
        // 标题筛选
        if (criteria.titleKeyword != null && !criteria.titleKeyword.isEmpty()) {
            if (song.getTitle() == null || 
                !song.getTitle().toLowerCase().contains(criteria.titleKeyword.toLowerCase())) {
                return false;
            }
        }
        
        // 艺术家筛选
        if (criteria.artistKeyword != null && !criteria.artistKeyword.isEmpty()) {
            if (song.getArtist() == null || 
                !song.getArtist().toLowerCase().contains(criteria.artistKeyword.toLowerCase())) {
                return false;
            }
        }
        
        // 专辑筛选
        if (criteria.albumKeyword != null && !criteria.albumKeyword.isEmpty()) {
            if (song.getAlbum() == null || 
                !song.getAlbum().toLowerCase().contains(criteria.albumKeyword.toLowerCase())) {
                return false;
            }
        }
        
        // 格式筛选
        if (criteria.format != null && !criteria.format.isEmpty()) {
            if (song.getFormat() == null || 
                !song.getFormat().equalsIgnoreCase(criteria.format)) {
                return false;
            }
        }
        
        // 时长筛选
        if (criteria.minDuration > 0 && song.getDuration() < criteria.minDuration) {
            return false;
        }
        if (criteria.maxDuration > 0 && song.getDuration() > criteria.maxDuration) {
            return false;
        }
        
        // 文件大小筛选
        if (criteria.minFileSize > 0 && song.getFileSize() < criteria.minFileSize) {
            return false;
        }
        if (criteria.maxFileSize > 0 && song.getFileSize() > criteria.maxFileSize) {
            return false;
        }
        
        // 播放次数筛选
        if (criteria.minPlayCount > 0) {
            // 需要从历史服务获取播放次数
            // 简化处理
        }
        
        // 是否收藏筛选
        if (criteria.onlyFavorites) {
            if (!favoritesService.isFavorite(song.getId())) {
                return false;
            }
        }
        
        return true;
    }
    
    // 搜索播放列表
    public List<Playlist> searchPlaylists(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return playlistService.getAllPlaylists();
        }
        
        keyword = keyword.toLowerCase();
        List<Playlist> results = new ArrayList<>();
        
        for (Playlist playlist : playlistService.getAllPlaylists()) {
            if (playlist.getName().toLowerCase().contains(keyword)) {
                results.add(playlist);
            }
        }
        
        return results;
    }
    
    // 查找相似歌曲
    public List<Song> findSimilarSongs(Song targetSong, int maxResults) {
        List<Song> allSongs = libraryService.getSongs();
        List<Song> similar = new ArrayList<>();
        
        // 基于艺术家、专辑、标题相似度
        for (Song song : allSongs) {
            if (song.getId().equals(targetSong.getId())) continue;
            
            double similarity = calculateSongSimilarity(targetSong, song);
            if (similarity > 0.3) {
                similar.add(song);
            }
        }
        
        // 按相似度排序
        similar.sort((a, b) -> {
            double simA = calculateSongSimilarity(targetSong, a);
            double simB = calculateSongSimilarity(targetSong, b);
            return Double.compare(simB, simA);
        });
        
        if (similar.size() > maxResults) {
            similar = similar.subList(0, maxResults);
        }
        
        return similar;
    }
    
    private double calculateSongSimilarity(Song song1, Song song2) {
        double score = 0;
        
        // 艺术家相同
        if (song1.getArtist() != null && song2.getArtist() != null) {
            if (song1.getArtist().equals(song2.getArtist())) {
                score += 50;
            } else if (song1.getArtist().toLowerCase().contains(song2.getArtist().toLowerCase()) ||
                       song2.getArtist().toLowerCase().contains(song1.getArtist().toLowerCase())) {
                score += 30;
            }
        }
        
        // 专辑相同
        if (song1.getAlbum() != null && song2.getAlbum() != null) {
            if (song1.getAlbum().equals(song2.getAlbum())) {
                score += 40;
            }
        }
        
        // 标题相似
        if (song1.getTitle() != null && song2.getTitle() != null) {
            double titleSim = fuzzyMatchScore(song1.getTitle().toLowerCase(), 
                                             song2.getTitle().toLowerCase());
            score += titleSim * 20;
        }
        
        return score / 100.0;
    }
    
    // 搜索历史记录
    public List<Song> searchHistory(String keyword) {
        // 从播放历史中搜索
        List<Song> results = new ArrayList<>();
        keyword = keyword != null ? keyword.toLowerCase() : "";
        
        for (var entry : historyService.getRecentHistory(100)) {
            Song song = new Song();
            song.setId(entry.getSongId());
            song.setTitle(entry.getTitle());
            song.setArtist(entry.getArtist());
            song.setFilePath(entry.getFilePath());
            
            if (keyword.isEmpty() || 
                (entry.getTitle() != null && entry.getTitle().toLowerCase().contains(keyword)) ||
                (entry.getArtist() != null && entry.getArtist().toLowerCase().contains(keyword))) {
                results.add(song);
            }
        }
        
        return results;
    }
    
    // 搜索收藏夹
    public List<Song> searchFavorites(String keyword) {
        List<Song> favorites = favoritesService.getAllFavorites();
        
        if (keyword == null || keyword.isEmpty()) {
            return favorites;
        }
        
        keyword = keyword.toLowerCase();
        List<Song> results = new ArrayList<>();
        
        for (Song song : favorites) {
            String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
            String artist = song.getArtist() != null ? song.getArtist().toLowerCase() : "";
            
            if (title.contains(keyword) || artist.contains(keyword)) {
                results.add(song);
            }
        }
        
        return results;
    }
    
    // 搜索条件类
    public static class SearchCriteria {
        public String titleKeyword;
        public String artistKeyword;
        public String albumKeyword;
        public String format;
        public int minDuration;
        public int maxDuration;
        public long minFileSize;
        public long maxFileSize;
        public int minPlayCount;
        public int maxPlayCount;
        public boolean onlyFavorites;
        public int year;
        
        public SearchCriteria() {
            minDuration = 0;
            maxDuration = 0;
            minFileSize = 0;
            maxFileSize = 0;
            minPlayCount = 0;
            maxPlayCount = 0;
            onlyFavorites = false;
            year = 0;
        }
    }
}