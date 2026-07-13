package com.yinyue.player.service;

import com.yinyue.player.util.ConfigManager;

public class AudioEffectService {
    private static AudioEffectService instance;

    private static final int BAND_COUNT = 10;
    private static final float[] FREQUENCIES = {32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f};
    private static final String[] FREQUENCY_LABELS = {"32Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"};

    private final float[] bands;
    private boolean enabled;
    private float playbackSpeed;
    private float pitch;
    private boolean reverbEnabled;

    public static AudioEffectService getInstance() {
        if (instance == null) {
            instance = new AudioEffectService();
        }
        return instance;
    }

    private AudioEffectService() {
        bands = new float[BAND_COUNT];
        ConfigManager config = ConfigManager.getInstance();
        enabled = config.isEqualizerEnabled();
        float[] savedBands = config.getEqualizerBands();
        if (savedBands != null && savedBands.length == BAND_COUNT) {
            System.arraycopy(savedBands, 0, bands, 0, BAND_COUNT);
        }
        playbackSpeed = 1.0f;
        pitch = 0.0f;
    }

    public void setEqualizerBand(int index, float value) {
        if (index >= 0 && index < BAND_COUNT) {
            bands[index] = value;
            saveEqualizerSettings();
        }
    }

    public float getEqualizerBand(int index) {
        if (index >= 0 && index < BAND_COUNT) {
            return bands[index];
        }
        return 0;
    }

    public float[] getEqualizerBands() {
        return bands.clone();
    }

    public void setEqualizerBands(float[] bands) {
        if (bands != null && bands.length == BAND_COUNT) {
            System.arraycopy(bands, 0, this.bands, 0, BAND_COUNT);
            saveEqualizerSettings();
        }
    }

    public boolean isEqualizerEnabled() {
        return enabled;
    }

    public void setEqualizerEnabled(boolean enabled) {
        this.enabled = enabled;
        ConfigManager.getInstance().setEqualizerEnabled(enabled);
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = Math.max(0.5f, Math.min(2.0f, speed));
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(-12f, Math.min(12f, pitch));
    }

    public boolean isReverbEnabled() {
        return reverbEnabled;
    }

    public void setReverbEnabled(boolean enabled) {
        this.reverbEnabled = enabled;
    }

    public int getBandCount() {
        return BAND_COUNT;
    }

    public float[] getFrequencies() {
        return FREQUENCIES.clone();
    }

    public String[] getFrequencyLabels() {
        return FREQUENCY_LABELS.clone();
    }

    public void setPreset(Preset preset) {
        switch (preset) {
            case FLAT:
                for (int i = 0; i < BAND_COUNT; i++) {
                    bands[i] = 0;
                }
                break;
            case ROCK:
                bands[0] = 4f; bands[1] = 3f; bands[2] = 1f; bands[3] = -1f;
                bands[4] = -2f; bands[5] = -1f; bands[6] = 2f; bands[7] = 4f;
                bands[8] = 5f; bands[9] = 5f;
                break;
            case POP:
                bands[0] = -1f; bands[1] = 1f; bands[2] = 3f; bands[3] = 4f;
                bands[4] = 4f; bands[5] = 3f; bands[6] = 2f; bands[7] = 1f;
                bands[8] = 0f; bands[9] = -1f;
                break;
            case JAZZ:
                bands[0] = 2f; bands[1] = 3f; bands[2] = 2f; bands[3] = 1f;
                bands[4] = 0f; bands[5] = 1f; bands[6] = 2f; bands[7] = 3f;
                bands[8] = 4f; bands[9] = 4f;
                break;
            case CLASSICAL:
                bands[0] = 3f; bands[1] = 2f; bands[2] = 1f; bands[3] = 0f;
                bands[4] = -1f; bands[5] = 0f; bands[6] = 1f; bands[7] = 2f;
                bands[8] = 3f; bands[9] = 4f;
                break;
            case BASS_BOOST:
                bands[0] = 5f; bands[1] = 5f; bands[2] = 4f; bands[3] = 3f;
                bands[4] = 1f; bands[5] = 0f; bands[6] = -1f; bands[7] = -1f;
                bands[8] = 0f; bands[9] = 0f;
                break;
            case TREBLE_BOOST:
                bands[0] = -2f; bands[1] = -1f; bands[2] = 0f; bands[3] = 1f;
                bands[4] = 2f; bands[5] = 3f; bands[6] = 4f; bands[7] = 5f;
                bands[8] = 5f; bands[9] = 5f;
                break;
            case VOCAL:
                bands[0] = -1f; bands[1] = -1f; bands[2] = 0f; bands[3] = 2f;
                bands[4] = 4f; bands[5] = 5f; bands[6] = 4f; bands[7] = 2f;
                bands[8] = 0f; bands[9] = -1f;
                break;
            case PARTY:
                bands[0] = 4f; bands[1] = 3f; bands[2] = 2f; bands[3] = 1f;
                bands[4] = 2f; bands[5] = 3f; bands[6] = 4f; bands[7] = 3f;
                bands[8] = 2f; bands[9] = 1f;
                break;
        }
        saveEqualizerSettings();
    }

    private void saveEqualizerSettings() {
        ConfigManager.getInstance().setEqualizerBands(bands);
    }

    public enum Preset {
        FLAT("平直"),
        ROCK("摇滚"),
        POP("流行"),
        JAZZ("爵士"),
        CLASSICAL("古典"),
        BASS_BOOST("重低音"),
        TREBLE_BOOST("高音增强"),
        VOCAL("人声"),
        PARTY("派对");

        private final String displayName;

        Preset(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}