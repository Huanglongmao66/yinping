package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.model.Playlist;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

/**
 * 播放列表模板服务
 * 保存和复用播放列表模板
 */
public class PlaylistTemplateService {
    private static PlaylistTemplateService instance;
    
    private Map<String, PlaylistTemplate> templates;
    private Gson gson;
    private String dataFile;
    
    public static PlaylistTemplateService getInstance() {
        if (instance == null) {
            instance = new PlaylistTemplateService();
        }
        return instance;
    }
    
    private PlaylistTemplateService() {
        templates = new HashMap<>();
        gson = new Gson();
        dataFile = System.getProperty("user.home") + "/.yinyue/templates.json";
        loadFromFile();
    }
    
    // 从播放列表创建模板
    public void createFromPlaylist(String templateName, Playlist playlist) {
        if (templateName == null || playlist == null) return;
        
        PlaylistTemplate template = new PlaylistTemplate();
        template.name = templateName;
        template.createTime = System.currentTimeMillis();
        
        for (Song song : playlist.getSongs()) {
            TemplateSong ts = new TemplateSong();
            ts.title = song.getTitle();
            ts.artist = song.getArtist();
            ts.album = song.getAlbum();
            template.songs.add(ts);
        }
        
        templates.put(templateName, template);
        saveToFile();
    }
    
    // 创建空模板
    public void createEmptyTemplate(String templateName) {
        PlaylistTemplate template = new PlaylistTemplate();
        template.name = templateName;
        template.createTime = System.currentTimeMillis();
        templates.put(templateName, template);
        saveToFile();
    }
    
    // 删除模板
    public void deleteTemplate(String templateName) {
        templates.remove(templateName);
        saveToFile();
    }
    
    // 获取模板
    public PlaylistTemplate getTemplate(String templateName) {
        return templates.get(templateName);
    }
    
    // 获取所有模板名称
    public List<String> getTemplateNames() {
        return new ArrayList<>(templates.keySet());
    }
    
    // 获取所有模板
    public List<PlaylistTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }
    
    // 应用模板创建播放列表
    public Playlist applyTemplate(String templateName, String newPlaylistName) {
        PlaylistTemplate template = templates.get(templateName);
        if (template == null) return null;
        
        Playlist playlist = new Playlist();
        playlist.setId(UUID.randomUUID().toString());
        playlist.setName(newPlaylistName);
        
        LibraryService library = LibraryService.getInstance();
        List<Song> matchedSongs = new ArrayList<>();
        
        for (TemplateSong ts : template.songs) {
            Song matched = findMatchingSong(ts, library.getSongs());
            if (matched != null) {
                matchedSongs.add(matched);
            }
        }
        
        playlist.setSongs(matchedSongs);
        PlaylistService.getInstance().getAllPlaylists().add(playlist);
        
        return playlist;
    }
    
    // 查找匹配的歌曲
    private Song findMatchingSong(TemplateSong ts, List<Song> songs) {
        for (Song song : songs) {
            boolean titleMatch = ts.title != null && ts.title.equals(song.getTitle());
            boolean artistMatch = ts.artist != null && ts.artist.equals(song.getArtist());
            
            if (titleMatch && artistMatch) {
                return song;
            }
        }
        
        // 尝试仅按标题匹配
        for (Song song : songs) {
            if (ts.title != null && ts.title.equals(song.getTitle())) {
                return song;
            }
        }
        
        return null;
    }
    
    // 导出模板
    public boolean exportTemplate(String templateName, String filePath) {
        PlaylistTemplate template = templates.get(templateName);
        if (template == null) return false;
        
        try {
            String json = gson.toJson(template);
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), json.getBytes());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 导入模板
    public boolean importTemplate(String filePath) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
            PlaylistTemplate template = gson.fromJson(json, PlaylistTemplate.class);
            if (template != null && template.name != null) {
                templates.put(template.name, template);
                saveToFile();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 重命名模板
    public boolean renameTemplate(String oldName, String newName) {
        PlaylistTemplate template = templates.remove(oldName);
        if (template != null) {
            template.name = newName;
            templates.put(newName, template);
            saveToFile();
            return true;
        }
        return false;
    }
    
    // 复制模板
    public boolean copyTemplate(String sourceName, String newName) {
        PlaylistTemplate source = templates.get(sourceName);
        if (source == null || templates.containsKey(newName)) return false;
        
        PlaylistTemplate copy = new PlaylistTemplate();
        copy.name = newName;
        copy.createTime = System.currentTimeMillis();
        copy.songs.addAll(source.songs);
        
        templates.put(newName, copy);
        saveToFile();
        return true;
    }
    
    // 预设模板
    public void createPresetTemplates() {
        // Top 50模板
        PlaylistTemplate top50 = new PlaylistTemplate();
        top50.name = "热门歌曲50首";
        top50.createTime = System.currentTimeMillis();
        templates.put(top50.name, top50);
        
        // 随机100首
        PlaylistTemplate random100 = new PlaylistTemplate();
        random100.name = "随机100首";
        random100.createTime = System.currentTimeMillis();
        templates.put(random100.name, random100);
        
        saveToFile();
    }
    
    // 模板类
    public static class PlaylistTemplate {
        public String name;
        public long createTime;
        public List<TemplateSong> songs = new ArrayList<>();
        
        public String getName() { return name; }
        public int getSongCount() { return songs.size(); }
    }
    
    // 模板歌曲类
    public static class TemplateSong {
        public String title;
        public String artist;
        public String album;
    }
    
    private void saveToFile() {
        try {
            File file = new File(dataFile);
            file.getParentFile().mkdirs();
            String json = gson.toJson(templates);
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
            if (!file.exists()) {
                createPresetTemplates();
                return;
            }
            
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            Map<String, PlaylistTemplate> loaded = gson.fromJson(json, new TypeToken<Map<String, PlaylistTemplate>>(){}.getType());
            if (loaded != null) {
                templates.putAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
            createPresetTemplates();
        }
    }
}