package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;

public class RadioService {
    private static RadioService instance;
    private MediaPlayer mediaPlayer;
    private final List<RadioStation> stations;
    private RadioStation currentStation;
    private boolean isPlaying = false;

    public static RadioService getInstance() {
        if (instance == null) instance = new RadioService();
        return instance;
    }

    private RadioService() {
        stations = new ArrayList<>();
        loadDefaultStations();
    }

    private void loadDefaultStations() {
        stations.add(new RadioStation("轻松FM", "http://media-ice.musicradio.com/LBCUKMP3Low", "新闻"));
        stations.add(new RadioStation("古典音乐", "http://live-radio01.mediahubaustralia.com/2FMW/mp3/", "古典"));
        stations.add(new RadioStation("爵士电台", "http://live-radio01.mediahubaustralia.com/2JAZ/mp3/", "爵士"));
        stations.add(new RadioStation("电子音乐", "http://streaming.radionomy.com/JamendoLounge", "电子"));
        stations.add(new RadioStation("流行金曲", "http://stream.zeno.fm/0r0xa792kwzuv", "流行"));
        stations.add(new RadioStation("摇滚频道", "http://stream.zeno.fm/6r9kppg7pzzuv", "摇滚"));
        stations.add(new RadioStation("轻音乐", "http://streaming.radionomy.com/ABC-Lounge", "轻音乐"));
        stations.add(new RadioStation("中国之声", "http://ngcdn001.cnr.cn/live/zgzs/index.m3u8", "综合"));
        stations.add(new RadioStation("音乐之声", "http://ngcdn003.cnr.cn/live/yyzs/index.m3u8", "音乐"));
    }

    public void playStation(RadioStation station) {
        stop();
        try {
            Media media = new Media(station.getUrl());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();
            currentStation = station;
            isPlaying = true;
        } catch (Exception e) {
            isPlaying = false;
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        isPlaying = false;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public RadioStation getCurrentStation() {
        return currentStation;
    }

    public List<RadioStation> getStations() {
        return new ArrayList<>(stations);
    }

    public void addStation(String name, String url, String genre) {
        stations.add(new RadioStation(name, url, genre));
    }

    public void removeStation(String name) {
        stations.removeIf(s -> s.getName().equals(name));
    }

    public static class RadioStation {
        private String name;
        private String url;
        private String genre;

        public RadioStation(String name, String url, String genre) {
            this.name = name;
            this.url = url;
            this.genre = genre;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        @Override
        public String toString() {
            return name + " [" + genre + "]";
        }
    }
}
