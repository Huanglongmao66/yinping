package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import javafx.scene.media.MediaPlayer;
import java.util.*;

/**
 * 音频分析服务
 * 提供音频频谱分析、节拍检测、音频指纹等功能
 */
public class AudioAnalyzerService {
    private static AudioAnalyzerService instance;
    
    private double[] spectrumData;
    private double[] peakData;
    private double averageEnergy;
    private double peakEnergy;
    private double bpmEstimate;
    private List<Double> energyHistory;
    private int historySize = 100;
    
    private AudioPlayerService playerService;
    
    public static AudioAnalyzerService getInstance() {
        if (instance == null) {
            instance = new AudioAnalyzerService();
        }
        return instance;
    }
    
    private AudioAnalyzerService() {
        spectrumData = new double[128];
        peakData = new double[128];
        energyHistory = new ArrayList<>();
        playerService = AudioPlayerService.getInstance();
    }
    
    // 分析音频频谱
    public double[] analyzeSpectrum(float[] magnitudes) {
        for (int i = 0; i < spectrumData.length && i < magnitudes.length; i++) {
            spectrumData[i] = (magnitudes[i] + 60) / 60.0; // Normalize
        }
        
        // 计算平均能量
        averageEnergy = Arrays.stream(spectrumData).average().orElse(0);
        
        // 记录能量历史用于节拍检测
        energyHistory.add(averageEnergy);
        if (energyHistory.size() > historySize) {
            energyHistory.remove(0);
        }
        
        // 检测峰值
        detectPeaks();
        
        // 检测节拍
        detectBeats();
        
        return spectrumData;
    }
    
    private void detectPeaks() {
        double threshold = averageEnergy * 1.5;
        for (int i = 0; i < spectrumData.length; i++) {
            if (spectrumData[i] > threshold) {
                peakData[i] = spectrumData[i];
            } else {
                peakData[i] = 0;
            }
        }
        
        // 计算峰值能量
        peakEnergy = Arrays.stream(peakData).sum() / spectrumData.length;
    }
    
    private void detectBeats() {
        if (energyHistory.size() < 20) return;
        
        // 简单节拍检测算法
        int beatCount = 0;
        double localMax = 0;
        for (int i = 10; i < energyHistory.size(); i++) {
            double current = energyHistory.get(i);
            double prev = energyHistory.get(i - 10);
            
            if (current > localMax) {
                localMax = current;
            }
            
            // 检测能量突增（节拍）
            if (current > prev * 1.3 && current > averageEnergy * 1.2) {
                beatCount++;
            }
        }
        
        //估算 BPM (基于帧率约60fps)
        if (beatCount > 0) {
            double beatsPerSecond = beatCount / (energyHistory.size() / 60.0);
            bpmEstimate = beatsPerSecond * 60;
            // 调整到合理范围(60-180 BPM)
            while (bpmEstimate < 60) bpmEstimate *= 2;
            while (bpmEstimate > 180) bpmEstimate /= 2;
        }
    }
    
    // 获取频谱数据
    public double[] getSpectrumData() {
        return spectrumData;
    }
    
    public double[] getPeakData() {
        return peakData;
    }
    
    // 获取能量信息
    public double getAverageEnergy() {
        return averageEnergy;
    }
    
    public double getPeakEnergy() {
        return peakEnergy;
    }
    
    // 获取估算BPM
    public double getBpmEstimate() {
        return bpmEstimate;
    }
    
    // 获取频率分布
    public Map<String, Double> getFrequencyDistribution() {
        Map<String, Double> distribution = new HashMap<>();
        
        // 低频(0-20): Bass
        double lowFreq = 0;
        for (int i = 0; i < 20 && i < spectrumData.length; i++) {
            lowFreq += spectrumData[i];
        }
        distribution.put("低频 (Bass)", lowFreq / 20);
        
        // 中频(20-60): Mid
        double midFreq = 0;
        for (int i = 20; i < 60 && i < spectrumData.length; i++) {
            midFreq += spectrumData[i];
        }
        distribution.put("中频 (Mid)", midFreq / 40);
        
        // 高频(60-128): High
        double highFreq = 0;
        for (int i = 60; i < spectrumData.length; i++) {
            highFreq += spectrumData[i];
        }
        distribution.put("高频 (High)", highFreq / (spectrumData.length - 60));
        
        return distribution;
    }
    
    // 检测歌曲特征
    public Map<String, Object> analyzeSongFeatures(Song song) {
        Map<String, Object> features = new HashMap<>();
        
        features.put("平均能量", averageEnergy);
        features.put("峰值能量", peakEnergy);
        features.put("估算BPM", bpmEstimate);
        features.put("频率分布", getFrequencyDistribution());
        features.put("动态范围", peakEnergy - averageEnergy);
        
        // 判断音乐类型
        String genre = estimateGenre();
        features.put("估算类型", genre);
        
        return features;
    }
    
    private String estimateGenre() {
        Map<String, Double> dist = getFrequencyDistribution();
        double low = dist.getOrDefault("低频 (Bass)", 0.0);
        double mid = dist.getOrDefault("中频 (Mid)", 0.0);
        double high = dist.getOrDefault("高频 (High)", 0.0);
        
        if (low > 0.6 && bpmEstimate > 100) {
            return "电子/舞曲";
        } else if (low > 0.5 && bpmEstimate < 80) {
            return "摇滚/金属";
        } else if (mid > 0.5 && high < 0.3) {
            return "流行/R&B";
        } else if (high > 0.4) {
            return "古典/爵士";
        } else {
            return "其他";
        }
    }
    
    // 音频指纹生成（简化版）
    public String generateAudioFingerprint(Song song) {
        StringBuilder fingerprint = new StringBuilder();
        
        // 基于频谱峰值生成指纹
        for (int i = 0; i < peakData.length; i += 10) {
            if (peakData[i] > 0) {
                fingerprint.append(String.format("%02X", (int)(peakData[i] * 255)));
            } else {
                fingerprint.append("00");
            }
        }
        
        return fingerprint.toString();
    }
    
    // 比较两首歌曲相似度
    public double compareSimilarity(String fingerprint1, String fingerprint2) {
        if (fingerprint1 == null || fingerprint2 == null ||
            fingerprint1.length() != fingerprint2.length()) {
            return 0;
        }
        
        int matches = 0;
        for (int i = 0; i < fingerprint1.length(); i += 2) {
            String chunk1 = fingerprint1.substring(i, Math.min(i + 2, fingerprint1.length()));
            String chunk2 = fingerprint2.substring(i, Math.min(i + 2, fingerprint2.length()));
            if (chunk1.equals(chunk2)) {
                matches++;
            }
        }
        
        return (double) matches / (fingerprint1.length() / 2);
    }
    
    // 清除分析数据
    public void clearAnalysis() {
        Arrays.fill(spectrumData, 0);
        Arrays.fill(peakData, 0);
        averageEnergy = 0;
        peakEnergy = 0;
        bpmEstimate = 0;
        energyHistory.clear();
    }
}