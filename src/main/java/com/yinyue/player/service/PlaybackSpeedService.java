package com.yinyue.player.service;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class PlaybackSpeedService {
    private static PlaybackSpeedService instance;
    private final DoubleProperty speed = new SimpleDoubleProperty(1.0);
    private static final double[] SPEED_OPTIONS = {0.5, 0.75, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0};

    public static PlaybackSpeedService getInstance() {
        if (instance == null) instance = new PlaybackSpeedService();
        return instance;
    }

    private PlaybackSpeedService() {}

    public void setSpeed(double speed) {
        this.speed.set(Math.max(0.25, Math.min(4.0, speed)));
    }

    public double getSpeed() {
        return speed.get();
    }

    public DoubleProperty speedProperty() {
        return speed;
    }

    public double[] getSpeedOptions() {
        return SPEED_OPTIONS;
    }

    public String getSpeedDisplay() {
        double s = speed.get();
        if (s == Math.floor(s) && s < 10) {
            return (int) s + ".0x";
        }
        return String.format("%.2fx", s);
    }

    public void increaseSpeed() {
        double current = speed.get();
        for (double opt : SPEED_OPTIONS) {
            if (opt > current + 0.01) {
                speed.set(opt);
                return;
            }
        }
    }

    public void decreaseSpeed() {
        double current = speed.get();
        for (int i = SPEED_OPTIONS.length - 1; i >= 0; i--) {
            if (SPEED_OPTIONS[i] < current - 0.01) {
                speed.set(SPEED_OPTIONS[i]);
                return;
            }
        }
    }

    public void resetSpeed() {
        speed.set(1.0);
    }
}
