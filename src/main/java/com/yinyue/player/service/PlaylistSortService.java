package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.model.Playlist;
import java.util.*;

/**
 * 播放列表排序服务
 * 提供多种智能排序方式
 */
public class PlaylistSortService {
    private static PlaylistSortService instance;
    
    private PlaylistService playlistService;
    private MusicRatingService ratingService;
    
    public static PlaylistSortService getInstance() {
        if (instance == null) {
            instance = new PlaylistSortService();
        }
        return instance;
    }
    
    private PlaylistSortService() {
        playlistService = PlaylistService.getInstance();
        ratingService = MusicRatingService.getInstance();
    }
    
    // 按标题排序
    public void sortByTitle(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            String ta = a.getTitle() != null ? a.getTitle() : "";
            String tb = b.getTitle() != null ? b.getTitle() : "";
            return ascending ? ta.compareToIgnoreCase(tb) : tb.compareToIgnoreCase(ta);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按艺术家排序
    public void sortByArtist(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            String aa = a.getArtist() != null ? a.getArtist() : "";
            String ab = b.getArtist() != null ? b.getArtist() : "";
            return ascending ? aa.compareToIgnoreCase(ab) : ab.compareToIgnoreCase(aa);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按专辑排序
    public void sortByAlbum(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            String aa = a.getAlbum() != null ? a.getAlbum() : "";
            String ab = b.getAlbum() != null ? b.getAlbum() : "";
            return ascending ? aa.compareToIgnoreCase(ab) : ab.compareToIgnoreCase(aa);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按时长排序
    public void sortByDuration(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            long da = a.getDuration();
            long db = b.getDuration();
            return ascending ? Long.compare(da, db) : Long.compare(db, da);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按文件大小排序
    public void sortByFileSize(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            long sa = a.getFileSize();
            long sb = b.getFileSize();
            return ascending ? Long.compare(sa, sb) : Long.compare(sb, sa);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按评分排序
    public void sortByRating(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            int ra = ratingService.getRating(a.getId());
            int rb = ratingService.getRating(b.getId());
            return ascending ? Integer.compare(ra, rb) : Integer.compare(rb, ra);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按播放次数排序
    public void sortByPlayCount(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            int ca = ratingService.getPlayCount(a.getId());
            int cb = ratingService.getPlayCount(b.getId());
            return ascending ? Integer.compare(ca, cb) : Integer.compare(cb, ca);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按综合评分排序
    public void sortByCompositeScore(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            double sa = ratingService.getCompositeScore(a.getId());
            double sb = ratingService.getCompositeScore(b.getId());
            return ascending ? Double.compare(sa, sb) : Double.compare(sb, sa);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 随机排序
    public void sortRandomly(Playlist playlist) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        Collections.shuffle(songs);
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 智能排序（基于播放历史和评分）
    public void sortSmartly(Playlist playlist) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        
        // 智能排序算法：综合评分 + 最近播放时间
        songs.sort((a, b) -> {
            double scoreA = calculateSmartScore(a);
            double scoreB = calculateSmartScore(b);
            return Double.compare(scoreB, scoreA);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    private double calculateSmartScore(Song song) {
        double score = 0;
        
        // 评分 (0-5星) -> 0-100
        int rating = ratingService.getRating(song.getId());
        score += rating * 20;
        
        // 播放次数 -> 加权
        int playCount = ratingService.getPlayCount(song.getId());
        score += Math.min(playCount * 2, 50);
        
        // 最近播放时间（越近分数越高）
        long lastPlay = ratingService.getLastPlayTime(song.getId());
        if (lastPlay > 0) {
            long daysSince = (System.currentTimeMillis() - lastPlay) / (1000 * 60 * 60 * 24);
            score += Math.max(0, 30 - daysSince);
        }
        
        return score;
    }
    
    // 按添加时间排序
    public void sortByAddedTime(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            // 使用文件修改时间作为添加时间的近似
            File fa = new File(a.getFilePath());
            File fb = new File(b.getFilePath());
            long ta = fa.exists() ? fa.lastModified() : 0;
            long tb = fb.exists() ? fb.lastModified() : 0;
            return ascending ? Long.compare(ta, tb) : Long.compare(tb, ta);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 按格式排序
    public void sortByFormat(Playlist playlist, boolean ascending) {
        if (playlist == null) return;
        
        List<Song> songs = playlist.getSongs();
        songs.sort((a, b) -> {
            String fa = a.getFormat() != null ? a.getFormat() : "";
            String fb = b.getFormat() != null ? b.getFormat() : "";
            return ascending ? fa.compareToIgnoreCase(fb) : fb.compareToIgnoreCase(fa);
        });
        
        playlist.setSongs(songs);
        // playlistService.savePlaylist(playlist);
    }
    
    // 获取可用的排序方式
    public List<SortOption> getAvailableSortOptions() {
        List<SortOption> options = new ArrayList<>();
        
        options.add(new SortOption("title", "标题"));
        options.add(new SortOption("artist", "艺术家"));
        options.add(new SortOption("album", "专辑"));
        options.add(new SortOption("duration", "时长"));
        options.add(new SortOption("fileSize", "文件大小"));
        options.add(new SortOption("rating", "评分"));
        options.add(new SortOption("playCount", "播放次数"));
        options.add(new SortOption("composite", "综合评分"));
        options.add(new SortOption("random", "随机"));
        options.add(new SortOption("smart", "智能排序"));
        options.add(new SortOption("addedTime", "添加时间"));
        options.add(new SortOption("format", "格式"));
        
        return options;
    }
    
    // 通用排序方法
    public void sort(Playlist playlist, String sortType, boolean ascending) {
        switch (sortType) {
            case "title": sortByTitle(playlist, ascending); break;
            case "artist": sortByArtist(playlist, ascending); break;
            case "album": sortByAlbum(playlist, ascending); break;
            case "duration": sortByDuration(playlist, ascending); break;
            case "fileSize": sortByFileSize(playlist, ascending); break;
            case "rating": sortByRating(playlist, ascending); break;
            case "playCount": sortByPlayCount(playlist, ascending); break;
            case "composite": sortByCompositeScore(playlist, ascending); break;
            case "random": sortRandomly(playlist); break;
            case "smart": sortSmartly(playlist); break;
            case "addedTime": sortByAddedTime(playlist, ascending); break;
            case "format": sortByFormat(playlist, ascending); break;
        }
    }
    
    // 排序选项类
    public static class SortOption {
        public String type;
        public String name;
        
        public SortOption(String type, String name) {
            this.type = type;
            this.name = name;
        }
        
        public String getType() { return type; }
        public String getName() { return name; }
    }
    
    private static class File {
        private String path;
        
        public File(String path) {
            this.path = path;
        }
        
        public boolean exists() {
            return new java.io.File(path).exists();
        }
        
        public long lastModified() {
            return new java.io.File(path).lastModified();
        }
    }
}