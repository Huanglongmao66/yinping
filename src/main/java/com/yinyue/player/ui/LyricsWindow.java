package com.yinyue.player.ui;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.LrcParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.util.List;

public class LyricsWindow {
    private Stage stage;
    private Label currentLineLabel;
    private Label nextLineLabel;
    private Timeline updateTimeline;
    private double xOffset = 0;
    private double yOffset = 0;
    private LrcParser lrcParser;
    private List<LrcParser.LyricLine> lyricLines;
    private long currentTime = 0;

    public LyricsWindow() {
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setTitle("桌面歌词");

        VBox root = new VBox(8);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 15 30; -fx-background-radius: 25;");
        root.setPrefWidth(600);

        currentLineLabel = createLyricLabel(24, Color.WHITE);
        nextLineLabel = createLyricLabel(16, Color.LIGHTGRAY);

        root.getChildren().addAll(currentLineLabel, nextLineLabel);

        // Drag support
        root.setOnMousePressed(this::onMousePressed);
        root.setOnMouseDragged(this::onMouseDragged);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        updateTimeline = new Timeline(new KeyFrame(Duration.millis(200), e -> updateLyrics()));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private Label createLyricLabel(int fontSize, Color color) {
        Label label = new Label("音悦播放器");
        label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, fontSize));
        label.setTextFill(color);
        label.setAlignment(Pos.CENTER);
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(3);
        label.setEffect(shadow);
        return label;
    }

    private void onMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void onMouseDragged(MouseEvent event) {
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    public void loadLyrics(Song song) {
        if (song == null || song.getFilePath() == null) {
            lrcParser = null;
            lyricLines = null;
            Platform.runLater(() -> {
                currentLineLabel.setText("音悦播放器");
                nextLineLabel.setText("等待播放...");
            });
            return;
        }

        String lrcPath = song.getFilePath().replaceAll("\\.[^.]+$", ".lrc");
        lrcParser = new LrcParser();
        lrcParser.parse(new File(lrcPath));
        if (!lrcParser.hasLyrics()) {
            lrcParser = null;
            lyricLines = null;
            Platform.runLater(() -> {
                currentLineLabel.setText(song.getDisplayTitle());
                nextLineLabel.setText(song.getArtist() != null ? song.getArtist() : "");
            });
            return;
        }

        lyricLines = lrcParser.getLyrics();
        Platform.runLater(() -> {
            currentLineLabel.setText(lyricLines.get(0).getText());
            nextLineLabel.setText(lyricLines.size() > 1 ? lyricLines.get(1).getText() : "");
        });
    }

    public void updateTime(long timeMs) {
        this.currentTime = timeMs;
    }

    private void updateLyrics() {
        if (lrcParser == null || lyricLines == null || lyricLines.isEmpty()) return;

        int currentIndex = lrcParser.getLyricIndexAt(currentTime);
        if (currentIndex < 0) return;

        String current = lyricLines.get(currentIndex).getText();
        String next = currentIndex + 1 < lyricLines.size() ? lyricLines.get(currentIndex + 1).getText() : "";

        Platform.runLater(() -> {
            currentLineLabel.setText(current);
            nextLineLabel.setText(next);
        });
    }

    public void show() {
        if (stage != null && !stage.isShowing()) {
            stage.show();
            updateTimeline.play();
        }
    }

    public void hide() {
        if (stage != null && stage.isShowing()) {
            stage.hide();
            updateTimeline.pause();
        }
    }

    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }

    public void toggle() {
        if (isShowing()) {
            hide();
        } else {
            show();
        }
    }

    public void close() {
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        if (stage != null) {
            stage.close();
        }
    }
}
