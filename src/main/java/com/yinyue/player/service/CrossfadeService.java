package com.yinyue.player.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class CrossfadeService {
    private static CrossfadeService instance;
    private boolean enabled = false;
    private double fadeDuration = 3.0; // seconds
    private Timeline fadeOutTimeline;
    private Timeline fadeInTimeline;

    public static CrossfadeService getInstance() {
        if (instance == null) instance = new CrossfadeService();
        return instance;
    }

    private CrossfadeService() {}

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setFadeDuration(double seconds) {
        this.fadeDuration = seconds;
    }

    public double getFadeDuration() {
        return fadeDuration;
    }

    public void fadeIn(AudioPlayerService playerService) {
        if (!enabled) return;
        double targetVolume = playerService.getVolume();
        int steps = 30;
        double stepDuration = fadeDuration * 1000 / steps;
        double volumeStep = targetVolume / steps;

        playerService.setVolume(0);
        if (fadeInTimeline != null) fadeInTimeline.stop();

        fadeInTimeline = new Timeline();
        for (int i = 1; i <= steps; i++) {
            final double vol = Math.min(volumeStep * i, targetVolume);
            KeyFrame kf = new KeyFrame(Duration.millis(stepDuration * i), e -> {
                playerService.setVolume(vol);
            });
            fadeInTimeline.getKeyFrames().add(kf);
        }
        fadeInTimeline.play();
    }

    public void fadeOut(AudioPlayerService playerService, Runnable onComplete) {
        if (!enabled) {
            if (onComplete != null) onComplete.run();
            return;
        }
        double currentVolume = playerService.getVolume();
        int steps = 30;
        double stepDuration = fadeDuration * 1000 / steps;
        double volumeStep = currentVolume / steps;

        if (fadeOutTimeline != null) fadeOutTimeline.stop();

        fadeOutTimeline = new Timeline();
        for (int i = 1; i <= steps; i++) {
            final double vol = Math.max(currentVolume - volumeStep * i, 0);
            KeyFrame kf = new KeyFrame(Duration.millis(stepDuration * i), e -> {
                playerService.setVolume(vol);
            });
            fadeOutTimeline.getKeyFrames().add(kf);
        }
        fadeOutTimeline.setOnFinished(e -> {
            if (onComplete != null) onComplete.run();
        });
        fadeOutTimeline.play();
    }

    public void stopAll() {
        if (fadeOutTimeline != null) fadeOutTimeline.stop();
        if (fadeInTimeline != null) fadeInTimeline.stop();
    }
}
