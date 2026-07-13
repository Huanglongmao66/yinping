package com.yinyue.player.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private List<Song> songs;
    private Date createTime;
    private Date updateTime;
    private String coverImage;
    private String description;

    public Playlist() {
        this.songs = new ArrayList<>();
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    public Playlist(String name) {
        this();
        this.name = name;
        this.id = java.util.UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updateTime = new Date();
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        this.updateTime = new Date();
    }

    public void addSong(Song song) {
        this.songs.add(song);
        this.updateTime = new Date();
    }

    public void addSong(int index, Song song) {
        this.songs.add(index, song);
        this.updateTime = new Date();
    }

    public void removeSong(Song song) {
        this.songs.remove(song);
        this.updateTime = new Date();
    }

    public void removeSong(int index) {
        this.songs.remove(index);
        this.updateTime = new Date();
    }

    public void clear() {
        this.songs.clear();
        this.updateTime = new Date();
    }

    public void shuffle() {
        Collections.shuffle(songs);
        this.updateTime = new Date();
    }

    public long getTotalDuration() {
        return songs.stream().mapToLong(Song::getDuration).sum();
    }

    public int getSongCount() {
        return songs.size();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name != null ? name : "未命名播放列表";
    }
}