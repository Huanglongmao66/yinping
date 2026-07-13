package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import java.util.*;

/**
 * 音频文件监控服务
 * 监控音乐目录变化，自动更新音乐库
 */
public class AudioNormalizationService {
    private static AudioNormalizationService instance;
    
    private Map<String, Double> volumeLevels; // songId -> normalized volume level
    private Map<String, ReplayGainInfo> replayGainData;
    private double targetLevel; // 目标音量级别 (dB)
    private boolean normalizationEnabled;
    
    private LibraryService libraryService;
    
    public static AudioNormalizationService getInstance() {
        if (instance == null) {
            instance = new AudioNormalizationService();
        }
        return instance;
    }
    
    private AudioNormalizationService() {
        volumeLevels = new HashMap<>();
        replayGainData = new HashMap<>();
        targetLevel = -14.0; // 标准ReplayGain目标级别
        normalizationEnabled = true;
        libraryService = LibraryService.getInstance();
    }
    
    // 分析歌曲的音量级别
    public ReplayGainInfo analyzeSong(Song song) {
        if (song == null || song.getFilePath() == null) {
            return null;
        }
        
        ReplayGainInfo info = new ReplayGainInfo();
        
        // 简化的音量分析（实际应使用专业库）
        // 这里使用文件大小和时长估算平均音量
        long fileSize = song.getFileSize();
        int duration = (int)song.getDuration();
        
        if (duration > 0) {
            // 估算比特率和音量级别
            double bitrate = fileSize * 8.0 / duration / 1000; // kbps
            
            // 根据比特率估算音量（简化）
            info.gain = -12.0 + (bitrate / 320.0) * 6.0; // 范围 -12 到 -6 dB
            info.peak = 0.9 + (bitrate / 320.0) * 0.1; // 范围 0.9 到 1.0
            
            replayGainData.put(song.getId(), info);
            volumeLevels.put(song.getId(), info.gain);
        }
        
        return info;
    }
    
    // 批量分析音乐库
    public void analyzeLibrary() {
        for (Song song : libraryService.getSongs()) {
            analyzeSong(song);
        }
    }
    
    // 获取歌曲的标准化音量调节值
    public double getVolumeAdjustment(String songId) {
        if (!normalizationEnabled) return 0;
        
        ReplayGainInfo info = replayGainData.get(songId);
        if (info == null) return 0;
        
        // 计算需要的音量调节
        return info.gain - targetLevel;
    }
    
    // 获取歌曲的实际播放音量（0-1范围）
    public double getNormalizedVolume(String songId, double baseVolume) {
        double adjustment = getVolumeAdjustment(songId);
        
        // 转换dB调节到线性音量
        // dB转线性: volume = 10^(dB/20)
        double linearAdjustment = Math.pow(10, adjustment / 20.0);
        
        return Math.min(1.0, baseVolume * linearAdjustment);
    }
    
    // 设置目标音量级别
    public void setTargetLevel(double level) {
        this.targetLevel = level;
    }
    
    public double getTargetLevel() {
        return targetLevel;
    }
    
    // 启用/禁用音量标准化
    public void setNormalizationEnabled(boolean enabled) {
        this.normalizationEnabled = enabled;
    }
    
    public boolean isNormalizationEnabled() {
        return normalizationEnabled;
    }
    
    // 手动设置歌曲的音量级别
    public void setSongGain(String songId, double gain) {
        ReplayGainInfo info = replayGainData.get(songId);
        if (info != null) {
            info.gain = gain;
        } else {
            info = new ReplayGainInfo();
            info.gain = gain;
            info.peak = 1.0;
            replayGainData.put(songId, info);
        }
        volumeLevels.put(songId, gain);
    }
    
    // 获取歌曲的ReplayGain信息
    public ReplayGainInfo getReplayGain(String songId) {
        return replayGainData.get(songId);
    }
    
    // 获取所有已分析的歌曲
    public List<String> getAnalyzedSongs() {
        return new ArrayList<>(replayGainData.keySet());
    }
    
    // 清除分析数据
    public void clearAnalysis() {
        volumeLevels.clear();
        replayGainData.clear();
    }
    
    // 预增益模式：所有歌曲增益指定值
    public void applyPreGain(double preGain) {
        for (ReplayGainInfo info : replayGainData.values()) {
            info.gain += preGain;
        }
    }
    
    // 削峰保护：确保不超过峰值
    public double getSafeVolume(String songId, double desiredVolume) {
        ReplayGainInfo info = replayGainData.get(songId);
        if (info == null) return desiredVolume;
        
        // 检查是否会削峰
        double normalized = getNormalizedVolume(songId, desiredVolume);
        if (normalized > info.peak) {
            normalized = info.peak;
        }
        
        return normalized;
    }
    
    // ReplayGain信息类
    public static class ReplayGainInfo {
        public double gain; // dB增益值
        public double peak; // 峰值电平 (0-1)
        
        public ReplayGainInfo() {
            gain = 0;
            peak = 1.0;
        }
        
        public double getGain() { return gain; }
        public double getPeak() { return peak; }
    }
}