package com.yinyue.player.model;

import java.util.Date;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String album;
    private int trackNumber;
    private long duration;
    private String filePath;
    private long fileSize;
    private String format;
    private int bitRate;
    private int sampleRate;
    private int channels;
    private byte[] albumArt;
    private Date addTime;
    private int playCount;
    private double replayGain;

    public Song() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDisplayTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return getFileName();
    }

    public String getFileName() {
        if (filePath != null) {
            int lastSeparator = filePath.lastIndexOf(System.getProperty("file.separator"));
            int lastDot = filePath.lastIndexOf('.');
            if (lastSeparator >= 0 && lastDot > lastSeparator) {
                return filePath.substring(lastSeparator + 1, lastDot);
            } else if (lastSeparator >= 0) {
                return filePath.substring(lastSeparator + 1);
            }
        }
        return "未知歌曲";
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public byte[] getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(byte[] albumArt) {
        this.albumArt = albumArt;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public double getReplayGain() {
        return replayGain;
    }

    public void setReplayGain(double replayGain) {
        this.replayGain = replayGain;
    }

    @Override
    public String toString() {
        return title != null ? title : getFileName();
    }
}