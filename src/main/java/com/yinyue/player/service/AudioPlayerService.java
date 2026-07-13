package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.ConfigManager;
import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

public class AudioPlayerService {
    private static AudioPlayerService instance;

    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private boolean isPlaying;

    private final DoubleProperty volume = new SimpleDoubleProperty();
    private final BooleanProperty mute = new SimpleBooleanProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final LongProperty currentTime = new SimpleLongProperty();
    private final LongProperty duration = new SimpleLongProperty();
    private final ObjectProperty<Song> currentSongProperty = new SimpleObjectProperty<>();
    private final BooleanProperty playingProperty = new SimpleBooleanProperty();

    public static AudioPlayerService getInstance() {
        if (instance == null) {
            instance = new AudioPlayerService();
        }
        return instance;
    }

    private AudioPlayerService() {
        ConfigManager config = ConfigManager.getInstance();
        volume.set(config.getVolume());
        mute.set(config.isMute());

        volume.addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
            config.setVolume(newVal.doubleValue());
        });

        mute.addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setMute(newVal);
            }
            config.setMute(newVal);
        });
    }

    public void play(Song song) {
        stop();

        if (song == null || song.getFilePath() == null) {
            return;
        }

        this.currentSong = song;
        currentSongProperty.set(song);

        try {
            File file = new File(song.getFilePath());
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.volumeProperty().bindBidirectional(volume);
            mediaPlayer.muteProperty().bindBidirectional(mute);

            mediaPlayer.setOnReady(() -> {
                duration.set((long) (mediaPlayer.getTotalDuration().toMillis()));
                mediaPlayer.play();
                isPlaying = true;
                playingProperty.set(true);
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                long current = (long) newVal.toMillis();
                currentTime.set(current);
                if (duration.get() > 0) {
                    progress.set(current / (double) duration.get());
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                isPlaying = false;
                playingProperty.set(false);
            });

            mediaPlayer.setOnError(() -> {
                isPlaying = false;
                playingProperty.set(false);
            });

            song.setPlayCount(song.getPlayCount() + 1);
        } catch (Exception e) {
            currentSong = null;
            currentSongProperty.set(null);
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
            playingProperty.set(false);
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            isPlaying = true;
            playingProperty.set(true);
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.volumeProperty().unbindBidirectional(volume);
                mediaPlayer.muteProperty().unbindBidirectional(mute);
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception e) {
                // ignore
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        playingProperty.set(false);
        currentTime.set(0);
        progress.set(0);
        duration.set(0);
    }

    public void seek(double percentage) {
        if (mediaPlayer != null && duration.get() > 0) {
            long seekTime = (long) (percentage * duration.get());
            mediaPlayer.seek(javafx.util.Duration.millis(seekTime));
        }
    }

    public void seekTo(long milliseconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(javafx.util.Duration.millis(milliseconds));
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public long getCurrentTime() {
        return currentTime.get();
    }

    public long getDuration() {
        return duration.get();
    }

    public double getProgress() {
        return progress.get();
    }

    public double getVolume() {
        return volume.get();
    }

    public void setVolume(double volume) {
        this.volume.set(volume);
    }

    public boolean isMute() {
        return mute.get();
    }

    public void setMute(boolean mute) {
        this.mute.set(mute);
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public BooleanProperty muteProperty() {
        return mute;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public LongProperty currentTimeProperty() {
        return currentTime;
    }

    public LongProperty durationProperty() {
        return duration;
    }

    public ObjectProperty<Song> currentSongProperty() {
        return currentSongProperty;
    }

    public BooleanProperty playingProperty() {
        return playingProperty;
    }

    public void togglePlayPause() {
        if (isPlaying) {
            pause();
        } else {
            if (currentSong != null) {
                resume();
            }
        }
    }
}