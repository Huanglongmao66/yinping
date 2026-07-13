package com.yinyue.player.controller;

import com.yinyue.player.service.StatisticsService;
import com.yinyue.player.util.AudioUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class StatisticsController {
    @FXML private Label totalSongsLabel;
    @FXML private Label totalArtistsLabel;
    @FXML private Label totalAlbumsLabel;
    @FXML private Label totalPlayCountLabel;
    @FXML private Label totalDurationLabel;
    @FXML private Label totalFileSizeLabel;
    @FXML private ListView<String> topArtistsList;
    @FXML private ListView<String> topSongsList;
    @FXML private ListView<String> formatList;
    @FXML private ListView<String> hourlyList;

    private Stage stage;

    @FXML
    public void initialize() {
        StatisticsService stats = StatisticsService.getInstance();
        Map<String, Object> overall = stats.getOverallStats();

        totalSongsLabel.setText(String.valueOf(overall.get("totalSongs")));
        totalArtistsLabel.setText(String.valueOf(overall.get("totalArtists")));
        totalAlbumsLabel.setText(String.valueOf(overall.get("totalAlbums")));
        totalPlayCountLabel.setText(String.valueOf(overall.get("totalPlayCount")));
        totalDurationLabel.setText(String.valueOf(overall.get("totalDuration")));
        totalFileSizeLabel.setText(String.valueOf(overall.get("totalFileSize")));

        ObservableList<String> artistItems = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> e : stats.getTopArtists(10)) {
            artistItems.add(e.getKey() + " - " + e.getValue() + " 次");
        }
        topArtistsList.setItems(artistItems);

        ObservableList<String> songItems = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> e : stats.getTopSongs(10)) {
            songItems.add(e.getKey() + " - " + e.getValue() + " 次");
        }
        topSongsList.setItems(songItems);

        ObservableList<String> fmtItems = FXCollections.observableArrayList();
        stats.getPlaysByFormat().forEach((k, v) -> fmtItems.add(k + ": " + v + " 首"));
        formatList.setItems(fmtItems);

        ObservableList<String> hourItems = FXCollections.observableArrayList();
        stats.getPlaysByHour().forEach((k, v) -> {
            if (v > 0) hourItems.add(k + " - " + v + " 次");
        });
        hourlyList.setItems(hourItems);
    }

    public void setStage(Stage stage) { this.stage = stage; }

    @FXML
    public void onClose() {
        if (stage != null) stage.close();
    }
}
