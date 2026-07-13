package com.yinyue.player.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yinyue.player.model.Song;
import com.yinyue.player.util.AudioUtils;
import com.yinyue.player.util.ConfigManager;
import com.yinyue.player.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LibraryService {
    private static LibraryService instance;

    private final Gson gson;
    private final List<Song> songs;
    private final Map<String, List<Song>> songsByArtist;
    private final Map<String, List<Song>> songsByAlbum;

    public static LibraryService getInstance() {
        if (instance == null) {
            instance = new LibraryService();
        }
        return instance;
    }

    private LibraryService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        songs = new ArrayList<>();
        songsByArtist = new HashMap<>();
        songsByAlbum = new HashMap<>();
        loadLibrary();
    }

    public void scanDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        List<Song> foundSongs = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            List<Path> audioFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> AudioUtils.isSupportedAudioFormat(p.getFileName().toString()))
                    .collect(Collectors.toList());

            for (Path audioPath : audioFiles) {
                Song song = createSongFromFile(audioPath.toFile());
                if (song != null) {
                    foundSongs.add(song);
                }
            }
        } catch (Exception e) {
            // ignore
        }

        addSongs(foundSongs);
    }

    private Song createSongFromFile(File file) {
        try {
            Song song = new Song();
            song.setId(UUID.randomUUID().toString());
            song.setFilePath(file.getAbsolutePath());
            song.setFileSize(file.length());
            song.setFormat(AudioUtils.getFileExtension(file.getName()));
            song.setAddTime(new Date());

            String baseName = FileUtils.getFileBaseName(file.getName());
            song.setTitle(baseName);

            return song;
        } catch (Exception e) {
            return null;
        }
    }

    public void addSong(Song song) {
        if (!songs.contains(song)) {
            songs.add(song);
            updateIndices();
            saveLibrary();
        }
    }

    public void addSongs(List<Song> newSongs) {
        for (Song song : newSongs) {
            if (!songs.contains(song)) {
                songs.add(song);
            }
        }
        updateIndices();
        saveLibrary();
    }

    public void removeSong(Song song) {
        songs.remove(song);
        updateIndices();
        saveLibrary();
    }

    public void removeSong(int index) {
        if (index >= 0 && index < songs.size()) {
            songs.remove(index);
            updateIndices();
            saveLibrary();
        }
    }

    public List<Song> getSongs() {
        return songs;
    }

    public List<Song> searchSongs(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return new ArrayList<>(songs);
        }

        String lowerKeyword = keyword.toLowerCase();
        return songs.stream()
                .filter(song ->
                        (song.getTitle() != null && song.getTitle().toLowerCase().contains(lowerKeyword)) ||
                        (song.getArtist() != null && song.getArtist().toLowerCase().contains(lowerKeyword)) ||
                        (song.getAlbum() != null && song.getAlbum().toLowerCase().contains(lowerKeyword)) ||
                        (song.getFileName() != null && song.getFileName().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }

    public List<String> getAllArtists() {
        return new ArrayList<>(songsByArtist.keySet());
    }

    public List<Song> getSongsByArtist(String artist) {
        return songsByArtist.getOrDefault(artist, new ArrayList<>());
    }

    public List<String> getAllAlbums() {
        return new ArrayList<>(songsByAlbum.keySet());
    }

    public List<Song> getSongsByAlbum(String album) {
        return songsByAlbum.getOrDefault(album, new ArrayList<>());
    }

    public void refreshLibrary() {
        songs.clear();
        songsByArtist.clear();
        songsByAlbum.clear();

        List<String> scanPaths = ConfigManager.getInstance().getScanPaths();
        for (String path : scanPaths) {
            scanDirectory(path);
        }

        saveLibrary();
    }

    public void addScanPath(String path) {
        ConfigManager config = ConfigManager.getInstance();
        List<String> scanPaths = new ArrayList<>(config.getScanPaths());
        if (!scanPaths.contains(path)) {
            scanPaths.add(path);
            config.setScanPaths(scanPaths);
            scanDirectory(path);
        }
    }

    public void removeScanPath(String path) {
        ConfigManager config = ConfigManager.getInstance();
        List<String> scanPaths = new ArrayList<>(config.getScanPaths());
        scanPaths.remove(path);
        config.setScanPaths(scanPaths);
    }

    public List<String> getScanPaths() {
        return ConfigManager.getInstance().getScanPaths();
    }

    private void updateIndices() {
        songsByArtist.clear();
        songsByAlbum.clear();

        for (Song song : songs) {
            String artist = song.getArtist() != null && !song.getArtist().isEmpty() ? song.getArtist() : "未知艺术家";
            String album = song.getAlbum() != null && !song.getAlbum().isEmpty() ? song.getAlbum() : "未知专辑";

            songsByArtist.computeIfAbsent(artist, k -> new ArrayList<>()).add(song);
            songsByAlbum.computeIfAbsent(album, k -> new ArrayList<>()).add(song);
        }
    }

    private void loadLibrary() {
        File libraryFile = new File(FileUtils.getLibraryFilePath());
        if (!libraryFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(libraryFile)) {
            Song[] loadedSongs = gson.fromJson(reader, Song[].class);
            if (loadedSongs != null) {
                songs.addAll(Arrays.asList(loadedSongs));
                updateIndices();
            }
        } catch (Exception e) {
            // ignore
        }

        if (ConfigManager.getInstance().isAutoScan()) {
            refreshLibrary();
        }
    }

    private void saveLibrary() {
        FileUtils.ensureDirectoryExists(FileUtils.getConfigDirectory());
        try (Writer writer = new FileWriter(FileUtils.getLibraryFilePath())) {
            gson.toJson(songs, writer);
        } catch (Exception e) {
            // ignore
        }
    }

    public int getSongCount() {
        return songs.size();
    }

    public long getTotalDuration() {
        return songs.stream().mapToLong(Song::getDuration).sum();
    }
}