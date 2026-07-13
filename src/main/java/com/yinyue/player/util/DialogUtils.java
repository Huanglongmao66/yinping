package com.yinyue.player.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Optional;

public class DialogUtils {
    private static Stage ownerStage;

    public static void setOwnerStage(Stage stage) {
        ownerStage = stage;
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        configureDialog(alert);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        configureDialog(alert);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        configureDialog(alert);
        alert.showAndWait();
    }

    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        configureDialog(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static String showInput(String title, String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        configureDialog(dialog);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private static void configureDialog(javafx.scene.control.Dialog<?> dialog) {
        if (ownerStage != null) {
            dialog.initOwner(ownerStage);
        }
        dialog.getDialogPane().getStylesheets().add(
                DialogUtils.class.getResource("/css/dark_theme.css").toExternalForm()
        );
    }
}