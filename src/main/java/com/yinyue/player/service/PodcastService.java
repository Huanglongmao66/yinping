package com.yinyue.player.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yinyue.player.model.Song;
import com.yinyue.player.util.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class PodcastService {
    private static PodcastService instance;
    private final List<PodcastFeed> feeds;
    private final Gson gson;
    private static final String FEEDS_FILE = FileUtils.getConfigDirectory() + "/podcasts.json";

    public static PodcastService getInstance() {
        if (instance == null) {
            instance = new PodcastService();
        }
        return instance;
    }

    private PodcastService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        feeds = loadFeeds();
    }

    private List<PodcastFeed> loadFeeds() {
        File file = new File(FEEDS_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<PodcastFeed>>() {}.getType();
                List<PodcastFeed> loaded = gson.fromJson(reader, listType);
                return loaded != null ? loaded : new ArrayList<>();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    public void saveFeeds() {
        try (Writer writer = new FileWriter(FEEDS_FILE)) {
            gson.toJson(feeds, writer);
        } catch (Exception e) {
            // ignore
        }
    }

    public void addFeed(String name, String url) {
        PodcastFeed feed = new PodcastFeed(name, url);
        feeds.add(feed);
        saveFeeds();
    }

    public void removeFeed(String url) {
        feeds.removeIf(f -> f.getUrl().equals(url));
        saveFeeds();
    }

    public List<PodcastFeed> getFeeds() {
        return new ArrayList<>(feeds);
    }

    public List<PodcastEpisode> fetchEpisodes(String feedUrl) {
        List<PodcastEpisode> episodes = new ArrayList<>();
        try {
            URL url = new URL(feedUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                StringBuilder xml = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    xml.append(line);
                }
                episodes = parseRSS(xml.toString());
            }
        } catch (Exception e) {
            // ignore
        }
        return episodes;
    }

    private List<PodcastEpisode> parseRSS(String xml) {
        List<PodcastEpisode> episodes = new ArrayList<>();
        String[] items = xml.split("<item>");
        for (int i = 1; i < items.length; i++) {
            String item = items[i];
            PodcastEpisode ep = new PodcastEpisode();
            ep.setTitle(extractTag(item, "title"));
            ep.setDescription(extractTag(item, "description"));
            ep.setPubDate(extractTag(item, "pubDate"));
            ep.setDuration(extractTag(item, "itunes:duration"));
            ep.setAudioUrl(extractEnclosureUrl(item));
            if (ep.getAudioUrl() != null) {
                episodes.add(ep);
            }
        }
        return episodes;
    }

    private String extractTag(String xml, String tag) {
        String start = "<" + tag + ">";
        String end = "</" + tag + ">";
        int s = xml.indexOf(start);
        int e = xml.indexOf(end);
        if (s >= 0 && e > s) {
            return xml.substring(s + start.length(), e).trim();
        }
        // Try with namespace
        int nsIdx = xml.indexOf(":" + tag + ">");
        if (nsIdx > 0) {
            int realStart = xml.lastIndexOf("<", nsIdx);
            String fullTag = xml.substring(realStart + 1, nsIdx + tag.length() + 1);
            String closeTag = "</" + fullTag + ">";
            int closeIdx = xml.indexOf(closeTag);
            if (closeIdx > realStart) {
                return xml.substring(nsIdx + tag.length() + 2, closeIdx).trim();
            }
        }
        return null;
    }

    private String extractEnclosureUrl(String xml) {
        int idx = xml.indexOf("<enclosure");
        if (idx >= 0) {
            int urlIdx = xml.indexOf("url=\"", idx);
            if (urlIdx >= 0) {
                int endIdx = xml.indexOf("\"", urlIdx + 5);
                if (endIdx > urlIdx) {
                    return xml.substring(urlIdx + 5, endIdx);
                }
            }
        }
        return null;
    }

    public static class PodcastFeed {
        private String name;
        private String url;
        private Date addedAt;

        public PodcastFeed() {}

        public PodcastFeed(String name, String url) {
            this.name = name;
            this.url = url;
            this.addedAt = new Date();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Date getAddedAt() { return addedAt; }
        public void setAddedAt(Date addedAt) { this.addedAt = addedAt; }
    }

    public static class PodcastEpisode {
        private String title;
        private String description;
        private String pubDate;
        private String duration;
        private String audioUrl;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPubDate() { return pubDate; }
        public void setPubDate(String pubDate) { this.pubDate = pubDate; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    }
}
