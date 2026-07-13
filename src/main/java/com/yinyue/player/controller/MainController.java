package com.yinyue.player.controller;

import com.yinyue.player.model.HistoryEntry;
import com.yinyue.player.model.PlayMode;
import com.yinyue.player.model.Song;
import com.yinyue.player.service.*;
import com.yinyue.player.ui.LyricsWindow;
import com.yinyue.player.ui.MiniPlayer;
import com.yinyue.player.util.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainController {
    @FXML private BorderPane root;
    @FXML private MenuBar menuBar;
    @FXML private ListView<String> playlistListView;
    @FXML private ListView<String> libraryListView;
    @FXML private TextField searchField;
    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button stopButton;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Slider progressSlider;
    @FXML private Slider volumeSlider;
    @FXML private Label timeLabel;
    @FXML private Label durationLabel;
    @FXML private Label songTitleLabel;
    @FXML private Label songArtistLabel;
    @FXML private Label playModeLabel;
    @FXML private Canvas visualizerCanvas;
    @FXML private VBox playlistSidebar;

    private Stage primaryStage;
    private AudioPlayerService playerService;
    private PlaylistService playlistService;
    private LibraryService libraryService;
    private AudioEffectService effectService;
    private HistoryService historyService;
    private PodcastService podcastService;
    private CloudSyncService cloudSyncService;
    private ConfigManager config;
    private ThemeManager themeManager;
    private SmartLibraryService smartLibraryService;
    private RecommendationEngine recommendationEngine;
    private RadioService radioService;
    private RemoteControlServer remoteControlServer;
    private AudioRecorder audioRecorder;

    private LyricsWindow lyricsWindow;
    private MiniPlayer miniPlayer;

    private ObservableList<String> playlistItems;
    private ObservableList<String> libraryItems;

    private Timeline visualizerTimeline;
    private int visualizerMode = 0;
    private Timeline sleepTimer;

    @FXML
    public void initialize() {
        playerService = AudioPlayerService.getInstance();
        playlistService = PlaylistService.getInstance();
        libraryService = LibraryService.getInstance();
        effectService = AudioEffectService.getInstance();
        historyService = HistoryService.getInstance();
        podcastService = PodcastService.getInstance();
        cloudSyncService = CloudSyncService.getInstance();
        config = ConfigManager.getInstance();
        themeManager = ThemeManager.getInstance();
        smartLibraryService = SmartLibraryService.getInstance();
        recommendationEngine = RecommendationEngine.getInstance();
        radioService = RadioService.getInstance();
        remoteControlServer = RemoteControlServer.getInstance();
        audioRecorder = new AudioRecorder();

        lyricsWindow = new LyricsWindow();
        miniPlayer = new MiniPlayer();

        playlistItems = FXCollections.observableArrayList();
        libraryItems = FXCollections.observableArrayList();

        playlistListView.setItems(playlistItems);
        libraryListView.setItems(libraryItems);

        setupBindings();
        setupVisualizer();
        updatePlaylistView();
        updateLibraryView();
        setupContextMenus();
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        DialogUtils.setOwnerStage(primaryStage);
    }

    private void setupBindings() {
        volumeSlider.setValue(playerService.getVolume() * 100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            playerService.setVolume(newVal.doubleValue() / 100);
        });

        playerService.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                timeLabel.setText(AudioUtils.formatTime(newVal.longValue()));
                if (playerService.getDuration() > 0) {
                    progressSlider.setValue(newVal.doubleValue() / playerService.getDuration() * 100);
                }
                // Update lyrics time
                lyricsWindow.updateTime(newVal.longValue());
                // Update history play duration
                if (playerService.getCurrentSong() != null && newVal.longValue() % 5000 < 200) {
                    historyService.updatePlayDuration(playerService.getCurrentSong(), newVal.longValue());
                }
            });
        });

        playerService.durationProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                durationLabel.setText(AudioUtils.formatTime(newVal.longValue()));
            });
        });

        playerService.currentSongProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal != null) {
                    songTitleLabel.setText(newVal.getDisplayTitle());
                    songArtistLabel.setText(newVal.getArtist() != null ? newVal.getArtist() : "未知艺术家");
                    updatePlaylistSelection();
                    // Add to history
                    historyService.addToHistory(newVal);
                    // Load lyrics
                    lyricsWindow.loadLyrics(newVal);
                    // System notification
                    if (java.awt.SystemTray.isSupported()) {
                        try {
                            java.awt.TrayIcon[] icons = java.awt.SystemTray.getSystemTray().getTrayIcons();
                            if (icons.length > 0) {
                                icons[0].displayMessage("正在播放", newVal.getDisplayTitle(), java.awt.TrayIcon.MessageType.INFO);
                            }
                        } catch (Exception ignored) {}
                    }
                    // Apply playback speed
                    PlaybackSpeedService.getInstance().speedProperty().addListener((obs2, oldV, newV) -> {
                        // Speed is applied when MediaPlayer is available
                    });
                } else {
                    songTitleLabel.setText("未播放");
                    songArtistLabel.setText("");
                }
            });
        });

        playerService.playingProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                playButton.setVisible(!newVal);
                pauseButton.setVisible(newVal);
            });
        });

        progressSlider.setOnMouseReleased(e -> {
            double percentage = progressSlider.getValue() / 100;
            playerService.seek(percentage);
        });

        playlistListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = playlistListView.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    playlistService.playSong(index);
                }
            }
        });

        libraryListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = libraryListView.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    Song song = libraryService.getSongs().get(index);
                    playlistService.addSong(song);
                    playlistService.playSong(playlistService.getCurrentPlaylist().getSongs().size() - 1);
                }
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateLibraryView());

        // Drag and drop support
        root.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        root.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                List<Song> songs = new ArrayList<>();
                for (File file : db.getFiles()) {
                    if (file.isFile() && AudioUtils.isSupportedAudioFormat(file.getName())) {
                        Song song = new Song();
                        song.setId(java.util.UUID.randomUUID().toString());
                        song.setFilePath(file.getAbsolutePath());
                        song.setFileSize(file.length());
                        song.setFormat(AudioUtils.getFileExtension(file.getName()));
                        song.setTitle(file.getName().replaceAll("\\.[^.]+$", ""));
                        songs.add(song);
                    }
                }
                if (!songs.isEmpty()) {
                    playlistService.addSongs(songs);
                    updatePlaylistView();
                }
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    private void setupVisualizer() {
        visualizerTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> drawVisualizer()));
        visualizerTimeline.setCycleCount(Timeline.INDEFINITE);
        if (config.isShowVisualization()) {
            visualizerTimeline.play();
        }
        visualizerCanvas.widthProperty().bind(root.widthProperty());
    }

    private void drawVisualizer() {
        GraphicsContext gc = visualizerCanvas.getGraphicsContext2D();
        double width = visualizerCanvas.getWidth();
        double height = visualizerCanvas.getHeight();
        gc.clearRect(0, 0, width, height);
        if (!playerService.isPlaying()) return;
        Random random = new Random();
        int barCount = 60;
        switch (visualizerMode) {
            case 0: drawBarsVisualizer(gc, width, height, barCount, random); break;
            case 1: drawWaveVisualizer(gc, width, height, barCount, random); break;
            case 2: drawCircleVisualizer(gc, width, height, barCount, random); break;
            case 3: drawSpectrumVisualizer(gc, width, height, barCount, random); break;
        }
    }

    private void drawBarsVisualizer(GraphicsContext gc, double width, double height, int barCount, Random random) {
        double barWidth = width / barCount;
        for (int i = 0; i < barCount; i++) {
            double value = random.nextDouble();
            double barHeight = value * height * 0.8;
            gc.setFill(Color.hsb(i * 6, 0.8, 0.8));
            gc.fillRect(i * barWidth + 1, height - barHeight, barWidth - 2, barHeight);
        }
    }

    private void drawWaveVisualizer(GraphicsContext gc, double width, double height, int barCount, Random random) {
        gc.setStroke(Color.LIGHTBLUE);
        gc.setLineWidth(2);
        gc.beginPath();
        gc.moveTo(0, height / 2);
        for (int i = 0; i < barCount; i++) {
            double x = (i / (double) barCount) * width;
            double y = height / 2 + (random.nextDouble() - 0.5) * height * 0.6;
            gc.lineTo(x, y);
        }
        gc.stroke();
    }

    private void drawCircleVisualizer(GraphicsContext gc, double width, double height, int barCount, Random random) {
        double cx = width / 2, cy = height / 2;
        double baseR = Math.min(width, height) * 0.2;
        for (int i = 0; i < barCount; i++) {
            double angle = (i / (double) barCount) * Math.PI * 2;
            double r = baseR + random.nextDouble() * baseR * 1.5;
            gc.setStroke(Color.hsb(i * 6, 0.8, 0.8));
            gc.setLineWidth(3);
            gc.strokeLine(cx + Math.cos(angle) * baseR, cy + Math.sin(angle) * baseR,
                          cx + Math.cos(angle) * r, cy + Math.sin(angle) * r);
        }
        gc.setFill(Color.PURPLE);
        gc.fillOval(cx - 10, cy - 10, 20, 20);
    }

    private void drawSpectrumVisualizer(GraphicsContext gc, double width, double height, int barCount, Random random) {
        double barWidth = width / barCount;
        for (int i = 0; i < barCount; i++) {
            double value = Math.pow(random.nextDouble(), 2);
            double barHeight = value * height * 0.9;
            gc.setFill(Color.hsb(280 + value * 60, 0.9, 0.9, value * 0.8 + 0.2));
            gc.fillRect(i * barWidth + 0.5, height - barHeight, barWidth - 1, barHeight);
        }
    }

    private void setupContextMenus() {
        ContextMenu libraryMenu = new ContextMenu();
        MenuItem addToPlaylist = new MenuItem("添加到播放列表");
        addToPlaylist.setOnAction(e -> {
            int index = libraryListView.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                Song song = libraryService.getSongs().get(index);
                playlistService.addSong(song);
            }
        });
        libraryMenu.getItems().add(addToPlaylist);
        libraryListView.setContextMenu(libraryMenu);

        ContextMenu playlistMenu = new ContextMenu();
        MenuItem removeFromPlaylist = new MenuItem("从播放列表移除");
        removeFromPlaylist.setOnAction(e -> {
            int index = playlistListView.getSelectionModel().getSelectedIndex();
            if (index >= 0) playlistService.removeSong(index);
        });
        MenuItem clearPlaylist = new MenuItem("清空播放列表");
        clearPlaylist.setOnAction(e -> {
            if (DialogUtils.showConfirm("确认清空", "确定要清空播放列表吗？")) {
                playlistService.clearCurrentPlaylist();
            }
        });
        playlistMenu.getItems().addAll(removeFromPlaylist, clearPlaylist);
        playlistListView.setContextMenu(playlistMenu);
    }

    private void updatePlaylistView() {
        playlistItems.clear();
        if (playlistService.getCurrentPlaylist() != null) {
            for (Song song : playlistService.getCurrentPlaylist().getSongs()) {
                playlistItems.add(song.getDisplayTitle());
            }
        }
    }

    private void updateLibraryView() {
        libraryItems.clear();
        String keyword = searchField.getText();
        List<Song> songs = keyword.isEmpty() ? libraryService.getSongs() : libraryService.searchSongs(keyword);
        for (Song song : songs) {
            libraryItems.add(song.getDisplayTitle());
        }
    }

    private void updatePlaylistSelection() {
        int currentIndex = playlistService.getCurrentIndex();
        if (currentIndex >= 0 && currentIndex < playlistItems.size()) {
            playlistListView.getSelectionModel().select(currentIndex);
            playlistListView.scrollTo(currentIndex);
        }
    }

    @FXML public void onPlay() { playerService.resume(); }
    @FXML public void onPause() { playerService.pause(); }
    @FXML public void onStop() { playerService.stop(); }
    @FXML public void onPrevious() { playlistService.playPrevious(); }
    @FXML public void onNext() { playlistService.playNext(); }

    @FXML
    public void onTogglePlayMode() {
        PlayMode currentMode = playlistService.getPlayMode();
        PlayMode[] modes = PlayMode.values();
        int index = (currentMode.ordinal() + 1) % modes.length;
        playlistService.setPlayMode(modes[index]);
        playModeLabel.setText(modes[index].getDisplayName());
    }

    @FXML
    public void onOpenFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.wav", "*.flac", "*.ogg", "*.aac", "*.m4a"));
        List<File> files = chooser.showOpenMultipleDialog(primaryStage);
        if (files != null) {
            List<Song> songs = new ArrayList<>();
            for (File file : files) {
                Song song = new Song();
                song.setId(java.util.UUID.randomUUID().toString());
                song.setFilePath(file.getAbsolutePath());
                song.setFileSize(file.length());
                song.setFormat(AudioUtils.getFileExtension(file.getName()));
                song.setTitle(AudioUtils.getFileExtension(file.getName()).equals("") ? file.getName() : file.getName().substring(0, file.getName().lastIndexOf('.')));
                songs.add(song);
            }
            playlistService.addSongs(songs);
            if (config.isAutoPlay() && !playerService.isPlaying()) {
                playlistService.playSong(0);
            }
        }
    }

    @FXML
    public void onOpenFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择音乐目录");
        File selected = chooser.showDialog(primaryStage);
        if (selected != null) {
            libraryService.scanDirectory(selected.getAbsolutePath());
            updateLibraryView();
        }
    }

    @FXML
    public void onEqualizer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/equalizer_view.fxml"));
            BorderPane view = loader.load();
            EqualizerController controller = loader.getController();
            controller.setAudioEffectService(effectService);
            Stage stage = new Stage();
            stage.setTitle("均衡器");
            stage.setScene(new Scene(view, 600, 350));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            controller.setStage(stage);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开均衡器窗口");
        }
    }

    @FXML
    public void onSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings_view.fxml"));
            BorderPane view = loader.load();
            SettingsController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("设置");
            stage.setScene(new Scene(view, 700, 500));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            controller.setStage(stage);
            controller.setSettingsChangeListener(this::onSettingsChanged);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开设置窗口");
        }
    }

    private void onSettingsChanged() {
        updateLibraryView();
        if (config.isShowVisualization()) {
            visualizerTimeline.play();
        } else {
            visualizerTimeline.pause();
        }
    }

    @FXML
    public void onSongInfo() {
        Song song = playerService.getCurrentSong();
        if (song != null) {
            showSongInfoDialog(song);
        } else {
            DialogUtils.showWarning("提示", "请先选择一首歌曲");
        }
    }

    private void showSongInfoDialog(Song song) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/song_info_view.fxml"));
            BorderPane view = loader.load();
            SongInfoController controller = loader.getController();
            controller.setSong(song);
            Stage stage = new Stage();
            stage.setTitle("歌曲信息");
            stage.setScene(new Scene(view, 500, 400));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(this.root.getScene().getWindow());
            controller.setStage(stage);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开歌曲信息窗口");
        }
    }

    @FXML
    public void onAbout() {
        DialogUtils.showInfo("关于音悦播放器", "音悦播放器 v1.0.0\n\n一款功能丰富的桌面音频播放软件\n支持 MP3、WAV、FLAC 等多种格式\n\n新功能:\n- 播放历史记录\n- 桌面歌词悬浮窗\n- 迷你播放器模式\n- 音频波形预览\n- 多主题切换\n- 播客/RSS订阅\n- 音频格式转换\n- 云同步备份\n\n版权所有 (C) 2024 音悦科技");
    }

    @FXML public void onExit() { Platform.exit(); System.exit(0); }

    @FXML
    public void onSleepTimer() {
        List<String> options = List.of("15分钟", "30分钟", "45分钟", "60分钟", "90分钟", "取消");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("30分钟", options);
        dialog.setTitle("定时关闭");
        dialog.setHeaderText(null);
        dialog.setContentText("选择定时关闭时间：");
        dialog.showAndWait().ifPresent(choice -> {
            if (!choice.equals("取消")) {
                int minutes = Integer.parseInt(choice.replace("分钟", ""));
                startSleepTimer(minutes * 60 * 1000);
            } else {
                cancelSleepTimer();
            }
        });
    }

    private void startSleepTimer(long delay) {
        cancelSleepTimer();
        sleepTimer = new Timeline(new KeyFrame(Duration.millis(delay), e -> Platform.exit()));
        sleepTimer.setCycleCount(1);
        sleepTimer.play();
        DialogUtils.showInfo("定时关闭", String.format("播放器将在 %d 分钟后关闭", delay / 60000));
    }

    private void cancelSleepTimer() {
        if (sleepTimer != null) {
            sleepTimer.stop();
            sleepTimer = null;
        }
    }

    @FXML public void onChangeVisualizerMode() { visualizerMode = (visualizerMode + 1) % 4; }

    @FXML
    public void onNavAlbums() {
        List<String> albums = libraryService.getAllAlbums();
        if (albums.isEmpty()) { DialogUtils.showInfo("提示", "暂无专辑信息"); return; }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(albums.get(0), albums);
        dialog.setTitle("浏览专辑");
        dialog.setHeaderText(null);
        dialog.setContentText("选择专辑：");
        dialog.showAndWait().ifPresent(album -> {
            List<Song> songs = libraryService.getSongsByAlbum(album);
            playlistService.clearCurrentPlaylist();
            playlistService.addSongs(songs);
            if (config.isAutoPlay()) playlistService.playSong(0);
        });
    }

    @FXML
    public void onNavArtists() {
        List<String> artists = libraryService.getAllArtists();
        if (artists.isEmpty()) { DialogUtils.showInfo("提示", "暂无艺术家信息"); return; }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(artists.get(0), artists);
        dialog.setTitle("浏览艺术家");
        dialog.setHeaderText(null);
        dialog.setContentText("选择艺术家：");
        dialog.showAndWait().ifPresent(artist -> {
            List<Song> songs = libraryService.getSongsByArtist(artist);
            playlistService.clearCurrentPlaylist();
            playlistService.addSongs(songs);
            if (config.isAutoPlay()) playlistService.playSong(0);
        });
    }

    // ===== New Features =====

    @FXML
    public void onShowHistory() {
        List<HistoryEntry> history = historyService.getRecentHistory(50);
        if (history.isEmpty()) {
            DialogUtils.showInfo("播放历史", "暂无播放记录");
            return;
        }
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        for (HistoryEntry entry : history) {
            items.add(entry.getDisplayText());
        }
        listView.setItems(items);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("播放历史");
        dialog.setHeaderText("最近播放的 " + history.size() + " 首歌曲");
        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = listView.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < history.size()) {
                    HistoryEntry entry = history.get(index);
                    Song song = new Song();
                    song.setId(entry.getSongId());
                    song.setTitle(entry.getTitle());
                    song.setArtist(entry.getArtist());
                    song.setFilePath(entry.getFilePath());
                    playlistService.addSong(song);
                    playlistService.playSong(playlistService.getCurrentPlaylist().getSongs().size() - 1);
                    dialog.close();
                }
            }
        });

        dialog.showAndWait();
    }

    @FXML
    public void onToggleLyrics() {
        lyricsWindow.toggle();
    }

    @FXML
    public void onToggleMiniPlayer() {
        miniPlayer.toggle();
    }

    @FXML
    public void onChangeTheme() {
        Map<String, String> themes = themeManager.getAvailableThemes();
        List<String> themeNames = new ArrayList<>(themes.keySet());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(themeManager.getCurrentTheme(), themeNames);
        dialog.setTitle("切换主题");
        dialog.setHeaderText(null);
        dialog.setContentText("选择主题：");
        dialog.showAndWait().ifPresent(theme -> {
            if (root.getScene() != null) {
                themeManager.applyTheme(root.getScene(), theme);
            }
        });
    }

    @FXML
    public void onShowPodcasts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/podcast_view.fxml"));
            BorderPane view = loader.load();
            Stage stage = new Stage();
            stage.setTitle("播客管理");
            stage.setScene(new Scene(view, 700, 500));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开播客窗口");
        }
    }

    @FXML
    public void onFormatConvert() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("音频文件", "*.wav", "*.au", "*.aiff"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;

        FileChooser saveChooser = new FileChooser();
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV 文件", "*.wav"));
        saveChooser.setInitialFileName(file.getName().replaceAll("\\.[^.]+$", ".wav"));
        File saveFile = saveChooser.showSaveDialog(primaryStage);
        if (saveFile == null) return;

        javafx.concurrent.Task<Void> task = FormatConverter.convertAsync(
            file.getAbsolutePath(), saveFile.getAbsolutePath(), "wav",
            new FormatConverter.ConversionCallback() {
                @Override public void onSuccess(String outputPath) {
                    DialogUtils.showInfo("转换完成", "文件已保存到:\n" + outputPath);
                }
                @Override public void onError(String error) {
                    DialogUtils.showError("转换失败", error);
                }
            }
        );
        new Thread(task).start();
    }

    @FXML
    public void onExportConfig() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        chooser.setInitialFileName("yinyue-backup.json");
        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            if (cloudSyncService.exportConfig(file.getAbsolutePath())) {
                DialogUtils.showInfo("导出成功", "配置已导出到:\n" + file.getAbsolutePath());
            } else {
                DialogUtils.showError("导出失败", "无法导出配置文件");
            }
        }
    }

    @FXML
    public void onImportConfig() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            if (DialogUtils.showConfirm("确认导入", "导入配置将覆盖当前设置，是否继续？")) {
                if (cloudSyncService.importConfig(file.getAbsolutePath())) {
                    DialogUtils.showInfo("导入成功", "配置已导入，请重启应用生效");
                } else {
                    DialogUtils.showError("导入失败", "无法导入配置文件");
                }
            }
        }
    }

    @FXML
    public void onBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择备份目录");
        File dir = chooser.showDialog(primaryStage);
        if (dir != null) {
            if (cloudSyncService.backupAll(dir.getAbsolutePath())) {
                DialogUtils.showInfo("备份成功", "配置已备份到:\n" + dir.getAbsolutePath());
            } else {
                DialogUtils.showError("备份失败", "无法备份配置文件");
            }
        }
    }

    public void setupGlobalShortcuts() {
        Scene scene = root.getScene();
        if (scene != null) {
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.SPACE && !e.isControlDown() && !e.isAltDown()) {
                    e.consume();
                    playerService.togglePlayPause();
                } else if (e.isControlDown() && e.getCode() == KeyCode.RIGHT) {
                    e.consume();
                    playlistService.playNext();
                } else if (e.isControlDown() && e.getCode() == KeyCode.LEFT) {
                    e.consume();
                    playlistService.playPrevious();
                } else if (e.isControlDown() && e.getCode() == KeyCode.E) {
                    e.consume();
                    onEqualizer();
                } else if (e.getCode() == KeyCode.M) {
                    e.consume();
                    playerService.setMute(!playerService.isMute());
                } else if (e.getCode() == KeyCode.F11) {
                    e.consume();
                    Stage stage = (Stage) scene.getWindow();
                    stage.setFullScreen(!stage.isFullScreen());
                } else if (e.isControlDown() && e.getCode() == KeyCode.L) {
                    e.consume();
                    onToggleLyrics();
                }
            });
        }
    }

    public void toggleMiniMode() {
        if (primaryStage != null) {
            double currentWidth = primaryStage.getWidth();
            if (currentWidth > 600) {
                primaryStage.setWidth(500);
                primaryStage.setHeight(150);
            } else {
                primaryStage.setWidth(config.getWindowWidth());
                primaryStage.setHeight(config.getWindowHeight());
            }
        }
    }

    @FXML public void onTogglePlaylist() { playlistSidebar.setVisible(!playlistSidebar.isVisible()); }

    // ===== Advanced Features =====

    @FXML
    public void onSmartLibrary() {
        List<String> options = List.of("按格式分类", "按文件大小分类", "按播放次数分类", "按目录分类", "最近添加", "最常播放", "最长歌曲");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("按格式分类", options);
        dialog.setTitle("智能分类");
        dialog.setHeaderText(null);
        dialog.setContentText("选择分类方式：");
        dialog.showAndWait().ifPresent(choice -> {
            List<Song> result = new ArrayList<>();
            switch (choice) {
                case "按格式分类":
                    Map<String, List<Song>> byFormat = smartLibraryService.groupByGenre(libraryService);
                    showSmartLibraryResult(byFormat);
                    return;
                case "按文件大小分类":
                    Map<String, List<Song>> bySize = smartLibraryService.groupByFileSize(libraryService);
                    showSmartLibraryResult(bySize);
                    return;
                case "按播放次数分类":
                    Map<String, List<Song>> byCount = smartLibraryService.groupByPlayCount(libraryService);
                    showSmartLibraryResult(byCount);
                    return;
                case "按目录分类":
                    Map<String, List<Song>> byDir = smartLibraryService.groupByDirectory(libraryService);
                    showSmartLibraryResult(byDir);
                    return;
                case "最近添加":
                    result = smartLibraryService.getRecentlyAdded(libraryService, 30);
                    break;
                case "最常播放":
                    result = smartLibraryService.getMostPlayed(libraryService, 30);
                    break;
                case "最长歌曲":
                    result = smartLibraryService.getLongestSongs(libraryService, 30);
                    break;
            }
            if (!result.isEmpty()) {
                playlistService.clearCurrentPlaylist();
                playlistService.addSongs(result);
                updatePlaylistView();
                DialogUtils.showInfo("分类结果", "已将 " + result.size() + " 首歌曲添加到播放列表");
            }
        });
    }

    private void showSmartLibraryResult(Map<String, List<Song>> groups) {
        ListView<String> listView = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Map.Entry<String, List<Song>> entry : groups.entrySet()) {
            items.add(entry.getKey() + " (" + entry.getValue().size() + " 首)");
        }
        listView.setItems(items);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("智能分类结果");
        dialog.setHeaderText("共 " + groups.size() + " 个分类");
        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = listView.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    String key = new ArrayList<>(groups.keySet()).get(index);
                    List<Song> songs = groups.get(key);
                    playlistService.clearCurrentPlaylist();
                    playlistService.addSongs(songs);
                    updatePlaylistView();
                    dialog.close();
                }
            }
        });
        dialog.showAndWait();
    }

    @FXML
    public void onRecommend() {
        List<String> options = List.of("智能推荐", "发现新歌曲");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("智能推荐", options);
        dialog.setTitle("歌曲推荐");
        dialog.setHeaderText(null);
        dialog.setContentText("选择推荐模式：");
        dialog.showAndWait().ifPresent(choice -> {
            List<Song> songs;
            if (choice.equals("智能推荐")) {
                songs = recommendationEngine.recommend(libraryService, historyService, 20);
            } else {
                songs = recommendationEngine.discoverNew(libraryService, historyService, 20);
            }
            if (songs.isEmpty()) {
                DialogUtils.showInfo("推荐结果", "暂无推荐歌曲");
                return;
            }
            playlistService.clearCurrentPlaylist();
            playlistService.addSongs(songs);
            updatePlaylistView();
            if (config.isAutoPlay()) playlistService.playSong(0);
            DialogUtils.showInfo("推荐完成", "已添加 " + songs.size() + " 首推荐歌曲到播放列表");
        });
    }

    @FXML
    public void onRadio() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/radio_view.fxml"));
            BorderPane view = loader.load();
            Stage stage = new Stage();
            stage.setTitle("网络电台");
            stage.setScene(new Scene(view, 600, 450));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开电台窗口");
        }
    }

    @FXML
    public void onRecord() {
        if (audioRecorder.isRecording()) {
            audioRecorder.stopRecording();
            return;
        }
        if (!AudioRecorder.isMicrophoneAvailable()) {
            DialogUtils.showError("错误", "未检测到麦克风设备");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV 文件", "*.wav"));
        chooser.setInitialFileName("recording_" + System.currentTimeMillis() + ".wav");
        File file = chooser.showSaveDialog(primaryStage);
        if (file == null) return;

        audioRecorder.startRecording(file, new AudioRecorder.RecordingCallback() {
            @Override public void onStarted() {
                DialogUtils.showInfo("录音", "开始录音...");
            }
            @Override public void onStopped(File f) {
                DialogUtils.showInfo("录音完成", "文件已保存到:\n" + f.getAbsolutePath());
            }
            @Override public void onError(String error) {
                DialogUtils.showError("录音失败", error);
            }
        });
    }

    @FXML
    public void onEditTags() {
        Song song = playerService.getCurrentSong();
        if (song == null) {
            int index = libraryListView.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                song = libraryService.getSongs().get(index);
            }
        }
        if (song == null) {
            DialogUtils.showWarning("提示", "请先选择一首歌曲");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tag_editor_view.fxml"));
            BorderPane view = loader.load();
            TagEditorController controller = loader.getController();
            controller.setSong(song);
            Stage stage = new Stage();
            stage.setTitle("编辑标签 - " + song.getDisplayTitle());
            stage.setScene(new Scene(view, 450, 320));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            controller.setStage(stage);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开标签编辑器");
        }
    }

    @FXML
    public void onRemoteControl() {
        if (remoteControlServer.isRunning()) {
            remoteControlServer.stop();
            DialogUtils.showInfo("远程控制", "远程控制服务器已停止");
        } else {
            try {
                remoteControlServer.start();
                String url = "http://localhost:" + remoteControlServer.getPort() + "/";
                DialogUtils.showInfo("远程控制已启动", "请在浏览器中访问:\n" + url);
            } catch (Exception e) {
                DialogUtils.showError("启动失败", "无法启动远程控制服务器: " + e.getMessage());
            }
        }
    }

    // ===== V2 New Features =====

    @FXML
    public void onEffectPlugins() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/effect_plugin_view.fxml"));
            BorderPane view = loader.load();
            Stage stage = new Stage();
            stage.setTitle("音效插件");
            stage.setScene(new Scene(view, 400, 380));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            loader.getController(); // initialize
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开音效插件窗口");
        }
    }

    @FXML
    public void onCrossfade() {
        CrossfadeService crossfade = CrossfadeService.getInstance();
        crossfade.setEnabled(!crossfade.isEnabled());
        DialogUtils.showInfo("淡入淡出", "淡入淡出已" + (crossfade.isEnabled() ? "开启" : "关闭"));
    }

    @FXML
    public void onPlaybackSpeed() {
        PlaybackSpeedService speedService = PlaybackSpeedService.getInstance();
        List<String> options = new ArrayList<>();
        for (double s : speedService.getSpeedOptions()) {
            options.add(String.format("%.2fx", s));
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(speedService.getSpeedDisplay(), options);
        dialog.setTitle("变速播放");
        dialog.setHeaderText(null);
        dialog.setContentText("选择播放速度：");
        dialog.showAndWait().ifPresent(choice -> {
            double speed = Double.parseDouble(choice.replace("x", ""));
            speedService.setSpeed(speed);
        });
    }

    @FXML
    public void onSetAPoint() {
        long current = playerService.getCurrentTime();
        ABLoopService.getInstance().setPointA(current);
        DialogUtils.showInfo("A 点已设置", "A 点: " + formatMs(current));
    }

    @FXML
    public void onSetBPoint() {
        long current = playerService.getCurrentTime();
        ABLoopService.getInstance().setPointB(current);
        DialogUtils.showInfo("B 点已设置", ABLoopService.getInstance().getLoopInfo());
    }

    @FXML
    public void onClearABLoop() {
        ABLoopService.getInstance().clearLoop();
        DialogUtils.showInfo("AB 循环", "AB 循环已清除");
    }

    @FXML
    public void onReplayGain() {
        DialogUtils.showInfo("音量均衡化", "正在分析音乐库，请稍候...");
        new Thread(() -> {
            ReplayGainUtil.normalizeLibrary();
            Platform.runLater(() -> DialogUtils.showInfo("完成", "音量均衡化分析完成"));
        }).start();
    }

    @FXML
    public void onShowStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_view.fxml"));
            BorderPane view = loader.load();
            Stage stage = new Stage();
            stage.setTitle("听歌统计");
            stage.setScene(new Scene(view, 700, 500));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开统计窗口");
        }
    }

    @FXML
    public void onAudioSplit() {
        Song song = playerService.getCurrentSong();
        if (song == null) {
            DialogUtils.showWarning("提示", "请先播放一首歌曲");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/audio_split_view.fxml"));
            BorderPane view = loader.load();
            AudioSplitController controller = loader.getController();
            controller.setInputFile(song.getFilePath(), playerService.getDuration());
            Stage stage = new Stage();
            stage.setTitle("音频分割");
            stage.setScene(new Scene(view, 550, 350));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(primaryStage);
            controller.setStage(stage);
            stage.show();
        } catch (Exception e) {
            DialogUtils.showError("错误", "无法打开音频分割窗口");
        }
    }

    @FXML
    public void onExportPlaylist() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("M3U 播放列表", "*.m3u"),
            new FileChooser.ExtensionFilter("PLS 播放列表", "*.pls")
        );
        File file = chooser.showSaveDialog(primaryStage);
        if (file == null) return;
        try {
            if (file.getName().endsWith(".m3u")) {
                PlaylistIO.exportM3U(playlistService.getCurrentPlaylist(), file.getAbsolutePath());
            } else {
                PlaylistIO.exportPLS(playlistService.getCurrentPlaylist(), file.getAbsolutePath());
            }
            DialogUtils.showInfo("导出成功", "播放列表已导出到:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            DialogUtils.showError("导出失败", e.getMessage());
        }
    }

    @FXML
    public void onImportPlaylist() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("播放列表文件", "*.m3u", "*.pls")
        );
        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;
        try {
            List<String> paths;
            if (file.getName().endsWith(".m3u")) {
                paths = PlaylistIO.importM3U(file.getAbsolutePath());
            } else {
                paths = PlaylistIO.importPLS(file.getAbsolutePath());
            }
            for (String path : paths) {
                File f = new File(path);
                if (f.exists()) {
                    Song song = new Song();
                    song.setId(java.util.UUID.randomUUID().toString());
                    song.setFilePath(f.getAbsolutePath());
                    song.setFileSize(f.length());
                    song.setTitle(f.getName().replaceAll("\\.[^.]+$", ""));
                    playlistService.addSong(song);
                }
            }
            updatePlaylistView();
            DialogUtils.showInfo("导入成功", "已导入 " + paths.size() + " 首歌曲");
        } catch (Exception e) {
            DialogUtils.showError("导入失败", e.getMessage());
        }
    }

    @FXML
    public void onCheckUpdate() {
        DialogUtils.showInfo("检查更新", "正在检查更新...");
        UpdateChecker.getInstance().checkForUpdates(new UpdateChecker.UpdateCallback() {
            @Override public void onUpdateAvailable(String ver, String url, String notes) {
                DialogUtils.showInfo("发现新版本", "最新版本: " + ver + "\n\n" + (notes != null ? notes : "") + "\n\n下载地址:\n" + url);
            }
            @Override public void onUpToDate() {
                DialogUtils.showInfo("检查更新", "当前已是最新版本 v" + UpdateChecker.getInstance().getCurrentVersion());
            }
            @Override public void onError(String error) {
                DialogUtils.showError("检查失败", "无法检查更新: " + error);
            }
        });
    }

    @FXML
    public void onNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog("新播放列表");
        dialog.setTitle("新建播放列表");
        dialog.setHeaderText(null);
        dialog.setContentText("播放列表名称：");
        dialog.showAndWait().ifPresent(name -> {
            playlistService.createPlaylist(name);
            DialogUtils.showInfo("创建成功", "已创建播放列表: " + name);
        });
    }

    private String formatMs(long ms) {
        int sec = (int) (ms / 1000);
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
