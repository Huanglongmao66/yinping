package com.yinyue.player.controller;

import com.yinyue.player.util.ConfigManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsController {
    @FXML
    private ListView<String> settingsListView;

    @FXML
    private TabPane contentTabPane;

    @FXML
    private Tab generalTab;
    @FXML
    private Tab playbackTab;
    @FXML
    private Tab interfaceTab;
    @FXML
    private Tab shortcutsTab;
    @FXML
    private Tab libraryTab;
    @FXML
    private Tab advancedTab;

    @FXML
    private Slider volumeSlider;
    @FXML
    private CheckBox autoPlayCheckBox;
    @FXML
    private CheckBox crossfadeCheckBox;
    @FXML
    private Spinner<Integer> crossfadeDurationSpinner;

    @FXML
    private ComboBox<String> themeComboBox;
    @FXML
    private CheckBox showLyricsCheckBox;
    @FXML
    private CheckBox showVisualizerCheckBox;
    @FXML
    private Spinner<Integer> fontSizeSpinner;

    @FXML
    private ListView<String> scanPathsListView;
    @FXML
    private Button addPathButton;
    @FXML
    private Button removePathButton;
    @FXML
    private CheckBox autoScanCheckBox;

    @FXML
    private Button applyButton;
    @FXML
    private Button okButton;
    @FXML
    private Button cancelButton;

    private Stage stage;
    private ConfigManager config;
    private SettingsChangeListener listener;

    public interface SettingsChangeListener {
        void onSettingsChanged();
    }

    @FXML
    public void initialize() {
        config = ConfigManager.getInstance();

        settingsListView.setItems(FXCollections.observableArrayList(
                "常规", "播放", "界面", "快捷键", "音乐库", "高级"
        ));
        settingsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "常规":
                    contentTabPane.getSelectionModel().select(generalTab);
                    break;
                case "播放":
                    contentTabPane.getSelectionModel().select(playbackTab);
                    break;
                case "界面":
                    contentTabPane.getSelectionModel().select(interfaceTab);
                    break;
                case "快捷键":
                    contentTabPane.getSelectionModel().select(shortcutsTab);
                    break;
                case "音乐库":
                    contentTabPane.getSelectionModel().select(libraryTab);
                    break;
                case "高级":
                    contentTabPane.getSelectionModel().select(advancedTab);
                    break;
            }
        });

        themeComboBox.getItems().addAll("暗色", "亮色");
        crossfadeDurationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));
        fontSizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));

        loadSettings();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setSettingsChangeListener(SettingsChangeListener listener) {
        this.listener = listener;
    }

    private void loadSettings() {
        volumeSlider.setValue(config.getVolume() * 100);
        autoPlayCheckBox.setSelected(config.isAutoPlay());
        crossfadeCheckBox.setSelected(config.isAutoPlay());
        themeComboBox.setValue(config.getTheme().equals("dark") ? "暗色" : "亮色");
        showLyricsCheckBox.setSelected(config.isShowLyrics());
        showVisualizerCheckBox.setSelected(config.isShowVisualization());
        autoScanCheckBox.setSelected(config.isAutoScan());

        scanPathsListView.setItems(FXCollections.observableArrayList(config.getScanPaths()));
    }

    private void saveSettings() {
        config.setVolume(volumeSlider.getValue() / 100);
        config.setAutoPlay(autoPlayCheckBox.isSelected());
        config.setTheme(themeComboBox.getValue().equals("暗色") ? "dark" : "light");
        config.setShowLyrics(showLyricsCheckBox.isSelected());
        config.setShowVisualization(showVisualizerCheckBox.isSelected());
        config.setAutoScan(autoScanCheckBox.isSelected());

        List<String> paths = new ArrayList<>(scanPathsListView.getItems());
        config.setScanPaths(paths);
    }

    @FXML
    private void onAddPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择音乐目录");
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            String path = selected.getAbsolutePath();
            if (!scanPathsListView.getItems().contains(path)) {
                scanPathsListView.getItems().add(path);
            }
        }
    }

    @FXML
    private void onRemovePath() {
        int selectedIndex = scanPathsListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            scanPathsListView.getItems().remove(selectedIndex);
        }
    }

    @FXML
    private void onApply() {
        saveSettings();
        if (listener != null) {
            listener.onSettingsChanged();
        }
    }

    @FXML
    private void onOk() {
        saveSettings();
        if (listener != null) {
            listener.onSettingsChanged();
        }
        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    private void onCancel() {
        if (stage != null) {
            stage.close();
        }
    }
}