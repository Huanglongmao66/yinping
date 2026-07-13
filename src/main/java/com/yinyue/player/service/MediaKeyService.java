package com.yinyue.player.service;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class MediaKeyService {
    private static MediaKeyService instance;

    public static MediaKeyService getInstance() {
        if (instance == null) instance = new MediaKeyService();
        return instance;
    }

    private MediaKeyService() {}

    public void initialize(Scene scene) {
        AudioPlayerService playerService = AudioPlayerService.getInstance();
        PlaylistService playlistService = PlaylistService.getInstance();

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case PLAY:
                case PAUSE:
                    e.consume();
                    if (playerService.isPlaying()) {
                        playerService.pause();
                    } else {
                        playerService.resume();
                    }
                    break;
                case STOP:
                    e.consume();
                    playerService.stop();
                    break;
                case TRACK_PREV:
                    e.consume();
                    playlistService.playPrevious();
                    break;
                case TRACK_NEXT:
                    e.consume();
                    playlistService.playNext();
                    break;
                default:
                    break;
            }
        });
    }
}