package com.yinyue.player.controller;

import com.yinyue.player.service.AudioPlayerService;
import com.yinyue.player.service.PlaylistService;
import com.yinyue.player.util.AudioSplitter;
import com.yinyue.player.util.DialogUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AudioSplitController {
    @FXML private Label fileLabel;
    @FXML private Label durationLabel;
    @FXML private Slider startSlider;
    @FXML private Slider endSlider;
    @FXML private Label startLabel;
    @FXML private Label endLabel;
    @FXML private TextField outputPathField;

    private String inputPath;
    private long durationMs;
    private Stage stage;

    @FXML
    public void initialize() {
        startSlider.valueProperty().addListener((obs, o, v) -> {
            startLabel.setText(formatTime(v.longValue()));
        });
        endSlider.valueProperty().addListener((obs, o, v) -> {
            endLabel.setText(formatTime(v.longValue()));
        });
    }

    public void setInputFile(String path, long duration) {
        this.inputPath = path;
        this.durationMs = duration;
        fileLabel.setText(new File(path).getName());
        durationLabel.setText(formatTime(duration));
        startSlider.setMax(duration);
        endSlider.setMax(duration);
        endSlider.setValue(duration);
        outputPathField.setText(path.replaceAll("\\.[^.]+$", "_clip.wav"));
    }

    public void setStage(Stage stage) { this.stage = stage; }

    @FXML
    public void onBrowseOutput() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV 文件", "*.wav"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            outputPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    public void onSplit() {
        if (inputPath == null) {
            DialogUtils.showWarning("提示", "请先选择音频文件");
            return;
        }
        String outputPath = outputPathField.getText();
        if (outputPath == null || outputPath.isEmpty()) {
            DialogUtils.showWarning("提示", "请指定输出路径");
            return;
        }

        long start = (long) startSlider.getValue();
        long end = (long) endSlider.getValue();

        javafx.concurrent.Task<Void> task = AudioSplitter.splitAsync(inputPath, outputPath, start, end,
                new AudioSplitter.SplitCallback() {
                    @Override public void onSuccess(String p) {
                        DialogUtils.showInfo("分割完成", "文件已保存到:\n" + p);
                    }
                    @Override public void onError(String error) {
                        DialogUtils.showError("分割失败", error);
                    }
                });
        new Thread(task).start();
    }

    @FXML
    public void onClose() {
        if (stage != null) stage.close();
    }

    private String formatTime(long ms) {
        int sec = (int) (ms / 1000);
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
