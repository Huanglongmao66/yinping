package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.AudioUtils;
import java.io.*;
import java.util.*;

/**
 * 批量标签编辑服务
 * 批量修改音频文件的ID3标签
 */
public class BatchTagEditorService {
    private static BatchTagEditorService instance;
    
    public static BatchTagEditorService getInstance() {
        if (instance == null) {
            instance = new BatchTagEditorService();
        }
        return instance;
    }
    
    private BatchTagEditorService() {}
    
    // 批量设置标题
    public int batchSetTitle(List<Song> songs, String title, boolean appendMode) {
        int count = 0;
        for (Song song : songs) {
            if (appendMode && song.getTitle() != null) {
                song.setTitle(song.getTitle() + title);
            } else {
                song.setTitle(title);
            }
            count++;
        }
        return count;
    }
    
    // 批量设置艺术家
    public int batchSetArtist(List<Song> songs, String artist) {
        int count = 0;
        for (Song song : songs) {
            song.setArtist(artist);
            count++;
        }
        return count;
    }
    
    // 批量设置专辑
    public int batchSetAlbum(List<Song> songs, String album) {
        int count = 0;
        for (Song song : songs) {
            song.setAlbum(album);
            count++;
        }
        return count;
    }
    
    // 批量设置曲目编号
    public int batchSetTrackNumber(List<Song> songs, int startNumber) {
        int count = 0;
        int number = startNumber;
        for (Song song : songs) {
            // 曲目编号功能暂不支持
            count++;
        }
        return count;
    }
    
    // 智能标题填充（从文件名提取）
    public int autoFillTitles(List<Song> songs) {
        int count = 0;
        for (Song song : songs) {
            String title = extractTitleFromFilename(song.getFilePath());
            if (title != null && !title.isEmpty()) {
                song.setTitle(title);
                count++;
            }
        }
        return count;
    }
    
    // 智能艺术家填充（从文件夹名提取）
    public int autoFillArtists(List<Song> songs) {
        int count = 0;
        for (Song song : songs) {
            String artist = extractArtistFromPath(song.getFilePath());
            if (artist != null && !artist.isEmpty()) {
                song.setArtist(artist);
                count++;
            }
        }
        return count;
    }
    
    // 智能专辑填充（从父文件夹名提取）
    public int autoFillAlbums(List<Song> songs) {
        int count = 0;
        for (Song song : songs) {
            String album = extractAlbumFromPath(song.getFilePath());
            if (album != null && !album.isEmpty()) {
                song.setAlbum(album);
                count++;
            }
        }
        return count;
    }
    
    // 从文件名提取标题
    private String extractTitleFromFilename(String filePath) {
        if (filePath == null) return null;
        
        File file = new File(filePath);
        String name = file.getName();
        
        // 移除扩展名
        name = name.replaceAll("\\.[^.]+$", "");
        
        // 尝试解析 "艺术家 - 标题" 格式
        if (name.contains(" - ")) {
            String[] parts = name.split(" - ", 2);
            return parts[1].trim();
        }
        
        // 尝试解析 "序号. 标题" 格式
        if (name.matches("^\\d+[.\\s]+.+")) {
            return name.replaceFirst("^\\d+[.\\s]+", "").trim();
        }
        
        return name;
    }
    
    // 从路径提取艺术家
    private String extractArtistFromPath(String filePath) {
        if (filePath == null) return null;
        
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null) {
            String folderName = parent.getName();
            
            // 如果文件夹名看起来像 "艺术家 - 专辑"
            if (folderName.contains(" - ")) {
                return folderName.split(" - ")[0].trim();
            }
            
            return folderName;
        }
        return null;
    }
    
    // 从路径提取专辑
    private String extractAlbumFromPath(String filePath) {
        if (filePath == null) return null;
        
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null) {
            String folderName = parent.getName();
            
            if (folderName.contains(" - ")) {
                String[] parts = folderName.split(" - ", 2);
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
            
            return folderName;
        }
        return null;
    }
    
    // 批量重命名文件
    public Map<Song, Boolean> batchRenameFiles(List<Song> songs, String pattern) {
        Map<Song, Boolean> results = new HashMap<>();
        
        for (Song song : songs) {
            String newName = generateNewFilename(song, pattern);
            if (newName != null) {
                boolean success = renameFile(song.getFilePath(), newName);
                results.put(song, success);
                if (success) {
                    song.setFilePath(newName);
                }
            } else {
                results.put(song, false);
            }
        }
        
        return results;
    }
    
    // 生成新文件名
    private String generateNewFilename(Song song, String pattern) {
        if (pattern == null) return null;
        
        String result = pattern;
        result = result.replace("{title}", song.getTitle() != null ? song.getTitle() : "Unknown");
        result = result.replace("{artist}", song.getArtist() != null ? song.getArtist() : "Unknown");
        result = result.replace("{album}", song.getAlbum() != null ? song.getAlbum() : "Unknown");
        result = result.replace("{track}", "0");
        result = result.replace("{year}", "0");
        
        // 清理非法字符
        result = result.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        File file = new File(song.getFilePath());
        String dir = file.getParent();
        String ext = AudioUtils.getFileExtension(file.getName());
        
        return dir + File.separator + result + "." + ext;
    }
    
    // 重命名文件
    private boolean renameFile(String oldPath, String newPath) {
        try {
            File oldFile = new File(oldPath);
            File newFile = new File(newPath);
            
            if (newFile.exists()) {
                return false;
            }
            
            return oldFile.renameTo(newFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 批量操作结果
    public static class BatchResult {
        public int successCount;
        public int failCount;
        public List<Song> failedSongs = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
        
        public boolean isAllSuccess() {
            return failCount == 0;
        }
    }
    
    // 执行批量操作
    public BatchResult executeBatchOperation(List<Song> songs, TagOperation operation) {
        BatchResult result = new BatchResult();
        
        for (Song song : songs) {
            try {
                applyOperation(song, operation);
                result.successCount++;
            } catch (Exception e) {
                result.failCount++;
                result.failedSongs.add(song);
                result.errors.add(e.getMessage());
            }
        }
        
        return result;
    }
    
    private void applyOperation(Song song, TagOperation operation) {
        switch (operation.type) {
            case "title":
                if (operation.append) {
                    song.setTitle((song.getTitle() != null ? song.getTitle() : "") + operation.value);
                } else {
                    song.setTitle(operation.value);
                }
                break;
            case "artist":
                song.setArtist(operation.value);
                break;
            case "album":
                song.setAlbum(operation.value);
                break;
            case "clear":
                if ("title".equals(operation.target)) song.setTitle("");
                else if ("artist".equals(operation.target)) song.setArtist("");
                else if ("album".equals(operation.target)) song.setAlbum("");
                break;
        }
    }
    
    // 标签操作类
    public static class TagOperation {
        public String type;
        public String value;
        public String target;
        public boolean append;
        
        public TagOperation(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}