package com.yinyue.player.model;

public class AudioInfo {
    private String format;
    private long duration;
    private int sampleRate;
    private int bitRate;
    private int channels;
    private int bitsPerSample;
    private String encoding;

    public AudioInfo() {}

    public AudioInfo(String format, long duration, int sampleRate, int bitRate, int channels) {
        this.format = format;
        this.duration = duration;
        this.sampleRate = sampleRate;
        this.bitRate = bitRate;
        this.channels = channels;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public void setBitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}