package com.yinyue.player.service;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.yinyue.player.model.Song;
import com.yinyue.player.util.AudioUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RemoteControlServer {
    private static RemoteControlServer instance;
    private HttpServer server;
    private int port = 8765;
    private boolean running = false;

    public static RemoteControlServer getInstance() {
        if (instance == null) instance = new RemoteControlServer();
        return instance;
    }

    private RemoteControlServer() {}

    public void start() throws IOException {
        if (running) return;
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/play", new PlayHandler());
        server.createContext("/api/pause", new PauseHandler());
        server.createContext("/api/next", new NextHandler());
        server.createContext("/api/previous", new PreviousHandler());
        server.createContext("/api/volume", new VolumeHandler());
        server.createContext("/api/playlist", new PlaylistHandler());
        server.createContext("/api/library", new LibraryHandler());
        server.createContext("/", new StaticHandler());

        server.setExecutor(null);
        server.start();
        running = true;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    private void sendResponse(HttpExchange exchange, String response, int code) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    class StatusHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            AudioPlayerService ps = AudioPlayerService.getInstance();
            Song song = ps.getCurrentSong();
            String json = String.format(
                "{\"playing\":%b,\"volume\":%.2f,\"mute\":%b,\"currentTime\":%d,\"duration\":%d,\"song\":%s}",
                ps.isPlaying(), ps.getVolume(), ps.isMute(), ps.getCurrentTime(), ps.getDuration(),
                song != null ? "\"" + escapeJson(song.getDisplayTitle()) + "\"" : "null"
            );
            sendResponse(exchange, json, 200);
        }
    }

    class PlayHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            AudioPlayerService.getInstance().resume();
            sendResponse(exchange, "{\"success\":true}", 200);
        }
    }

    class PauseHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            AudioPlayerService.getInstance().pause();
            sendResponse(exchange, "{\"success\":true}", 200);
        }
    }

    class NextHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            PlaylistService.getInstance().playNext();
            sendResponse(exchange, "{\"success\":true}", 200);
        }
    }

    class PreviousHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            PlaylistService.getInstance().playPrevious();
            sendResponse(exchange, "{\"success\":true}", 200);
        }
    }

    class VolumeHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("v=")) {
                try {
                    double vol = Double.parseDouble(query.substring(2)) / 100.0;
                    AudioPlayerService.getInstance().setVolume(vol);
                } catch (Exception ignored) {}
            }
            sendResponse(exchange, "{\"success\":true}", 200);
        }
    }

    class PlaylistHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            List<Song> songs = PlaylistService.getInstance().getCurrentPlaylist().getSongs();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < songs.size(); i++) {
                Song s = songs.get(i);
                sb.append(String.format("{\"title\":\"%s\",\"artist\":\"%s\"}",
                    escapeJson(s.getDisplayTitle()), escapeJson(s.getArtist() != null ? s.getArtist() : "")));
                if (i < songs.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendResponse(exchange, sb.toString(), 200);
        }
    }

    class LibraryHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            List<Song> songs = LibraryService.getInstance().getSongs();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < Math.min(songs.size(), 100); i++) {
                Song s = songs.get(i);
                sb.append(String.format("{\"title\":\"%s\",\"artist\":\"%s\"}",
                    escapeJson(s.getDisplayTitle()), escapeJson(s.getArtist() != null ? s.getArtist() : "")));
                if (i < Math.min(songs.size(), 100) - 1) sb.append(",");
            }
            sb.append("]");
            sendResponse(exchange, sb.toString(), 200);
        }
    }

    class StaticHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>音悦远程控制</title>"
                + "<style>body{font-family:sans-serif;background:#1a1a2e;color:#fff;text-align:center;padding:20px}"
                + "button{background:#e94560;color:#fff;border:none;padding:15px 30px;margin:10px;font-size:16px;border-radius:8px;cursor:pointer}"
                + "button:hover{background:#ff6b6b}#status{margin:20px;font-size:18px}</style></head>"
                + "<body><h1>音悦播放器远程控制</h1><div id='status'>加载中...</div>"
                + "<button onclick='send(\"play\")'>播放</button>"
                + "<button onclick='send(\"pause\")'>暂停</button>"
                + "<button onclick='send(\"previous\")'>上一曲</button>"
                + "<button onclick='send(\"next\")'>下一曲</button><br>"
                + "<input type='range' id='vol' min='0' max='100' value='70' onchange='setVolume(this.value)'>"
                + "<script>async function send(cmd){await fetch('/api/'+cmd);update();}"
                + "async function setVolume(v){await fetch('/api/volume?v='+v);}"
                + "async function update(){let r=await fetch('/api/status');let d=await r.json();"
                + "document.getElementById('status').innerText=(d.playing?'▶ ':'⏸ ')+(d.song||'未播放');}"
                + "setInterval(update,2000);update();</script></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
