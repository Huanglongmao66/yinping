package com.yinyue.player.ui;

import com.yinyue.player.service.AudioPlayerService;
import com.yinyue.player.service.PlaylistService;
import com.yinyue.player.util.AudioUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MiniPlayer {
    private Stage stage;
    private Label titleLabel;
    private Label artistLabel;
    private Button playButton;
    private Button prevButton;
    private Button nextButton;
    private Slider progressSlider;
    private double xOffset = 0;
    private double yOffset = 0;

    private final AudioPlayerService playerService;
    private final PlaylistService playlistService;

    public MiniPlayer() {
        playerService = AudioPlayerService.getInstance();
        playlistService = PlaylistService.getInstance();
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true);
        stage.setTitle("迷你播放器");
        stage.setWidth(350);
        stage.setHeight(120);

        VBox root = new VBox(5);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 10; -fx-border-color: #e94560; -fx-border-width: 2;");

        titleLabel = new Label("未播放");
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");

        artistLabel = new Label("");
        artistLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");

        progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(300);
        progressSlider.setStyle("-fx-accent: #e94560;");
        progressSlider.setOnMouseReleased(e -> {
            playerService.seek(progressSlider.getValue() / 100);
        });

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);

        prevButton = createControlButton("⏮", () -> playlistService.playPrevious());
        playButton = createControlButton("▶", () -> {
            if (playerService.isPlaying()) {
                playerService.pause();
            } else {
                playerService.resume();
            }
        });
        nextButton = createControlButton("⏭", () -> playlistService.playNext());

        Button closeButton = createControlButton("×", () -> hide());
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #e94560; -fx-font-size: 14px;");

        controls.getChildren().addAll(prevButton, playButton, nextButton, closeButton);
        root.getChildren().addAll(titleLabel, artistLabel, progressSlider, controls);

        // Drag support
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Bindings
        playerService.currentSongProperty().addListener((obs, old, song) -> {
            if (song != null) {
                titleLabel.setText(song.getDisplayTitle());
                artistLabel.setText(song.getArtist() != null ? song.getArtist() : "未知艺术家");
            } else {
                titleLabel.setText("未播放");
                artistLabel.setText("");
            }
        });

        playerService.playingProperty().addListener((obs, old, playing) -> {
            playButton.setText(playing ? "⏸" : "▶");
        });

        playerService.currentTimeProperty().addListener((obs, old, time) -> {
            if (playerService.getDuration() > 0) {
                progressSlider.setValue(time.doubleValue() / playerService.getDuration() * 100);
            }
        });
    }

    private Button createControlButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e0e0e0; -fx-font-size: 16px;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    public void show() {
        if (stage != null && !stage.isShowing()) {
            stage.show();
        }
    }

    public void hide() {
        if (stage != null && stage.isShowing()) {
            stage.hide();
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
}
