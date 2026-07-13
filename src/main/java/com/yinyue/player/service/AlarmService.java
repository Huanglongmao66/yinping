package com.yinyue.player.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;

import java.util.Calendar;
import java.util.Date;

public class AlarmService {
    private static AlarmService instance;
    private Timeline alarmTimeline;
    private Date alarmTime;
    private String alarmPlaylist;
    private boolean enabled = false;

    public static AlarmService getInstance() {
        if (instance == null) instance = new AlarmService();
        return instance;
    }

    private AlarmService() {}

    public void setAlarm(int hour, int minute, String playlistName) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTime().before(new Date())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        alarmTime = cal.getTime();
        alarmPlaylist = playlistName;
        enabled = true;
        startAlarmCheck();
    }

    public void cancelAlarm() {
        enabled = false;
        alarmTime = null;
        if (alarmTimeline != null) {
            alarmTimeline.stop();
            alarmTimeline = null;
        }
    }

    private void startAlarmCheck() {
        if (alarmTimeline != null) alarmTimeline.stop();
        alarmTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (!enabled || alarmTime == null) return;
            if (new Date().after(alarmTime)) {
                triggerAlarm();
                cancelAlarm();
            }
        }));
        alarmTimeline.setCycleCount(Timeline.INDEFINITE);
        alarmTimeline.play();
    }

    private void triggerAlarm() {
        Platform.runLater(() -> {
            if (alarmPlaylist != null) {
                PlaylistService ps = PlaylistService.getInstance();
                for (com.yinyue.player.model.Playlist pl : ps.getAllPlaylists()) {
                    if (pl.getName().equals(alarmPlaylist)) {
                        ps.setCurrentPlaylist(pl);
                        if (!pl.getSongs().isEmpty()) {
                            ps.playSong(0);
                        }
                        break;
                    }
                }
            } else {
                AudioPlayerService.getInstance().resume();
            }
        });
    }

    public boolean isEnabled() { return enabled; }
    public Date getAlarmTime() { return alarmTime; }
    public String getAlarmPlaylist() { return alarmPlaylist; }
}