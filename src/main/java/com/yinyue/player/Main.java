package com.yinyue.player;

import com.yinyue.player.controller.MainController;
import com.yinyue.player.util.FileUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class Main extends Application {
    private Stage primaryStage;
    private MainController controller;
    private TrayIcon trayIcon;

    public static void main(String[] args) {
        FileUtils.ensureAllDirectories();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_view.fxml"));
            BorderPane root = loader.load();
            controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/dark_theme.css").toExternalForm());

            primaryStage.setTitle("音悦播放器");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> {
                e.consume();
                primaryStage.hide();
                showTrayMessage("音悦播放器", "已最小化到系统托盘");
            });

            controller.setupGlobalShortcuts();
            setupSystemTray();

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            return;
        }

        try {
            PopupMenu popupMenu = new PopupMenu();

            MenuItem showItem = new MenuItem("显示窗口");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));
            popupMenu.add(showItem);

            MenuItem playPauseItem = new MenuItem("播放/暂停");
            playPauseItem.addActionListener(e -> Platform.runLater(() -> {
                if (controller != null) {
                    try {
                        controller.onPlay();
                    } catch (Exception ex) {
                        controller.onPause();
                    }
                }
            }));
            popupMenu.add(playPauseItem);

            MenuItem previousItem = new MenuItem("上一曲");
            previousItem.addActionListener(e -> Platform.runLater(() -> {
                if (controller != null) {
                    controller.onPrevious();
                }
            }));
            popupMenu.add(previousItem);

            MenuItem nextItem = new MenuItem("下一曲");
            nextItem.addActionListener(e -> Platform.runLater(() -> {
                if (controller != null) {
                    controller.onNext();
                }
            }));
            popupMenu.add(nextItem);

            popupMenu.addSeparator();

            MenuItem exitItem = new MenuItem("退出");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });
            popupMenu.add(exitItem);

            trayIcon = new TrayIcon(createTrayImage(), "音悦播放器", popupMenu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (primaryStage.isShowing()) {
                    primaryStage.hide();
                } else {
                    primaryStage.show();
                    primaryStage.toFront();
                }
            }));

            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            // ignore
        }
    }

    private java.awt.Image createTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(138, 43, 226));
        g2d.fillOval(2, 2, 12, 12);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(5, 5, 2, 6);
        g2d.fillRect(9, 5, 2, 6);
        g2d.dispose();
        return image;
    }

    private void showTrayMessage(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    @Override
    public void stop() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}