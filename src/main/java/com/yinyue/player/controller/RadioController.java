package com.yinyue.player.controller;

import com.yinyue.player.service.RadioService;
import com.yinyue.player.util.DialogUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RadioController {
    @FXML private TextField nameField;
    @FXML private TextField urlField;
    @FXML private TextField genreField;
    @FXML private ListView<String> stationListView;
    @FXML private Label statusLabel;

    private RadioService radioService;
    private ObservableList<String> stationItems;

    @FXML
    public void initialize() {
        radioService = RadioService.getInstance();
        stationItems = FXCollections.observableArrayList();
        stationListView.setItems(stationItems);
        updateStationList();

        stationListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = stationListView.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    playStation(index);
                }
            }
        });
    }

    private void updateStationList() {
        stationItems.clear();
        for (RadioService.RadioStation station : radioService.getStations()) {
            stationItems.add(station.toString());
        }
    }

    @FXML
    public void onPlay() {
        int index = stationListView.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            playStation(index);
        }
    }

    @FXML
    public void onStop() {
        radioService.stop();
        statusLabel.setText("已停止");
    }

    @FXML
    public void onAdd() {
        String name = nameField.getText();
        String url = urlField.getText();
        String genre = genreField.getText();
        if (name == null || name.isEmpty() || url == null || url.isEmpty()) {
            DialogUtils.showWarning("提示", "请输入电台名称和地址");
            return;
        }
        radioService.addStation(name, url, genre != null ? genre : "其他");
        updateStationList();
        nameField.clear();
        urlField.clear();
        genreField.clear();
    }

    @FXML
    public void onRemove() {
        int index = stationListView.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            RadioService.RadioStation station = radioService.getStations().get(index);
            radioService.removeStation(station.getName());
            updateStationList();
        }
    }

    private void playStation(int index) {
        RadioService.RadioStation station = radioService.getStations().get(index);
        radioService.playStation(station);
        statusLabel.setText("正在播放: " + station.getName());
    }
}
