package com.yinyue.player.service;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.animation.AnimationTimer;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.AudioSpectrumListener;
import javafx.application.Platform;
import java.util.Arrays;

/**
 * 音频可视化增强服务
 * 提供波形、频谱、粒子等多种可视化效果
 */
public class AudioVisualizerService {
    private static AudioVisualizerService instance;
    
    private Canvas canvas;
    private MediaPlayer mediaPlayer;
    private AnimationTimer animationTimer;
    private double[] spectrumData;
    private double[] previousSpectrum;
    
    private int visualizerMode = 0; // 0=频谱, 1=波形, 2=圆形, 3=粒子, 4=瀑布图
    private Color primaryColor = Color.hsb(200, 0.8, 0.9);
    private Color secondaryColor = Color.hsb(280, 0.7, 0.8);
    private boolean smoothTransition = true;
    private double glowIntensity = 0.3;
    
    // 粒子系统
    private Particle[] particles;
    private int particleCount = 100;
    
    // 瀑布图数据
    private double[][] waterfallData;
    private int waterfallRows = 50;
    private int waterfallIndex = 0;
    
    public static AudioVisualizerService getInstance() {
        if (instance == null) {
            instance = new AudioVisualizerService();
        }
        return instance;
    }
    
    private AudioVisualizerService() {
        spectrumData = new double[128];
        previousSpectrum = new double[128];
        particles = new Particle[particleCount];
        for (int i = 0; i < particleCount; i++) {
            particles[i] = new Particle();
        }
        waterfallData = new double[waterfallRows][128];
    }
    
    public void initialize(Canvas canvas, MediaPlayer mediaPlayer) {
        this.canvas = canvas;
        this.mediaPlayer = mediaPlayer;
        
        if (mediaPlayer != null) {
            mediaPlayer.setAudioSpectrumListener(new AudioSpectrumListener() {
                @Override
                public void spectrumDataUpdate(double timestamp, double duration, 
                                               float[] magnitudes, float[] phases) {
                    Platform.runLater(() -> {
                        updateSpectrumData(magnitudes);
                    });
                }
            });
        }
        
        startAnimation();
    }
    
    private void updateSpectrumData(float[] magnitudes) {
        if (smoothTransition && previousSpectrum != null) {
            for (int i = 0; i < spectrumData.length && i < magnitudes.length; i++) {
                double target = (magnitudes[i] + 60) / 60.0; // Normalize from -60dB to 0dB
                spectrumData[i] = previousSpectrum[i] + (target - previousSpectrum[i]) * 0.3;
                previousSpectrum[i] = spectrumData[i];
            }
        } else {
            for (int i = 0; i < spectrumData.length && i < magnitudes.length; i++) {
                spectrumData[i] = (magnitudes[i] + 60) / 60.0;
            }
        }
        
        // 更新瀑布图数据
        waterfallData[waterfallIndex] = Arrays.copyOf(spectrumData, spectrumData.length);
        waterfallIndex = (waterfallIndex + 1) % waterfallRows;
        
        // 更新粒子
        updateParticles();
    }
    
    private void updateParticles() {
        double avgEnergy = Arrays.stream(spectrumData).average().orElse(0);
        for (Particle p : particles) {
            p.update(avgEnergy);
        }
    }
    
    public void startAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawVisualizer();
            }
        };
        animationTimer.start();
    }
    
    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
    
    private void drawVisualizer() {
        if (canvas == null) return;
        
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        
        // 清除画布，带渐变效果
        gc.setFill(Color.rgb(20, 20, 30, 0.3));
        gc.fillRect(0, 0, width, height);
        
        switch (visualizerMode) {
            case 0: drawSpectrum(gc, width, height); break;
            case 1: drawWaveform(gc, width, height); break;
            case 2: drawCircular(gc, width, height); break;
            case 3: drawParticles(gc, width, height); break;
            case 4: drawWaterfall(gc, width, height); break;
        }
    }
    
    private void drawSpectrum(GraphicsContext gc, double width, double height) {
        int barCount = Math.min(64, spectrumData.length);
        double barWidth = width / barCount - 2;
        double maxHeight = height * 0.9;
        
        for (int i = 0; i < barCount; i++) {
            double value = spectrumData[i];
            double barHeight = value * maxHeight;
            
            // 渐变色
            double hue = 200 + value * 80;
            Color color = Color.hsb(hue, 0.8, 0.9, smoothTransition ? 0.8 : 1.0);
            
            // 发光效果
            if (glowIntensity > 0) {
                gc.setFill(color.deriveColor(0, 1, 1, glowIntensity));
                gc.fillRect(i * (barWidth + 2) - glowIntensity * 2, 
                           height - barHeight - glowIntensity * 2,
                           barWidth + glowIntensity * 4,
                           barHeight + glowIntensity * 4);
            }
            
            gc.setFill(color);
            gc.fillRect(i * (barWidth + 2), height - barHeight, barWidth, barHeight);
            
            // 反射效果
            gc.setFill(color.deriveColor(0, 1, 0.5, 0.3));
            gc.fillRect(i * (barWidth + 2), height, barWidth, barHeight * 0.3);
        }
    }
    
    private void drawWaveform(GraphicsContext gc, double width, double height) {
        gc.setStroke(primaryColor);
        gc.setLineWidth(2);
        
        // 主波形
        gc.beginPath();
        gc.moveTo(0, height / 2);
        for (int i = 0; i < spectrumData.length && i < width; i++) {
            double x = i * width / spectrumData.length;
            double y = height / 2 + (spectrumData[i] - 0.5) * height * 0.8;
            gc.lineTo(x, y);
        }
        gc.stroke();
        
        // 镜像波形
        gc.setStroke(secondaryColor);
        gc.setLineWidth(1.5);
        gc.beginPath();
        gc.moveTo(0, height / 2);
        for (int i = 0; i < spectrumData.length && i < width; i++) {
            double x = i * width / spectrumData.length;
            double y = height / 2 - (spectrumData[i] - 0.5) * height * 0.6;
            gc.lineTo(x, y);
        }
        gc.stroke();
    }
    
    private void drawCircular(GraphicsContext gc, double width, double height) {
        double cx = width / 2;
        double cy = height / 2;
        double baseRadius = Math.min(width, height) * 0.25;
        double maxRadius = Math.min(width, height) * 0.45;
        
        int segments = Math.min(64, spectrumData.length);
        
        // 绘制外圈
        for (int i = 0; i < segments; i++) {
            double angle = (i / (double) segments) * Math.PI * 2 - Math.PI / 2;
            double value = spectrumData[i];
            double r = baseRadius + value * (maxRadius - baseRadius);
            
            double x1 = cx + Math.cos(angle) * baseRadius;
            double y1 = cy + Math.sin(angle) * baseRadius;
            double x2 = cx + Math.cos(angle) * r;
            double y2 = cy + Math.sin(angle) * r;
            
            double hue = 200 + value * 80;
            gc.setStroke(Color.hsb(hue, 0.8, 0.9, 0.9));
            gc.setLineWidth(3);
            gc.strokeLine(x1, y1, x2, y2);
            
            // 端点发光
            gc.setFill(Color.hsb(hue, 1.0, 1.0, value));
            gc.fillOval(x2 - 3, y2 - 3, 6, 6);
        }
        
        // 绘制中心圆
        double avg = Arrays.stream(spectrumData).average().orElse(0);
        gc.setFill(primaryColor.deriveColor(0, 1, 1, avg));
        gc.fillOval(cx - baseRadius * 0.5, cy - baseRadius * 0.5, baseRadius, baseRadius);
        
        // 中心文字（可选）
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(12));
    }
    
    private void drawParticles(GraphicsContext gc, double width, double height) {
        double avgEnergy = Arrays.stream(spectrumData).average().orElse(0);
        
        for (Particle p : particles) {
            // 根据音频能量调整粒子
            double energyFactor = avgEnergy * 2;
            double size = p.size * (1 + energyFactor);
            
            double hue = p.hue + energyFactor * 50;
            Color color = Color.hsb(hue % 360, 0.8, 0.9, p.alpha * energyFactor);
            
            gc.setFill(color);
            gc.fillOval(p.x - size / 2, p.y - size / 2, size, size);
        }
        
        // 连接相近粒子
        gc.setStroke(primaryColor.deriveColor(0, 1, 1, 0.2));
        gc.setLineWidth(0.5);
        for (int i = 0; i < particles.length; i++) {
            for (int j = i + 1; j < particles.length; j++) {
                double dist = Math.hypot(particles[i].x - particles[j].x, 
                                         particles[i].y - particles[j].y);
                if (dist < 50 && avgEnergy > 0.3) {
                    gc.strokeLine(particles[i].x, particles[i].y, 
                                 particles[j].x, particles[j].y);
                }
            }
        }
    }
    
    private void drawWaterfall(GraphicsContext gc, double width, double height) {
        int cols = Math.min(64, spectrumData.length);
        double cellWidth = width / cols;
        double cellHeight = height / waterfallRows;
        
        for (int row = 0; row < waterfallRows; row++) {
            int dataIndex = (waterfallIndex + row) % waterfallRows;
            double[] rowSpectrum = waterfallData[dataIndex];
            
            for (int col = 0; col < cols && col < rowSpectrum.length; col++) {
                double value = rowSpectrum[col];
                double hue = 280 - value * 80;
                double brightness = 0.3 + value * 0.7;
                
                Color color = Color.hsb(hue, 0.9, brightness, value + 0.1);
                gc.setFill(color);
                gc.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
            }
        }
    }
    
    public void setVisualizerMode(int mode) {
        this.visualizerMode = mode % 5;
    }
    
    public int getVisualizerMode() {
        return visualizerMode;
    }
    
    public void nextVisualizerMode() {
        setVisualizerMode(visualizerMode + 1);
    }
    
    public void setColors(Color primary, Color secondary) {
        this.primaryColor = primary;
        this.secondaryColor = secondary;
    }
    
    public void setSmoothTransition(boolean smooth) {
        this.smoothTransition = smooth;
    }
    
    public void setGlowIntensity(double intensity) {
        this.glowIntensity = intensity;
    }
    
    public String[] getVisualizerNames() {
        return new String[]{"频谱分析", "波形显示", "圆形可视化", "粒子效果", "瀑布图"};
    }
    
    // 粒子类
    private class Particle {
        double x, y;
        double vx, vy;
        double size;
        double alpha;
        double hue;
        
        Particle() {
            reset();
        }
        
        void reset() {
            x = Math.random() * 800;
            y = Math.random() * 100;
            vx = (Math.random() - 0.5) * 2;
            vy = (Math.random() - 0.5) * 2;
            size = 2 + Math.random() * 4;
            alpha = 0.3 + Math.random() * 0.7;
            hue = Math.random() * 360;
        }
        
        void update(double energy) {
            x += vx * (1 + energy);
            y += vy * (1 + energy);
            
            // 边界反弹
            if (x < 0 || x > 800) vx = -vx;
            if (y < 0 || y > 100) vy = -vy;
            
            // 根据能量调整
            if (energy > 0.5) {
                vx += (Math.random() - 0.5) * energy;
                vy += (Math.random() - 0.5) * energy;
            }
            
            // 限制速度
            vx = Math.max(-3, Math.min(3, vx));
            vy = Math.max(-3, Math.min(3, vy));
        }
    }
}