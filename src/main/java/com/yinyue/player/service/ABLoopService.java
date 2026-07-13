package com.yinyue.player.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class ABLoopService {
    private static ABLoopService instance;
    private long pointA = -1;
    private long pointB = -1;
    private boolean enabled = false;
    private Timeline loopTimeline;

    public static ABLoopService getInstance() {
        if (instance == null) instance = new ABLoopService();
        return instance;
    }

    private ABLoopService() {}

    public void setPointA(long timeMs) {
        this.pointA = timeMs;
        this.enabled = pointA >= 0 && pointB >= 0;
        startLoopCheck();
    }

    public void setPointB(long timeMs) {
        this.pointB = timeMs;
        this.enabled = pointA >= 0 && pointB >= 0 && pointB > pointA;
        startLoopCheck();
    }

    public void setLoop(long a, long b) {
        this.pointA = a;
        this.pointB = b;
        this.enabled = a >= 0 && b > a;
        startLoopCheck();
    }

    public void clearLoop() {
        pointA = -1;
        pointB = -1;
        enabled = false;
        if (loopTimeline != null) {
            loopTimeline.stop();
            loopTimeline = null;
        }
    }

    private void startLoopCheck() {
        if (!enabled) return;
        if (loopTimeline != null) loopTimeline.stop();

        loopTimeline = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            AudioPlayerService ps = AudioPlayerService.getInstance();
            if (ps.isPlaying() && pointB > 0) {
                long current = ps.getCurrentTime();
                if (current >= pointB) {
                    ps.seekTo(pointA);
                }
            }
        }));
        loopTimeline.setCycleCount(Timeline.INDEFINITE);
        loopTimeline.play();
    }

    public boolean isEnabled() { return enabled; }
    public long getPointA() { return pointA; }
    public long getPointB() { return pointB; }

    public String getLoopInfo() {
        if (!enabled) return "AB循环: 未设置";
        return String.format("AB循环: %s → %s", formatTime(pointA), formatTime(pointB));
    }

    private String formatTime(long ms) {
        int sec = (int) (ms / 1000);
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
