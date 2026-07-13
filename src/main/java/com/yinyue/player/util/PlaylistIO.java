package com.yinyue.player.util;

import com.yinyue.player.model.Playlist;
import com.yinyue.player.model.Song;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistIO {
    public static void exportM3U(Playlist playlist, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath), "UTF-8"))) {
            writer.write("#EXTM3U\n");
            for (Song song : playlist.getSongs()) {
                writer.write("#EXTINF:" + (song.getDuration() / 1000) + "," +
                        song.getDisplayTitle() + " - " + (song.getArtist() != null ? song.getArtist() : "") + "\n");
                writer.write(song.getFilePath() + "\n");
            }
        }
    }

    public static void exportPLS(Playlist playlist, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath), "UTF-8"))) {
            writer.write("[playlist]\n");
            List<Song> songs = playlist.getSongs();
            for (int i = 0; i < songs.size(); i++) {
                Song song = songs.get(i);
                writer.write("File" + (i + 1) + "=" + song.getFilePath() + "\n");
                writer.write("Title" + (i + 1) + "=" + song.getDisplayTitle() + "\n");
                writer.write("Length" + (i + 1) + "=" + (song.getDuration() / 1000) + "\n");
            }
            writer.write("NumberOfEntries=" + songs.size() + "\n");
            writer.write("Version=2\n");
        }
    }

    public static List<String> importM3U(String filePath) throws IOException {
        List<String> paths = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                paths.add(line);
            }
        }
        return paths;
    }

    public static List<String> importPLS(String filePath) throws IOException {
        List<String> paths = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("File")) {
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        paths.add(line.substring(eqIdx + 1));
                    }
                }
            }
        }
        return paths;
    }
}
