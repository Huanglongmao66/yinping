package com.yinyue.player.controller;

import com.yinyue.player.service.KaraokeService;
import com.yinyue.player.util.DialogUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class KaraokeController {
    @FXML private CheckBox karaokeCheck;
    @FXML private Slider reductionSlider;
    @FXML private Label reductionLabel;

    private KaraokeService karaokeService;
    private Stage stage;

    @FXML
    public void initialize() {
        karaokeService = KaraokeService.getInstance();
        karaokeCheck.setSelected(karaokeService.isEnabled());
        reductionSlider.setValue(karaokeService.getVocalReduction() * 100);
        reductionLabel.setText(String.format("%.0f%%", reductionSlider.getValue()));

        karaokeCheck.selectedProperty().addListener((obs, o, v) -> karaokeService.setEnabled(v));
        reductionSlider.valueProperty().addListener((obs, o, v) -> {
            karaokeService.setVocalReduction(v.doubleValue() / 100);
            reductionLabel.setText(String.format("%.0f%%", v.doubleValue()));
        });
    }

    public void setStage(Stage stage) { this.stage = stage; }

    @FXML
    public void onReset() {
        karaokeService.setEnabled(false);
        reductionSlider.setValue(80);
        karaokeCheck.setSelected(false);
    }

    @FXML
    public void onClose() {
        if (stage != null) stage.close();
    }
}