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
}
