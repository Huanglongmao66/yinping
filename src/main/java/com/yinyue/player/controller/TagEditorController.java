package com.yinyue.player.controller;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.DialogUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class TagEditorController {
    @FXML private TextField titleField;
    @FXML private TextField artistField;
    @FXML private TextField albumField;
    @FXML private TextField trackField;
    @FXML private TextField genreField;
    @FXML private TextField yearField;

    private Song song;
    private Stage stage;

    public void setSong(Song song) {
        this.song = song;
        if (song != null) {
            titleField.setText(song.getTitle() != null ? song.getTitle() : "");
            artistField.setText(song.getArtist() != null ? song.getArtist() : "");
            albumField.setText(song.getAlbum() != null ? song.getAlbum() : "");
            trackField.setText(song.getTrackNumber() > 0 ? String.valueOf(song.getTrackNumber()) : "");
            genreField.setText(song.getFormat() != null ? song.getFormat() : "");
            yearField.setText("");
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void onSave() {
        if (song != null) {
            song.setTitle(titleField.getText());
            song.setArtist(artistField.getText());
            song.setAlbum(albumField.getText());
            try {
                song.setTrackNumber(Integer.parseInt(trackField.getText()));
            } catch (Exception ignored) {}
            DialogUtils.showInfo("保存成功", "歌曲信息已更新（内存中，未写入文件）");
        }
        if (stage != null) stage.close();
    }

    @FXML
    public void onCancel() {
        if (stage != null) stage.close();
    }
}
