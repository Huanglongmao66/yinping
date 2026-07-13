package com.yinyue.player.service;

import java.util.*;

/**
 * 音频效果链服务
 * 支持多种音频效果串联应用
 */
public class AudioEffectChainService {
    private static AudioEffectChainService instance;
    
    private List<AudioEffect> effectChain;
    private boolean isEnabled;
    
    public static AudioEffectChainService getInstance() {
        if (instance == null) {
            instance = new AudioEffectChainService();
        }
        return instance;
    }
    
    private AudioEffectChainService() {
        effectChain = new ArrayList<>();
        isEnabled = true;
        
        initDefaultEffects();
    }
    
    private void initDefaultEffects() {
        effectChain.add(new EqualizerEffect());
        effectChain.add(new ReverbEffect());
        effectChain.add(new BassBoostEffect());
    }
    
    public void initialize(Object player) {
        // 简化实现，不直接操作MediaPlayer
    }
    
    private void processSpectrum(float[] magnitudes) {
        if (!isEnabled || effectChain.isEmpty()) return;
        
        for (AudioEffect effect : effectChain) {
            if (effect.isEnabled()) {
                effect.process(magnitudes);
            }
        }
    }
    
    public void addEffect(AudioEffect effect) {
        effectChain.add(effect);
    }
    
    public void removeEffect(AudioEffect effect) {
        effectChain.remove(effect);
    }
    
    public void removeEffectByType(String effectType) {
        effectChain.removeIf(e -> e.getType().equals(effectType));
    }
    
    public List<AudioEffect> getEffects() {
        return new ArrayList<>(effectChain);
    }
    
    public AudioEffect getEffectByType(String effectType) {
        for (AudioEffect effect : effectChain) {
            if (effect.getType().equals(effectType)) {
                return effect;
            }
        }
        return null;
    }
    
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public void setEffectEnabled(String effectType, boolean enabled) {
        AudioEffect effect = getEffectByType(effectType);
        if (effect != null) {
            effect.setEnabled(enabled);
        }
    }
    
    private void applyEffects() {
        // 简化实现
    }
    
    private void resetEffects() {
        // 简化实现
    }
    
    public void setEffectParameter(String effectType, String parameter, double value) {
        AudioEffect effect = getEffectByType(effectType);
        if (effect != null) {
            effect.setParameter(parameter, value);
        }
    }
    
    public double getEffectParameter(String effectType, String parameter) {
        AudioEffect effect = getEffectByType(effectType);
        return effect != null ? effect.getParameter(parameter) : 0;
    }
    
    public void clearEffects() {
        effectChain.clear();
    }
    
    public List<EffectInfo> getEffectInfoList() {
        List<EffectInfo> infoList = new ArrayList<>();
        for (AudioEffect effect : effectChain) {
            infoList.add(effect.getInfo());
        }
        return infoList;
    }
    
    // 音频效果基类
    public static abstract class AudioEffect {
        protected boolean enabled;
        protected Map<String, Double> parameters;
        
        public AudioEffect() {
            enabled = true;
            parameters = new HashMap<>();
        }
        
        public abstract String getType();
        public abstract String getName();
        public abstract void apply(Object equalizer);
        public abstract void process(float[] magnitudes);
        public abstract EffectInfo getInfo();
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public double getParameter(String key) {
            return parameters.getOrDefault(key, 0.0);
        }
        
        public void setParameter(String key, double value) {
            parameters.put(key, value);
        }
    }
    
    // 均衡器效果
    public static class EqualizerEffect extends AudioEffect {
        private static final double[] FREQUENCIES = {31.25, 62.5, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
        
        public EqualizerEffect() {
            for (int i = 0; i < FREQUENCIES.length; i++) {
                setParameter("band_" + i, 0);
            }
        }
        
        @Override
        public String getType() { return "equalizer"; }
        
        @Override
        public String getName() { return "均衡器"; }
        
        @Override
        public void apply(Object equalizer) {
            // 简化实现
        }
        
        @Override
        public void process(float[] magnitudes) {
        }
        
        @Override
        public EffectInfo getInfo() {
            EffectInfo info = new EffectInfo();
            info.type = getType();
            info.name = getName();
            info.enabled = isEnabled();
            info.parameters = new HashMap<>();
            for (int i = 0; i < FREQUENCIES.length; i++) {
                info.parameters.put("band_" + i + " (" + FREQUENCIES[i] + "Hz)", 
                                    getParameter("band_" + i));
            }
            return info;
        }
    }
    
    // 混响效果
    public static class ReverbEffect extends AudioEffect {
        public ReverbEffect() {
            setParameter("amount", 0.3);
            setParameter("delay", 100);
        }
        
        @Override
        public String getType() { return "reverb"; }
        
        @Override
        public String getName() { return "混响"; }
        
        @Override
        public void apply(Object equalizer) {
        }
        
        @Override
        public void process(float[] magnitudes) {
        }
        
        @Override
        public EffectInfo getInfo() {
            EffectInfo info = new EffectInfo();
            info.type = getType();
            info.name = getName();
            info.enabled = isEnabled();
            info.parameters.put("amount", getParameter("amount"));
            info.parameters.put("delay", getParameter("delay"));
            return info;
        }
    }
    
    // 低音增强效果
    public static class BassBoostEffect extends AudioEffect {
        public BassBoostEffect() {
            setParameter("boost", 0);
        }
        
        @Override
        public String getType() { return "bass_boost"; }
        
        @Override
        public String getName() { return "低音增强"; }
        
        @Override
        public void apply(Object equalizer) {
        }
        
        @Override
        public void process(float[] magnitudes) {
        }
        
        @Override
        public EffectInfo getInfo() {
            EffectInfo info = new EffectInfo();
            info.type = getType();
            info.name = getName();
            info.enabled = isEnabled();
            info.parameters.put("boost", getParameter("boost"));
            return info;
        }
    }
    
    // 高音增强效果
    public static class TrebleBoostEffect extends AudioEffect {
        public TrebleBoostEffect() {
            setParameter("boost", 0);
        }
        
        @Override
        public String getType() { return "treble_boost"; }
        
        @Override
        public String getName() { return "高音增强"; }
        
        @Override
        public void apply(Object equalizer) {
        }
        
        @Override
        public void process(float[] magnitudes) {
        }
        
        @Override
        public EffectInfo getInfo() {
            EffectInfo info = new EffectInfo();
            info.type = getType();
            info.name = getName();
            info.enabled = isEnabled();
            info.parameters.put("boost", getParameter("boost"));
            return info;
        }
    }
    
    // 效果信息类
    public static class EffectInfo {
        public String type;
        public String name;
        public boolean enabled;
        public Map<String, Double> parameters = new HashMap<>();
    }
}