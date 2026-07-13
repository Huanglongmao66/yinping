package com.yinyue.player.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yinyue.player.model.Song;
import com.yinyue.player.util.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlaylistSnapshotService {
    private static PlaylistSnapshotService instance;
    private final Gson gson;
    private static final String SNAPSHOTS_DIR = FileUtils.getConfigDirectory() + "/snapshots";

    public static PlaylistSnapshotService getInstance() {
        if (instance == null) instance = new PlaylistSnapshotService();
        return instance;
    }

    private PlaylistSnapshotService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        new File(SNAPSHOTS_DIR).mkdirs();
    }

    public void saveSnapshot(String name) {
        PlaylistService ps = PlaylistService.getInstance();
        AudioPlayerService aps = AudioPlayerService.getInstance();

        SnapshotData data = new SnapshotData();
        data.name = name;
        data.timestamp = new Date();
        data.currentIndex = ps.getCurrentIndex();
        data.position = aps.getCurrentTime();
        data.volume = aps.getVolume();
        data.songs = new ArrayList<>();
        
        if (ps.getCurrentPlaylist() != null) {
            for (Song song : ps.getCurrentPlaylist().getSongs()) {
                data.songs.add(song.getFilePath());
            }
        }

        String filename = name.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + System.currentTimeMillis() + ".json";
        try (Writer writer = new FileWriter(SNAPSHOTS_DIR + "/" + filename)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            // ignore
        }
    }

    public void restoreSnapshot(String filename) {
        try (Reader reader = new FileReader(SNAPSHOTS_DIR + "/" + filename)) {
            SnapshotData data = gson.fromJson(reader, SnapshotData.class);
            if (data == null) return;

            PlaylistService ps = PlaylistService.getInstance();
            AudioPlayerService aps = AudioPlayerService.getInstance();

            ps.clearCurrentPlaylist();
            LibraryService library = LibraryService.getInstance();
            
            for (String path : data.songs) {
                File f = new File(path);
                if (f.exists()) {
                    Song song = new Song();
                    song.setId(java.util.UUID.randomUUID().toString());
                    song.setFilePath(path);
                    song.setFileSize(f.length());
                    song.setTitle(f.getName().replaceAll("\\.[^.]+$", ""));
                    ps.addSong(song);
                }
            }

            if (data.currentIndex >= 0 && data.currentIndex < data.songs.size()) {
                ps.playSong(data.currentIndex);
                aps.setVolume(data.volume);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public List<SnapshotInfo> listSnapshots() {
        List<SnapshotInfo> list = new ArrayList<>();
        File dir = new File(SNAPSHOTS_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return list;
        for (File f : files) {
            SnapshotInfo info = new SnapshotInfo();
            info.filename = f.getName();
            info.modified = new Date(f.lastModified());
            list.add(info);
        }
        return list;
    }

    public void deleteSnapshot(String filename) {
        new File(SNAPSHOTS_DIR + "/" + filename).delete();
    }

    private static class SnapshotData {
        String name;
        Date timestamp;
        int currentIndex;
        long position;
        double volume;
        List<String> songs;
    }

    public static class SnapshotInfo {
        public String filename;
        public Date modified;
    }
}