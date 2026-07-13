package com.yinyue.player.service;

import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;

public class KaraokeService {
    private static KaraokeService instance;
    private boolean enabled = false;
    private double vocalReduction = 0.8;
    private MediaPlayer mediaPlayer;

    public static KaraokeService getInstance() {
        if (instance == null) instance = new KaraokeService();
        return instance;
    }

    private KaraokeService() {}

    public void setMediaPlayer(MediaPlayer player) {
        this.mediaPlayer = player;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        applyKaraokeEffect();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVocalReduction(double reduction) {
        this.vocalReduction = Math.max(0, Math.min(1, reduction));
        if (enabled) applyKaraokeEffect();
    }

    public double getVocalReduction() {
        return vocalReduction;
    }

    private void applyKaraokeEffect() {
        if (mediaPlayer == null) return;
        AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
        if (eq == null) return;

        if (enabled) {
            // Reduce mid frequencies where vocals typically are
            double reduction = -12 * vocalReduction;
            for (int i = 2; i <= 7; i++) {
                EqualizerBand band = eq.getBands().get(i);
                if (band != null) {
                    band.setGain(reduction);
                }
            }
        } else {
            for (int i = 0; i < 10; i++) {
                EqualizerBand band = eq.getBands().get(i);
                if (band != null) {
                    band.setGain(0);
                }
            }
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }
}