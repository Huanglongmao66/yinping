package com.yinyue.player.model;

import java.util.Date;

public class HistoryEntry {
    private String songId;
    private String title;
    private String artist;
    private String filePath;
    private long playTime;
    private Date playedAt;
    private long durationPlayed;

    public HistoryEntry() {}

    public HistoryEntry(Song song) {
        this.songId = song.getId();
        this.title = song.getDisplayTitle();
        this.artist = song.getArtist();
        this.filePath = song.getFilePath();
        this.playTime = 0;
        this.playedAt = new Date();
        this.durationPlayed = 0;
    }

    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getPlayTime() { return playTime; }
    public void setPlayTime(long playTime) { this.playTime = playTime; }

    public Date getPlayedAt() { return playedAt; }
    public void setPlayedAt(Date playedAt) { this.playedAt = playedAt; }

    public long getDurationPlayed() { return durationPlayed; }
    public void setDurationPlayed(long durationPlayed) { this.durationPlayed = durationPlayed; }

    public String getDisplayText() {
        if (artist != null && !artist.isEmpty()) {
            return title + " - " + artist;
        }
        return title;
    }
}
