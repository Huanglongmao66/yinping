package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 播放队列服务
 * 提供高级队列操作：优先播放、临时队列、队列历史
 */
public class PlaylistQueueService {
    private static PlaylistQueueService instance;
    
    private Queue<Song> priorityQueue; // 优先播放队列
    private Queue<Song> tempQueue; // 临时播放队列（播放后清除）
    private List<Song> queueHistory; // 已播放的队列歌曲历史
    private int maxHistorySize = 100;
    
    private PlaylistService playlistService;
    
    public static PlaylistQueueService getInstance() {
        if (instance == null) {
            instance = new PlaylistQueueService();
        }
        return instance;
    }
    
    private PlaylistQueueService() {
        priorityQueue = new ConcurrentLinkedQueue<>();
        tempQueue = new ConcurrentLinkedQueue<>();
        queueHistory = new ArrayList<>();
        playlistService = PlaylistService.getInstance();
    }
    
    // 添加到优先队列
    public void addToPriority(Song song) {
        if (song == null) return;
        priorityQueue.offer(song);
    }
    
    // 批量添加到优先队列
    public void addToPriorityBatch(List<Song> songs) {
        for (Song song : songs) {
            addToPriority(song);
        }
    }
    
    // 添加到临时队列（下一首播放）
    public void playNext(Song song) {
        if (song == null) return;
        tempQueue.offer(song);
    }
    
    // 批量添加到临时队列
    public void playNextBatch(List<Song> songs) {
        for (Song song : songs) {
            playNext(song);
        }
    }
    
    // 添加到临时队列末尾
    public void addToTempQueue(Song song) {
        if (song == null) return;
        tempQueue.offer(song);
    }
    
    // 获取下一首歌曲（优先队列 > 临时队列 > 正常播放列表）
    public Song getNextSong() {
        // 先检查优先队列
        Song song = priorityQueue.poll();
        if (song != null) {
            addToHistory(song);
            return song;
        }
        
        // 再检查临时队列
        song = tempQueue.poll();
        if (song != null) {
            addToHistory(song);
            return song;
        }
        
        // 最后使用正常播放列表
        return null; // 让PlaylistService处理
    }
    
    public boolean hasQueuedSongs() {
        return !priorityQueue.isEmpty() || !tempQueue.isEmpty();
    }
    
    public int getPriorityQueueSize() {
        return priorityQueue.size();
    }
    
    public int getTempQueueSize() {
        return tempQueue.size();
    }
    
    public List<Song> getPriorityQueueList() {
        return new ArrayList<>(priorityQueue);
    }
    
    public List<Song> getTempQueueList() {
        return new ArrayList<>(tempQueue);
    }
    
    // 清空队列
    public void clearPriorityQueue() {
        priorityQueue.clear();
    }
    
    public void clearTempQueue() {
        tempQueue.clear();
    }
    
    public void clearAllQueues() {
        priorityQueue.clear();
        tempQueue.clear();
    }
    
    // 移动队列中的歌曲
    public void moveInPriorityQueue(int fromIndex, int toIndex) {
        List<Song> list = new ArrayList<>(priorityQueue);
        if (fromIndex >= 0 && fromIndex < list.size() && 
            toIndex >= 0 && toIndex < list.size()) {
            Song song = list.remove(fromIndex);
            list.add(toIndex, song);
            priorityQueue.clear();
            priorityQueue.addAll(list);
        }
    }
    
    public void moveInTempQueue(int fromIndex, int toIndex) {
        List<Song> list = new ArrayList<>(tempQueue);
        if (fromIndex >= 0 && fromIndex < list.size() && 
            toIndex >= 0 && toIndex < list.size()) {
            Song song = list.remove(fromIndex);
            list.add(toIndex, song);
            tempQueue.clear();
            tempQueue.addAll(list);
        }
    }
    
    // 从队列中移除
    public void removeFromPriorityQueue(Song song) {
        priorityQueue.remove(song);
    }
    
    public void removeFromTempQueue(Song song) {
        tempQueue.remove(song);
    }
    
    // 历史记录
    private void addToHistory(Song song) {
        queueHistory.add(song);
        if (queueHistory.size() > maxHistorySize) {
            queueHistory.remove(0);
        }
    }
    
    public List<Song> getQueueHistory() {
        return new ArrayList<>(queueHistory);
    }
    
    public void clearQueueHistory() {
        queueHistory.clear();
    }
    
    // 重新播放历史中的歌曲
    public void replayFromHistory(int index) {
        if (index >= 0 && index < queueHistory.size()) {
            Song song = queueHistory.get(index);
            addToPriority(song);
        }
    }
    
    // 随机播放队列
    public void shufflePriorityQueue() {
        List<Song> list = new ArrayList<>(priorityQueue);
        Collections.shuffle(list);
        priorityQueue.clear();
        priorityQueue.addAll(list);
    }
    
    public void shuffleTempQueue() {
        List<Song> list = new ArrayList<>(tempQueue);
        Collections.shuffle(list);
        tempQueue.clear();
        tempQueue.addAll(list);
    }
    
    // 队列信息
    public String getQueueInfo() {
        int priority = priorityQueue.size();
        int temp = tempQueue.size();
        if (priority == 0 && temp == 0) {
            return "无队列歌曲";
        }
        return String.format("优先队列: %d 首, 临时队列: %d 首", priority, temp);
    }
    
    // 预览即将播放的歌曲
    public List<Song> getUpcomingSongs(int count) {
        List<Song> upcoming = new ArrayList<>();
        
        // 从优先队列取
        int i = 0;
        for (Song song : priorityQueue) {
            if (i >= count) break;
            upcoming.add(song);
            i++;
        }
        
        // 从临时队列取
        for (Song song : tempQueue) {
            if (i >= count) break;
            upcoming.add(song);
            i++;
        }
        
        // 如果需要更多，从正常播放列表获取
        if (i < count && playlistService.getCurrentPlaylist() != null) {
            int currentIndex = playlistService.getCurrentIndex();
            List<Song> playlistSongs = playlistService.getCurrentPlaylist().getSongs();
            for (int j = currentIndex + 1; j < playlistSongs.size() && i < count; j++) {
                upcoming.add(playlistSongs.get(j));
                i++;
            }
        }
        
        return upcoming;
    }
    
    // 保存/恢复队列状态
    public void saveQueueState(String key) {
        // 可以扩展为持久化存储
    }
    
    public void restoreQueueState(String key) {
        // 可以扩展为持久化存储
    }
}