package com.yinyue.player.service;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private static UpdateChecker instance;
    private static final String CURRENT_VERSION = "1.0.0";
    private static final String REPO_URL = "https://api.github.com/repos/Huanglongmao66/yinping/releases/latest";

    public static UpdateChecker getInstance() {
        if (instance == null) instance = new UpdateChecker();
        return instance;
    }

    private UpdateChecker() {}

    public interface UpdateCallback {
        void onUpdateAvailable(String latestVersion, String downloadUrl, String releaseNotes);
        void onUpToDate();
        void onError(String error);
    }

    public void checkForUpdates(UpdateCallback callback) {
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL(REPO_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    String response = sb.toString();
                    String latestVersion = extractJsonField(response, "tag_name");
                    String htmlUrl = extractJsonField(response, "html_url");
                    String body = extractJsonField(response, "body");

                    if (latestVersion != null) {
                        final String ver = latestVersion.replace("v", "");
                        final String downloadUrl = htmlUrl != null ? htmlUrl : "";
                        final String notes = body != null ? body : "";
                        if (compareVersions(ver, CURRENT_VERSION) > 0) {
                            Platform.runLater(() -> callback.onUpdateAvailable(ver, downloadUrl, notes));
                        } else {
                            Platform.runLater(() -> callback.onUpToDate());
                        }
                    }
                } else {
                    final int code = conn.getResponseCode();
                    Platform.runLater(() -> callback.onError("HTTP " + code));
                }
            } catch (Exception e) {
                Platform.runLater(() -> callback.onError(e.getMessage()));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
