package com.yinyue.player.util;

import javafx.scene.media.MediaPlayer;
import java.util.*;

/**
 * 音频混音工具
 * 支持多音轨混合、音量调节、效果叠加
 */
public class AudioMixer {
    private List<MixerTrack> tracks;
    private double masterVolume;
    private boolean isMuted;
    
    public AudioMixer() {
        tracks = new ArrayList<>();
        masterVolume = 1.0;
        isMuted = false;
    }
    
    // 添加音轨
    public void addTrack(MediaPlayer player, String name, double volume) {
        MixerTrack track = new MixerTrack(player, name, volume);
        tracks.add(track);
        updateTrackVolume(track);
    }
    
    // 移除音轨
    public void removeTrack(String name) {
        tracks.removeIf(t -> t.name.equals(name));
    }
    
    // 获取音轨
    public MixerTrack getTrack(String name) {
        for (MixerTrack track : tracks) {
            if (track.name.equals(name)) return track;
        }
        return null;
    }
    
    // 获取所有音轨
    public List<MixerTrack> getAllTracks() {
        return new ArrayList<>(tracks);
    }
    
    // 设置音轨音量
    public void setTrackVolume(String name, double volume) {
        MixerTrack track = getTrack(name);
        if (track != null) {
            track.volume = Math.max(0, Math.min(1, volume));
            updateTrackVolume(track);
        }
    }
    
    // 设置主音量
    public void setMasterVolume(double volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));
        for (MixerTrack track : tracks) {
            updateTrackVolume(track);
        }
    }
    
    public double getMasterVolume() {
        return masterVolume;
    }
    
    // 更新音轨实际音量
    private void updateTrackVolume(MixerTrack track) {
        if (track.player != null) {
            double actualVolume = isMuted ? 0 : track.volume * masterVolume;
            track.player.setVolume(actualVolume);
        }
    }
    
    // 静音/取消静音
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        for (MixerTrack track : tracks) {
            updateTrackVolume(track);
        }
    }
    
    public boolean isMuted() {
        return isMuted;
    }
    
    // 静音单个音轨
    public void setTrackMuted(String name, boolean muted) {
        MixerTrack track = getTrack(name);
        if (track != null) {
            track.muted = muted;
            if (track.player != null) {
                double actualVolume = muted || isMuted ? 0 : track.volume * masterVolume;
                track.player.setVolume(actualVolume);
            }
        }
    }
    
    // 独奏（只播放该音轨）
    public void soloTrack(String name) {
        for (MixerTrack track : tracks) {
            if (track.name.equals(name)) {
                track.solo = true;
                if (track.player != null) {
                    track.player.setVolume(isMuted ? 0 : track.volume * masterVolume);
                }
            } else {
                track.solo = false;
                if (track.player != null) {
                    track.player.setVolume(0);
                }
            }
        }
    }
    
    // 取消独奏
    public void cancelSolo() {
        for (MixerTrack track : tracks) {
            track.solo = false;
            updateTrackVolume(track);
        }
    }
    
    // 播放所有音轨
    public void playAll() {
        for (MixerTrack track : tracks) {
            if (track.player != null) {
                track.player.play();
            }
        }
    }
    
    // 暂停所有音轨
    public void pauseAll() {
        for (MixerTrack track : tracks) {
            if (track.player != null) {
                track.player.pause();
            }
        }
    }
    
    // 停止所有音轨
    public void stopAll() {
        for (MixerTrack track : tracks) {
            if (track.player != null) {
                track.player.stop();
            }
        }
    }
    
    // 同步所有音轨到指定时间
    public void seekAll(double seconds) {
        for (MixerTrack track : tracks) {
            if (track.player != null) {
                track.player.seek(javafx.util.Duration.seconds(seconds));
            }
        }
    }
    
    // 设置所有音轨播放速度
    public void setSpeedAll(double speed) {
        for (MixerTrack track : tracks) {
            if (track.player != null) {
                track.player.setRate(speed);
            }
        }
    }
    
    // 添加效果到音轨
    public void addEffectToTrack(String name, String effectName, double value) {
        MixerTrack track = getTrack(name);
        if (track != null) {
            track.effects.put(effectName, value);
        }
    }
    
    // 移除音轨效果
    public void removeEffectFromTrack(String name, String effectName) {
        MixerTrack track = getTrack(name);
        if (track != null) {
            track.effects.remove(effectName);
        }
    }
    
    // 获取音轨效果值
    public double getTrackEffect(String name, String effectName) {
        MixerTrack track = getTrack(name);
        if (track != null && track.effects.containsKey(effectName)) {
            return track.effects.get(effectName);
        }
        return 0;
    }
    
    // 获取音轨数量
    public int getTrackCount() {
        return tracks.size();
    }
    
    // 音轨类
    public static class MixerTrack {
        MediaPlayer player;
        String name;
        double volume;
        boolean muted;
        boolean solo;
        Map<String, Double> effects;
        
        public MixerTrack(MediaPlayer player, String name, double volume) {
            this.player = player;
            this.name = name;
            this.volume = volume;
            this.muted = false;
            this.solo = false;
            this.effects = new HashMap<>();
        }
        
        public MediaPlayer getPlayer() { return player; }
        public String getName() { return name; }
        public double getVolume() { return volume; }
        public boolean isMuted() { return muted; }
        public boolean isSolo() { return solo; }
        public Map<String, Double> getEffects() { return effects; }
    }
}