package com.yinyue.player.controller;

import com.yinyue.player.service.PlaylistSnapshotService;
import com.yinyue.player.util.DialogUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.List;

public class SnapshotController {
    @FXML private ListView<String> snapshotList;
    @FXML private Button restoreButton;
    @FXML private Button deleteButton;

    private ObservableList<String> items;
    private Stage stage;

    @FXML
    public void initialize() {
        items = FXCollections.observableArrayList();
        snapshotList.setItems(items);
        refreshList();

        snapshotList.getSelectionModel().selectedItemProperty().addListener((obs, o, v) -> {
            restoreButton.setDisable(v == null);
            deleteButton.setDisable(v == null);
        });
    }

    public void setStage(Stage stage) { this.stage = stage; }

    private void refreshList() {
        items.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (PlaylistSnapshotService.SnapshotInfo info : PlaylistSnapshotService.getInstance().listSnapshots()) {
            items.add(info.filename + " (" + sdf.format(info.modified) + ")");
        }
    }

    @FXML
    public void onSave() {
        TextInputDialog dialog = new TextInputDialog("快照");
        dialog.setTitle("保存快照");
        dialog.setHeaderText(null);
        dialog.setContentText("快照名称：");
        dialog.showAndWait().ifPresent(name -> {
            PlaylistSnapshotService.getInstance().saveSnapshot(name);
            refreshList();
            DialogUtils.showInfo("保存成功", "播放列表快照已保存");
        });
    }

    @FXML
    public void onRestore() {
        String selected = snapshotList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String filename = selected.split(" \\(")[0];
            PlaylistSnapshotService.getInstance().restoreSnapshot(filename);
            DialogUtils.showInfo("恢复成功", "播放列表快照已恢复");
        }
    }

    @FXML
    public void onDelete() {
        String selected = snapshotList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (DialogUtils.showConfirm("确认删除", "确定要删除此快照吗？")) {
                String filename = selected.split(" \\(")[0];
                PlaylistSnapshotService.getInstance().deleteSnapshot(filename);
                refreshList();
            }
        }
    }

    @FXML
    public void onClose() {
        if (stage != null) stage.close();
    }
}