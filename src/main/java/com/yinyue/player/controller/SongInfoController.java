package com.yinyue.player.controller;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.AudioUtils;
import com.yinyue.player.util.FileUtils;
import com.yinyue.player.util.LrcParser;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class SongInfoController {
    @FXML
    private TextField titleField;

    @FXML
    private TextField artistField;

    @FXML
    private TextField albumField;

    @FXML
    private Label durationLabel;

    @FXML
    private Label fileSizeLabel;

    @FXML
    private Label formatLabel;

    @FXML
    private Label bitRateLabel;

    @FXML
    private Label sampleRateLabel;

    @FXML
    private Label channelsLabel;

    @FXML
    private Label filePathLabel;

    @FXML
    private TextArea lyricsArea;

    @FXML
    private ImageView albumArtView;

    private Song song;
    private Stage stage;

    @FXML
    public void initialize() {
    }

    public void setSong(Song song) {
        this.song = song;
        loadSongInfo();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void loadSongInfo() {
        if (song == null) {
            return;
        }

        titleField.setText(song.getTitle() != null ? song.getTitle() : "");
        artistField.setText(song.getArtist() != null ? song.getArtist() : "");
        albumField.setText(song.getAlbum() != null ? song.getAlbum() : "");

        durationLabel.setText(AudioUtils.formatTime(song.getDuration()));
        fileSizeLabel.setText(AudioUtils.formatFileSize(song.getFileSize()));
        formatLabel.setText(song.getFormat() != null ? song.getFormat().toUpperCase() : "");
        bitRateLabel.setText(AudioUtils.formatBitRate(song.getBitRate()));
        sampleRateLabel.setText(AudioUtils.formatSampleRate(song.getSampleRate()));
        channelsLabel.setText(AudioUtils.formatChannels(song.getChannels()));
        filePathLabel.setText(song.getFilePath() != null ? song.getFilePath() : "");

        loadLyrics();
    }

    private void loadLyrics() {
        if (song == null || song.getFilePath() == null) {
            lyricsArea.setText("未找到歌词文件");
            return;
        }

        String lrcPath = song.getFilePath().substring(0, song.getFilePath().lastIndexOf('.')) + ".lrc";
        File lrcFile = new File(lrcPath);

        if (lrcFile.exists()) {
            LrcParser parser = new LrcParser();
            parser.parse(lrcFile);

            if (parser.hasLyrics()) {
                StringBuilder lyrics = new StringBuilder();
                for (LrcParser.LyricLine line : parser.getLyrics()) {
                    lyrics.append(line.getText()).append("\n");
                }
                lyricsArea.setText(lyrics.toString());
            } else {
                lyricsArea.setText("歌词文件为空");
            }
        } else {
            lyricsArea.setText("未找到歌词文件");
        }
    }

    @FXML
    private void onOpenFileLocation() {
        if (song != null && song.getFilePath() != null) {
            try {
                FileUtils.openFileLocation(song.getFilePath());
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
}