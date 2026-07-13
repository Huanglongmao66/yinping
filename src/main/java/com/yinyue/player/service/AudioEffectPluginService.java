package com.yinyue.player.service;

import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;

public class AudioEffectPluginService {
    private static AudioEffectPluginService instance;
    private MediaPlayer mediaPlayer;
    private boolean reverbEnabled = false;
    private boolean bassBoostEnabled = false;
    private boolean surroundEnabled = false;
    private double bassBoostGain = 6.0;
    private double reverbMix = 0.3;

    public static AudioEffectPluginService getInstance() {
        if (instance == null) instance = new AudioEffectPluginService();
        return instance;
    }

    private AudioEffectPluginService() {}

    public void setMediaPlayer(MediaPlayer player) {
        this.mediaPlayer = player;
    }

    public void setReverb(boolean enabled) {
        this.reverbEnabled = enabled;
        applyEffects();
    }

    public void setBassBoost(boolean enabled) {
        this.bassBoostEnabled = enabled;
        applyEffects();
    }

    public void setSurround(boolean enabled) {
        this.surroundEnabled = enabled;
        applyEffects();
    }

    public void setBassBoostGain(double gain) {
        this.bassBoostGain = gain;
        if (bassBoostEnabled) applyEffects();
    }

    public void setReverbMix(double mix) {
        this.reverbMix = mix;
        if (reverbEnabled) applyEffects();
    }

    private void applyEffects() {
        if (mediaPlayer == null) return;
        AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
        if (eq == null) return;

        double[] gains = new double[10];

        if (bassBoostEnabled) {
            gains[0] = bassBoostGain;
            gains[1] = bassBoostGain * 0.8;
            gains[2] = bassBoostGain * 0.5;
        }

        if (reverbEnabled) {
            double mix = reverbMix;
            gains[3] += mix * 3;
            gains[4] += mix * 2;
            gains[5] += mix * 2;
            gains[6] += mix * 1.5;
        }

        if (surroundEnabled) {
            gains[7] += 3;
            gains[8] += 2;
            gains[9] += 1;
        }

        for (int i = 0; i < 10; i++) {
            EqualizerBand band = eq.getBands().get(i);
            if (band != null) {
                band.setGain(gains[i]);
            }
        }
    }

    public void resetAll() {
        reverbEnabled = false;
        bassBoostEnabled = false;
        surroundEnabled = false;
        if (mediaPlayer != null) {
            AudioEqualizer eq = mediaPlayer.getAudioEqualizer();
            if (eq != null) {
                for (int i = 0; i < eq.getBands().size(); i++) {
                    eq.getBands().get(i).setGain(0);
                }
            }
        }
    }

    public boolean isReverbEnabled() { return reverbEnabled; }
    public boolean isBassBoostEnabled() { return bassBoostEnabled; }
    public boolean isSurroundEnabled() { return surroundEnabled; }
    public double getBassBoostGain() { return bassBoostGain; }
    public double getReverbMix() { return reverbMix; }
}
