package com.yinyue.player.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yinyue.player.model.PlayMode;
import com.yinyue.player.model.Playlist;
import com.yinyue.player.model.Song;
import com.yinyue.player.util.ConfigManager;
import com.yinyue.player.util.FileUtils;

import java.io.*;
import java.util.*;

public class PlaylistService {
    private static PlaylistService instance;

    private final Gson gson;
    private final List<Playlist> playlists;
    private Playlist currentPlaylist;
    private int currentIndex;
    private PlayMode playMode;
    private final List<Integer> shuffleHistory;

    public static PlaylistService getInstance() {
        if (instance == null) {
            instance = new PlaylistService();
        }
        return instance;
    }

    private PlaylistService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        playlists = new ArrayList<>();
        shuffleHistory = new ArrayList<>();
        playMode = PlayMode.valueOf(ConfigManager.getInstance().getPlayMode());
        loadPlaylists();
    }

    public void addSong(Song song) {
        if (currentPlaylist != null) {
            currentPlaylist.addSong(song);
            saveCurrentPlaylist();
        }
    }

    public void addSongs(List<Song> songs) {
        if (currentPlaylist != null) {
            for (Song song : songs) {
                currentPlaylist.addSong(song);
            }
            saveCurrentPlaylist();
        }
    }

    public void removeSong(int index) {
        if (currentPlaylist != null && index >= 0 && index < currentPlaylist.getSongs().size()) {
            currentPlaylist.removeSong(index);
            if (currentIndex >= currentPlaylist.getSongs().size()) {
                currentIndex = Math.max(0, currentPlaylist.getSongs().size() - 1);
            }
            saveCurrentPlaylist();
        }
    }

    public void removeSong(Song song) {
        if (currentPlaylist != null) {
            int index = currentPlaylist.getSongs().indexOf(song);
            if (index >= 0) {
                removeSong(index);
            }
        }
    }

    public void playSong(int index) {
        if (currentPlaylist != null && index >= 0 && index < currentPlaylist.getSongs().size()) {
            this.currentIndex = index;
            Song song = currentPlaylist.getSongs().get(index);
            AudioPlayerService.getInstance().play(song);
        }
    }

    public void playNext() {
        if (currentPlaylist == null || currentPlaylist.getSongs().isEmpty()) {
            return;
        }

        switch (playMode) {
            case SEQUENCE:
                if (currentIndex < currentPlaylist.getSongs().size() - 1) {
                    currentIndex++;
                } else {
                    return;
                }
                break;
            case REPEAT_ONE:
                break;
            case REPEAT_ALL:
                currentIndex = (currentIndex + 1) % currentPlaylist.getSongs().size();
                break;
            case SHUFFLE:
                currentIndex = getNextShuffleIndex();
                break;
        }

        Song song = currentPlaylist.getSongs().get(currentIndex);
        AudioPlayerService.getInstance().play(song);
    }

    public void playPrevious() {
        if (currentPlaylist == null || currentPlaylist.getSongs().isEmpty()) {
            return;
        }

        switch (playMode) {
            case SEQUENCE:
                if (currentIndex > 0) {
                    currentIndex--;
                } else {
                    return;
                }
                break;
            case REPEAT_ONE:
                break;
            case REPEAT_ALL:
                currentIndex = (currentIndex - 1 + currentPlaylist.getSongs().size()) % currentPlaylist.getSongs().size();
                break;
            case SHUFFLE:
                currentIndex = getPreviousShuffleIndex();
                break;
        }

        Song song = currentPlaylist.getSongs().get(currentIndex);
        AudioPlayerService.getInstance().play(song);
    }

    private int getNextShuffleIndex() {
        if (currentPlaylist == null || currentPlaylist.getSongs().isEmpty()) {
            return 0;
        }

        if (currentPlaylist.getSongs().size() == 1) {
            return 0;
        }

        int newIndex;
        do {
            newIndex = new Random().nextInt(currentPlaylist.getSongs().size());
        } while (newIndex == currentIndex && currentPlaylist.getSongs().size() > 1);

        shuffleHistory.add(currentIndex);
        if (shuffleHistory.size() > 50) {
            shuffleHistory.remove(0);
        }

        return newIndex;
    }

    private int getPreviousShuffleIndex() {
        if (currentPlaylist == null || currentPlaylist.getSongs().isEmpty()) {
            return 0;
        }

        if (!shuffleHistory.isEmpty()) {
            return shuffleHistory.remove(shuffleHistory.size() - 1);
        }

        if (currentIndex > 0) {
            return currentIndex - 1;
        }
        return currentPlaylist.getSongs().size() - 1;
    }

    public void setPlayMode(PlayMode mode) {
        this.playMode = mode;
        ConfigManager.getInstance().setPlayMode(mode.name());
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCurrentPlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;
        this.currentIndex = 0;
        shuffleHistory.clear();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public List<Playlist> getAllPlaylists() {
        return playlists;
    }

    public Playlist createPlaylist(String name) {
        Playlist playlist = new Playlist(name);
        playlists.add(playlist);
        savePlaylist(playlist);
        return playlist;
    }

    public void deletePlaylist(Playlist playlist) {
        playlists.remove(playlist);
        if (currentPlaylist == playlist) {
            currentPlaylist = null;
            currentIndex = 0;
        }
        deletePlaylistFile(playlist.getId());
    }

    public void renamePlaylist(Playlist playlist, String newName) {
        playlist.setName(newName);
        savePlaylist(playlist);
    }

    public void loadPlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;
        this.currentIndex = 0;
        shuffleHistory.clear();
    }

    private void loadPlaylists() {
        File playlistsDir = new File(FileUtils.getPlaylistsDirectory());
        if (!playlistsDir.exists()) {
            return;
        }

        File[] files = playlistsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                Playlist playlist = gson.fromJson(reader, Playlist.class);
                if (playlist != null && playlist.getName() != null) {
                    playlists.add(playlist);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (playlists.isEmpty()) {
            Playlist defaultPlaylist = new Playlist("默认播放列表");
            playlists.add(defaultPlaylist);
            savePlaylist(defaultPlaylist);
        }

        currentPlaylist = playlists.get(0);
    }

    private void savePlaylist(Playlist playlist) {
        FileUtils.ensureDirectoryExists(FileUtils.getPlaylistsDirectory());
        String filePath = FileUtils.getPlaylistsDirectory() + File.separator + playlist.getId() + ".json";
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(playlist, writer);
        } catch (Exception e) {
            // ignore
        }
    }

    private void saveCurrentPlaylist() {
        if (currentPlaylist != null) {
            savePlaylist(currentPlaylist);
        }
    }

    private void deletePlaylistFile(String playlistId) {
        String filePath = FileUtils.getPlaylistsDirectory() + File.separator + playlistId + ".json";
        new File(filePath).delete();
    }

    public void clearCurrentPlaylist() {
        if (currentPlaylist != null) {
            currentPlaylist.clear();
            currentIndex = 0;
            saveCurrentPlaylist();
        }
    }

    public void shuffleCurrentPlaylist() {
        if (currentPlaylist != null) {
            currentPlaylist.shuffle();
            currentIndex = 0;
            saveCurrentPlaylist();
        }
    }
}