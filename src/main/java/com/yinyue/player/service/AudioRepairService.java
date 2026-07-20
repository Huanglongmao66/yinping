package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.AudioUtils;
import java.io.*;
import java.util.*;

/**
 * 音频修复服务
 * 检测和修复损坏的音频文件
 */
public class AudioRepairService {
    private static AudioRepairService instance;
    
    public static AudioRepairService getInstance() {
        if (instance == null) {
            instance = new AudioRepairService();
        }
        return instance;
    }
    
    private AudioRepairService() {}
    
    // 检测文件是否损坏
    public boolean isFileCorrupted(String filePath) {
        if (filePath == null || filePath.isEmpty()) return true;
        
        File file = new File(filePath);
        
        // 检查文件是否存在
        if (!file.exists() || !file.isFile()) {
            return true;
        }
        
        // 检查文件大小
        if (file.length() == 0) {
            return true;
        }
        
        // 检查文件是否可读
        if (!file.canRead()) {
            return true;
        }
        
        // 检查文件头
        return !checkFileHeader(file);
    }
    
    // 检查文件头
    private boolean checkFileHeader(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[12];
            int read = fis.read(header);
            if (read < 4) return false;
            
            String format = AudioUtils.getFileExtension(file.getName()).toLowerCase();
            
            switch (format) {
                case "mp3":
                    return (header[0] == (byte)0xFF && (header[1] & 0xE0) == 0xE0) ||
                           (header[0] == 'I' && header[1] == 'D' && header[2] == '3');
                case "wav":
                    return header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F';
                case "flac":
                    return header[0] == 'f' && header[1] == 'L' && header[2] == 'a' && header[3] == 'C';
                case "ogg":
                    return header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S';
                case "m4a":
                case "mp4":
                    return header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    // 扫描音乐库中的损坏文件
    public List<Song> scanForCorruptedFiles() {
        List<Song> corrupted = new ArrayList<>();
        LibraryService library = LibraryService.getInstance();
        
        for (Song song : library.getSongs()) {
            if (isFileCorrupted(song.getFilePath())) {
                corrupted.add(song);
            }
        }
        
        return corrupted;
    }
    
    // 获取文件信息
    public FileInfo getFileInfo(String filePath) {
        FileInfo info = new FileInfo();
        File file = new File(filePath);
        
        if (!file.exists()) {
            info.status = "不存在";
            return info;
        }
        
        info.size = file.length();
        info.lastModified = file.lastModified();
        info.canRead = file.canRead();
        info.canWrite = file.canWrite();
        info.isCorrupted = isFileCorrupted(filePath);
        info.status = info.isCorrupted ? "已损坏" : "正常";
        
        return info;
    }
    
    // 修复文件（尝试重建文件头）
    public boolean repairFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) return false;
        
        File file = new File(filePath);
        if (!file.exists()) return false;
        
        try {
            String format = AudioUtils.getFileExtension(file.getName()).toLowerCase();
            
            switch (format) {
                case "mp3":
                    return repairMp3(file);
                case "wav":
                    return repairWav(file);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 修复MP3文件
    private boolean repairMp3(File file) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            
            // 查找第一个有效的MP3帧
            int frameStart = -1;
            for (int i = 0; i < data.length - 4; i++) {
                if (data[i] == (byte)0xFF && (data[i + 1] & 0xE0) == 0xE0) {
                    frameStart = i;
                    break;
                }
            }
            
            if (frameStart <= 0) return false;
            
            // 移除损坏的前缀数据
            byte[] repaired = Arrays.copyOfRange(data, frameStart, data.length);
            java.nio.file.Files.write(file.toPath(), repaired);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 修复WAV文件
    private boolean repairWav(File file) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            
            // 查找RIFF标记
            int riffPos = -1;
            for (int i = 0; i < data.length - 4; i++) {
                if (data[i] == 'R' && data[i + 1] == 'I' && data[i + 2] == 'F' && data[i + 3] == 'F') {
                    riffPos = i;
                    break;
                }
            }
            
            if (riffPos <= 0) return false;
            
            byte[] repaired = Arrays.copyOfRange(data, riffPos, data.length);
            java.nio.file.Files.write(file.toPath(), repaired);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 批量修复
    public Map<Song, Boolean> batchRepair(List<Song> songs) {
        Map<Song, Boolean> results = new HashMap<>();
        
        for (Song song : songs) {
            boolean success = repairFile(song.getFilePath());
            results.put(song, success);
        }
        
        return results;
    }
    
    // 删除损坏文件
    public boolean deleteCorruptedFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
    
    // 获取修复报告
    public RepairReport generateReport(List<Song> songs) {
        RepairReport report = new RepairReport();
        
        for (Song song : songs) {
            FileInfo info = getFileInfo(song.getFilePath());
            if (info.isCorrupted) {
                report.corruptedCount++;
                report.corruptedFiles.add(song);
            } else {
                report.healthyCount++;
            }
        }
        
        report.totalCount = songs.size();
        return report;
    }
    
    // 文件信息类
    public static class FileInfo {
        public long size;
        public long lastModified;
        public boolean canRead;
        public boolean canWrite;
        public boolean isCorrupted;
        public String status;
    }
    
    // 修复报告类
    public static class RepairReport {
        public int totalCount;
        public int healthyCount;
        public int corruptedCount;
        public List<Song> corruptedFiles = new ArrayList<>();
        
        public double getCorruptionRate() {
            return totalCount > 0 ? (corruptedCount * 100.0 / totalCount) : 0;
        }
    }
}