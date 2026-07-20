package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.model.Playlist;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

/**
 * 智能播放列表生成服务
 * 根据条件自动生成播放列表
 */
public class SmartPlaylistService {
    private static SmartPlaylistService instance;
    
    private LibraryService libraryService;
    private MusicRatingService ratingService;
    private HistoryService historyService;
    private Map<String, SmartRule> rules;
    private Gson gson;
    private String dataFile;
    
    public static SmartPlaylistService getInstance() {
        if (instance == null) {
            instance = new SmartPlaylistService();
        }
        return instance;
    }
    
    private SmartPlaylistService() {
        libraryService = LibraryService.getInstance();
        ratingService = MusicRatingService.getInstance();
        historyService = HistoryService.getInstance();
        rules = new HashMap<>();
        gson = new Gson();
        dataFile = System.getProperty("user.home") + "/.yinyue/smart_playlists.json";
        loadFromFile();
    }
    
    // 创建智能播放列表规则
    public void createRule(String ruleName, SmartRule rule) {
        rules.put(ruleName, rule);
        saveToFile();
    }
    
    // 删除规则
    public void deleteRule(String ruleName) {
        rules.remove(ruleName);
        saveToFile();
    }
    
    // 获取规则
    public SmartRule getRule(String ruleName) {
        return rules.get(ruleName);
    }
    
    // 获取所有规则名称
    public List<String> getRuleNames() {
        return new ArrayList<>(rules.keySet());
    }
    
    // 根据规则生成播放列表
    public List<Song> generatePlaylist(String ruleName) {
        SmartRule rule = rules.get(ruleName);
        if (rule == null) return new ArrayList<>();
        
        List<Song> allSongs = libraryService.getSongs();
        List<Song> result = new ArrayList<>();
        
        for (Song song : allSongs) {
            if (matchesRule(song, rule)) {
                result.add(song);
            }
        }
        
        // 排序
        if (rule.sortBy != null && !rule.sortBy.isEmpty()) {
            sortResult(result, rule.sortBy, rule.ascending);
        }
        
        // 限制数量
        if (rule.maxCount > 0 && result.size() > rule.maxCount) {
            result = result.subList(0, rule.maxCount);
        }
        
        return result;
    }
    
    // 检查歌曲是否匹配规则
    private boolean matchesRule(Song song, SmartRule rule) {
        if (rule.conditions.isEmpty()) return true;
        
        boolean result = true;
        for (Condition condition : rule.conditions) {
            boolean match = evaluateCondition(song, condition);
            if (condition.operator.equals("AND")) {
                result = result && match;
            } else {
                result = result || match;
            }
        }
        
        return result;
    }
    
    // 评估单个条件
    private boolean evaluateCondition(Song song, Condition condition) {
        String field = condition.field;
        String op = condition.operator;
        String value = condition.value;
        
        switch (field) {
            case "title":
                return matchString(song.getTitle(), op, value);
            case "artist":
                return matchString(song.getArtist(), op, value);
            case "album":
                return matchString(song.getAlbum(), op, value);
            case "format":
                return matchString(song.getFormat(), op, value);
            case "duration":
                return matchNumber(song.getDuration(), op, parseNumber(value));
            case "fileSize":
                return matchNumber(song.getFileSize(), op, parseNumber(value));
            case "rating":
                return matchNumber(ratingService.getRating(song.getId()), op, parseNumber(value));
            case "playCount":
                return matchNumber(ratingService.getPlayCount(song.getId()), op, parseNumber(value));
            case "lastPlayed":
                return matchNumber(ratingService.getLastPlayTime(song.getId()), op, parseNumber(value));
            default:
                return true;
        }
    }
    
    private boolean matchString(String field, String op, String value) {
        if (field == null) field = "";
        if (value == null) value = "";
        
        field = field.toLowerCase();
        value = value.toLowerCase();
        
        switch (op) {
            case "contains": return field.contains(value);
            case "equals": return field.equals(value);
            case "startsWith": return field.startsWith(value);
            case "endsWith": return field.endsWith(value);
            case "notContains": return !field.contains(value);
            case "notEquals": return !field.equals(value);
            default: return true;
        }
    }
    
    private boolean matchNumber(long field, String op, long value) {
        switch (op) {
            case ">": return field > value;
            case ">=": return field >= value;
            case "<": return field < value;
            case "<=": return field <= value;
            case "=": return field == value;
            case "!=": return field != value;
            default: return true;
        }
    }
    
    private long parseNumber(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    // 排序结果
    private void sortResult(List<Song> songs, String sortBy, boolean ascending) {
        switch (sortBy) {
            case "title":
                songs.sort((a, b) -> {
                    String ta = a.getTitle() != null ? a.getTitle() : "";
                    String tb = b.getTitle() != null ? b.getTitle() : "";
                    return ascending ? ta.compareToIgnoreCase(tb) : tb.compareToIgnoreCase(ta);
                });
                break;
            case "artist":
                songs.sort((a, b) -> {
                    String aa = a.getArtist() != null ? a.getArtist() : "";
                    String ab = b.getArtist() != null ? b.getArtist() : "";
                    return ascending ? aa.compareToIgnoreCase(ab) : ab.compareToIgnoreCase(aa);
                });
                break;
            case "rating":
                songs.sort((a, b) -> {
                    int ra = ratingService.getRating(a.getId());
                    int rb = ratingService.getRating(b.getId());
                    return ascending ? Integer.compare(ra, rb) : Integer.compare(rb, ra);
                });
                break;
            case "playCount":
                songs.sort((a, b) -> {
                    int ca = ratingService.getPlayCount(a.getId());
                    int cb = ratingService.getPlayCount(b.getId());
                    return ascending ? Integer.compare(ca, cb) : Integer.compare(cb, ca);
                });
                break;
            case "random":
                Collections.shuffle(songs);
                break;
        }
    }
    
    // 预设规则
    public void createPresetRules() {
        // 最近播放
        SmartRule recent = new SmartRule();
        recent.conditions.add(new Condition("lastPlayed", ">", String.valueOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)));
        recent.sortBy = "lastPlayed";
        recent.ascending = false;
        recent.maxCount = 50;
        createRule("最近播放", recent);
        
        // 高评分
        SmartRule topRated = new SmartRule();
        topRated.conditions.add(new Condition("rating", ">=", "4"));
        topRated.sortBy = "rating";
        topRated.ascending = false;
        topRated.maxCount = 100;
        createRule("高评分歌曲", topRated);
        
        // 常播放
        SmartRule mostPlayed = new SmartRule();
        mostPlayed.conditions.add(new Condition("playCount", ">=", "10"));
        mostPlayed.sortBy = "playCount";
        mostPlayed.ascending = false;
        mostPlayed.maxCount = 100;
        createRule("常播放歌曲", mostPlayed);
        
        // 从未播放
        SmartRule neverPlayed = new SmartRule();
        neverPlayed.conditions.add(new Condition("playCount", "=", "0"));
        neverPlayed.sortBy = "random";
        neverPlayed.maxCount = 50;
        createRule("从未播放", neverPlayed);
        
        // 长歌曲
        SmartRule longSongs = new SmartRule();
        longSongs.conditions.add(new Condition("duration", ">=", String.valueOf(5 * 60 * 1000)));
        longSongs.sortBy = "title";
        longSongs.maxCount = 50;
        createRule("长歌曲", longSongs);
    }
    
    // 智能规则类
    public static class SmartRule {
        public List<Condition> conditions;
        public String sortBy;
        public boolean ascending;
        public int maxCount;
        
        public SmartRule() {
            conditions = new ArrayList<>();
            ascending = true;
            maxCount = 0;
        }
    }
    
    // 条件类
    public static class Condition {
        public String field;
        public String operator;
        public String value;
        
        public Condition() {}
        
        public Condition(String field, String operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
    }
    
    private void saveToFile() {
        try {
            File file = new File(dataFile);
            file.getParentFile().mkdirs();
            String json = gson.toJson(rules);
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadFromFile() {
        try {
            File file = new File(dataFile);
            if (!file.exists()) {
                createPresetRules();
                return;
            }
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            Map<String, SmartRule> loaded = gson.fromJson(json, new TypeToken<Map<String, SmartRule>>(){}.getType());
            if (loaded != null) {
                rules.putAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
            createPresetRules();
        }
    }
    
    private static com.google.gson.reflect.TypeToken TypeToken = new com.google.gson.reflect.TypeToken() {};
}