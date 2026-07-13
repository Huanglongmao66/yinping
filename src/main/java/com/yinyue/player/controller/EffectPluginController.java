package com.yinyue.player.controller;

import com.yinyue.player.service.AudioEffectPluginService;
import com.yinyue.player.util.DialogUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EffectPluginController {
    @FXML private CheckBox reverbCheck;
    @FXML private CheckBox bassBoostCheck;
    @FXML private CheckBox surroundCheck;
    @FXML private Slider bassGainSlider;
    @FXML private Slider reverbMixSlider;
    @FXML private Label bassGainLabel;
    @FXML private Label reverbMixLabel;

    private AudioEffectPluginService effectService;
    private Stage stage;

    @FXML
    public void initialize() {
        effectService = AudioEffectPluginService.getInstance();

        reverbCheck.setSelected(effectService.isReverbEnabled());
        bassBoostCheck.setSelected(effectService.isBassBoostEnabled());
        surroundCheck.setSelected(effectService.isSurroundEnabled());
        bassGainSlider.setValue(effectService.getBassBoostGain());
        reverbMixSlider.setValue(effectService.getReverbMix());

        reverbCheck.selectedProperty().addListener((obs, o, v) -> effectService.setReverb(v));
        bassBoostCheck.selectedProperty().addListener((obs, o, v) -> effectService.setBassBoost(v));
        surroundCheck.selectedProperty().addListener((obs, o, v) -> effectService.setSurround(v));

        bassGainSlider.valueProperty().addListener((obs, o, v) -> {
            effectService.setBassBoostGain(v.doubleValue());
            bassGainLabel.setText(String.format("%.1f dB", v.doubleValue()));
        });
        reverbMixSlider.valueProperty().addListener((obs, o, v) -> {
            effectService.setReverbMix(v.doubleValue());
            reverbMixLabel.setText(String.format("%.0f%%", v.doubleValue() * 100));
        });
    }

    public void setStage(Stage stage) { this.stage = stage; }

    @FXML
    public void onReset() {
        effectService.resetAll();
        reverbCheck.setSelected(false);
        bassBoostCheck.setSelected(false);
        surroundCheck.setSelected(false);
        bassGainSlider.setValue(6.0);
        reverbMixSlider.setValue(0.3);
    }

    @FXML
    public void onClose() {
        if (stage != null) stage.close();
    }
}
