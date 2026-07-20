package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

/**
 * 跨设备同步服务
 * 管理播放状态、播放列表和设置的跨设备同步
 */
public class SyncService {
    private static SyncService instance;
    
    private Gson gson;
    private String syncDir;
    private String deviceId;
    
    public static SyncService getInstance() {
        if (instance == null) {
            instance = new SyncService();
        }
        return instance;
    }
    
    private SyncService() {
        gson = new Gson();
        syncDir = System.getProperty("user.home") + "/.yinyue/sync";
        deviceId = generateDeviceId();
        new File(syncDir).mkdirs();
    }
    
    private String generateDeviceId() {
        return "device_" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
    }
    
    // 导出当前设备状态
    public void exportState() {
        try {
            PlayerState state = new PlayerState();
            state.deviceId = deviceId;
            state.timestamp = System.currentTimeMillis();
            
            // 保存播放状态
            AudioPlayerService player = AudioPlayerService.getInstance();
            state.isPlaying = player.isPlaying();
            state.volume = player.getVolume();
            
            // 保存当前歌曲
            Song current = player.getCurrentSong();
            if (current != null) {
                state.currentSongId = current.getId();
                state.currentSongPath = current.getFilePath();
                state.currentSongTitle = current.getTitle();
                state.currentSongArtist = current.getArtist();
            }
            
            // 保存播放列表
            PlaylistService playlistService = PlaylistService.getInstance();
            state.currentPlaylistName = playlistService.getCurrentPlaylist() != null ? 
                playlistService.getCurrentPlaylist().getName() : null;
            state.currentIndex = playlistService.getCurrentIndex();
            
            // 保存设置
            state.playMode = playlistService.getPlayMode().ordinal();
            
            String json = gson.toJson(state);
            File file = new File(syncDir, deviceId + "_state.json");
            java.nio.file.Files.write(file.toPath(), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 从其他设备导入状态
    public PlayerState importState(String deviceId) {
        try {
            File file = new File(syncDir, deviceId + "_state.json");
            if (!file.exists()) return null;
            
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            return gson.fromJson(json, PlayerState.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 获取所有可用设备
    public List<String> getAvailableDevices() {
        List<String> devices = new ArrayList<>();
        File dir = new File(syncDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith("_state.json"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String deviceId = name.substring(0, name.lastIndexOf("_state.json"));
                if (!deviceId.equals(this.deviceId)) {
                    devices.add(deviceId);
                }
            }
        }
        return devices;
    }
    
    // 同步播放列表
    public void exportPlaylists() {
        try {
            PlaylistService playlistService = PlaylistService.getInstance();
            List<com.yinyue.player.model.Playlist> playlists = playlistService.getAllPlaylists();
            
            String json = gson.toJson(playlists);
            File file = new File(syncDir, deviceId + "_playlists.json");
            java.nio.file.Files.write(file.toPath(), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<com.yinyue.player.model.Playlist> importPlaylists(String deviceId) {
        try {
            File file = new File(syncDir, deviceId + "_playlists.json");
            if (!file.exists()) return new ArrayList<>();
            
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            return gson.fromJson(json, new TypeToken<List<com.yinyue.player.model.Playlist>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // 同步设置
    public void exportSettings() {
        try {
            Map<String, Object> settings = new HashMap<>();
            AudioPlayerService player = AudioPlayerService.getInstance();
            settings.put("volume", player.getVolume());
            settings.put("playMode", PlaylistService.getInstance().getPlayMode());
            
            String json = gson.toJson(settings);
            File file = new File(syncDir, deviceId + "_settings.json");
            java.nio.file.Files.write(file.toPath(), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, Object> importSettings(String deviceId) {
        try {
            File file = new File(syncDir, deviceId + "_settings.json");
            if (!file.exists()) return new HashMap<>();
            
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            return gson.fromJson(json, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
    
    // 获取设备ID
    public String getDeviceId() {
        return deviceId;
    }
    
    // 清理过期同步数据
    public void cleanupOldSyncs(long maxAge) {
        File dir = new File(syncDir);
        File[] files = dir.listFiles();
        if (files != null) {
            long now = System.currentTimeMillis();
            for (File file : files) {
                if (now - file.lastModified() > maxAge) {
                    file.delete();
                }
            }
        }
    }
    
    // 播放状态类
    public static class PlayerState {
        public String deviceId;
        public long timestamp;
        public boolean isPlaying;
        public double volume;
        public String currentSongId;
        public String currentSongPath;
        public String currentSongTitle;
        public String currentSongArtist;
        public String currentPlaylistName;
        public int currentIndex;
        public int playMode;
    }
    
}