package com.yinyue.player.controller;

import com.yinyue.player.service.AudioPlayerService;
import com.yinyue.player.service.PodcastService;
import com.yinyue.player.util.DialogUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class PodcastController {
    @FXML private TextField feedNameField;
    @FXML private TextField feedUrlField;
    @FXML private ListView<String> feedListView;
    @FXML private ListView<String> episodeListView;

    private PodcastService podcastService;
    private ObservableList<String> feedItems;
    private ObservableList<String> episodeItems;

    @FXML
    public void initialize() {
        podcastService = PodcastService.getInstance();
        feedItems = FXCollections.observableArrayList();
        episodeItems = FXCollections.observableArrayList();
        feedListView.setItems(feedItems);
        episodeListView.setItems(episodeItems);
        updateFeedList();

        feedListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                int index = feedListView.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    loadEpisodes(index);
                }
            }
        });

        episodeListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int index = episodeListView.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    playEpisode(index);
                }
            }
        });
    }

    private void updateFeedList() {
        feedItems.clear();
        for (PodcastService.PodcastFeed feed : podcastService.getFeeds()) {
            feedItems.add(feed.getName());
        }
    }

    @FXML
    public void onAddFeed() {
        String name = feedNameField.getText();
        String url = feedUrlField.getText();
        if (name == null || name.isEmpty() || url == null || url.isEmpty()) {
            DialogUtils.showWarning("提示", "请输入播客名称和RSS地址");
            return;
        }
        podcastService.addFeed(name, url);
        updateFeedList();
        feedNameField.clear();
        feedUrlField.clear();
    }

    @FXML
    public void onRefresh() {
        updateFeedList();
        episodeItems.clear();
    }

    private void loadEpisodes(int feedIndex) {
        if (feedIndex < 0 || feedIndex >= podcastService.getFeeds().size()) return;
        PodcastService.PodcastFeed feed = podcastService.getFeeds().get(feedIndex);
        episodeItems.clear();
        java.util.List<PodcastService.PodcastEpisode> episodes = podcastService.fetchEpisodes(feed.getUrl());
        for (PodcastService.PodcastEpisode ep : episodes) {
            episodeItems.add(ep.getTitle() + (ep.getDuration() != null ? " (" + ep.getDuration() + ")" : ""));
        }
    }

    private void playEpisode(int index) {
        // This is a placeholder - in a real app, you'd play the podcast episode URL
        DialogUtils.showInfo("提示", "播客播放功能需要网络音频流支持");
    }
}
