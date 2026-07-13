package com.yinyue.player.controller;

import com.yinyue.player.service.AudioEffectService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EqualizerController {
    @FXML
    private CheckBox enableEqualizerCheckBox;

    @FXML
    private ComboBox<String> presetComboBox;

    @FXML
    private VBox bandsContainer;

    @FXML
    private Button resetButton;

    private AudioEffectService effectService;
    private Stage stage;
    private Slider[] bandSliders;

    @FXML
    public void initialize() {
        if (effectService == null) {
            effectService = AudioEffectService.getInstance();
        }

        String[] labels = effectService.getFrequencyLabels();
        bandSliders = new Slider[effectService.getBandCount()];

        for (int i = 0; i < effectService.getBandCount(); i++) {
            final int bandIndex = i;
            Slider slider = new Slider(-12, 12, 0);
            slider.setPrefHeight(100);
            slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setMajorTickUnit(6);
            slider.setMinorTickCount(2);
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                effectService.setEqualizerBand(bandIndex, newVal.floatValue());
            });
            bandSliders[i] = slider;

            javafx.scene.control.Label label = new javafx.scene.control.Label(labels[i]);
            label.setStyle("-fx-text-fill: #ccc; -fx-font-size: 10px;");

            VBox bandBox = new VBox(label, slider);
            bandBox.setAlignment(javafx.geometry.Pos.CENTER);
            bandBox.setSpacing(4);
            bandsContainer.getChildren().add(bandBox);
        }

        presetComboBox.getItems().addAll(
                "平直", "摇滚", "流行", "爵士", "古典", "重低音", "高音增强", "人声", "派对"
        );

        loadSettings();
    }

    public void setAudioEffectService(AudioEffectService service) {
        this.effectService = service;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void loadSettings() {
        enableEqualizerCheckBox.setSelected(effectService.isEqualizerEnabled());
        float[] bands = effectService.getEqualizerBands();
        for (int i = 0; i < bands.length; i++) {
            if (bandSliders[i] != null) {
                bandSliders[i].setValue(bands[i]);
            }
        }
    }

    @FXML
    private void onEnableEqualizer() {
        effectService.setEqualizerEnabled(enableEqualizerCheckBox.isSelected());
    }

    @FXML
    private void onPresetChanged() {
        String presetName = presetComboBox.getValue();
        if (presetName == null) {
            return;
        }

        AudioEffectService.Preset preset = null;
        for (AudioEffectService.Preset p : AudioEffectService.Preset.values()) {
            if (p.getDisplayName().equals(presetName)) {
                preset = p;
                break;
            }
        }

        if (preset != null) {
            effectService.setPreset(preset);
            float[] bands = effectService.getEqualizerBands();
            for (int i = 0; i < bands.length; i++) {
                bandSliders[i].setValue(bands[i]);
            }
        }
    }

    @FXML
    private void onReset() {
        effectService.setPreset(AudioEffectService.Preset.FLAT);
        float[] bands = effectService.getEqualizerBands();
        for (int i = 0; i < bands.length; i++) {
            bandSliders[i].setValue(bands[i]);
        }
        presetComboBox.setValue("平直");
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
}