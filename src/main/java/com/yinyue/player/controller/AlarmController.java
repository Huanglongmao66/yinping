package com.yinyue.player.controller;

import com.yinyue.player.service.AlarmService;
import com.yinyue.player.service.PlaylistService;
import com.yinyue.player.util.DialogUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmController {
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private ComboBox<String> playlistCombo;
    @FXML private Label statusLabel;
    @FXML private Button setButton;
    @FXML private Button cancelButton;

    private Stage stage;

    @FXML
    public void initialize() {
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 7));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        List<String> playlists = new ArrayList<>();
        playlists.add("当前播放列表");
        for (com.yinyue.player.model.Playlist pl : PlaylistService.getInstance().getAllPlaylists()) {
            playlists.add(pl.getName());
        }
        playlistCombo.setItems(javafx.collections.FXCollections.observableArrayList(playlists));
        playlistCombo.getSelectionModel().selectFirst();

        updateStatus();
    }

    public void setStage(Stage stage) { this.stage = stage; }

    private void updateStatus() {
        AlarmService alarm = AlarmService.getInstance();
        if (alarm.isEnabled() && alarm.getAlarmTime() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(alarm.getAlarmTime());
            statusLabel.setText(String.format("闹钟已设置: %02d:%02d", 
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
            cancelButton.setDisable(false);
        } else {
            statusLabel.setText("闹钟未设置");
            cancelButton.setDisable(true);
        }
    }

    @FXML
    public void onSet() {
        int hour = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        String playlist = playlistCombo.getValue();
        if ("当前播放列表".equals(playlist)) playlist = null;
        AlarmService.getInstance().setAlarm(hour, minute, playlist);
        DialogUtils.showInfo("闹钟已设置", String.format("将在 %02d:%02d 播放音乐", hour, minute));
        updateStatus();
    }

    @FXML
    public void onCancel() {
        AlarmService.getInstance().cancelAlarm();
        statusLabel.setText("闹钟已取消");
        updateStatus();
    }

    @FXML
    public void onClose() {
        if (stage != null) stage.close();
    }
}