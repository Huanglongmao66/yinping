package com.yinyue.player.service;

import com.yinyue.player.model.Song;
import com.yinyue.player.util.AudioUtils;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 音频文件监控服务
 * 监控音乐目录变化，自动更新音乐库
 */
public class FileMonitorService {
    private static FileMonitorService instance;
    
    private WatchService watchService;
    private Map<Path, WatchKey> watchedDirectories;
    private ExecutorService executor;
    private boolean isRunning;
    private List<FileChangeListener> listeners;
    
    private LibraryService libraryService;
    
    public static FileMonitorService getInstance() {
        if (instance == null) {
            instance = new FileMonitorService();
        }
        return instance;
    }
    
    private FileMonitorService() {
        watchedDirectories = new HashMap<>();
        listeners = new ArrayList<>();
        libraryService = LibraryService.getInstance();
    }
    
    // 启动监控服务
    public void startMonitoring() {
        if (isRunning) return;
        
        try {
            watchService = FileSystems.getDefault().newWatchService();
            executor = Executors.newSingleThreadExecutor();
            isRunning = true;
            
            executor.submit(() -> {
                while (isRunning) {
                    try {
                        WatchKey key = watchService.take();
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changedFile = ((Path) key.watchable()).resolve((Path) event.context());
                            
                            handleFileEvent(changedFile, event.kind());
                        }
                        
                        key.reset();
                    } catch (InterruptedException e) {
                        break;
                    } catch (ClosedWatchServiceException e) {
                        break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 停止监控服务
    public void stopMonitoring() {
        isRunning = false;
        
        if (executor != null) {
            executor.shutdown();
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        watchedDirectories.clear();
    }
    
    // 添加监控目录
    public void addWatchDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!path.toFile().isDirectory()) return;
            
            WatchKey key = path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            
            watchedDirectories.put(path, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 移除监控目录
    public void removeWatchDirectory(String directoryPath) {
        Path path = Paths.get(directoryPath);
        WatchKey key = watchedDirectories.remove(path);
        if (key != null) {
            key.cancel();
        }
    }
    
    // 处理文件事件
    private void handleFileEvent(Path file, WatchEvent.Kind<?> kind) {
        String fileName = file.getFileName().toString();
        
        // 只处理音频文件
        if (!AudioUtils.isSupportedAudioFormat(fileName)) {
            return;
        }
        
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            // 新文件创建
            Song song = createSongFromFile(file.toFile());
            if (song != null) {
                libraryService.addSong(song);
                notifyListeners(FileChangeEvent.ADD, song);
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            // 文件删除
            String filePath = file.toFile().getAbsolutePath();
            // 从音乐库中移除对应路径的歌曲
            for (Song s : libraryService.getSongs()) {
                if (s.getFilePath() != null && s.getFilePath().equals(filePath)) {
                    libraryService.removeSong(s);
                    notifyListeners(FileChangeEvent.REMOVE, s);
                    break;
                }
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            // 文件修改
            String filePath = file.toFile().getAbsolutePath();
            for (Song s : libraryService.getSongs()) {
                if (s.getFilePath() != null && s.getFilePath().equals(filePath)) {
                    // 更新文件信息
                    s.setFileSize(file.toFile().length());
                    notifyListeners(FileChangeEvent.UPDATE, s);
                    break;
                }
            }
        }
    }
    
    // 从文件创建歌曲对象
    private Song createSongFromFile(File file) {
        if (!file.exists() || !file.isFile()) return null;
        
        Song song = new Song();
        song.setId(UUID.randomUUID().toString());
        song.setFilePath(file.getAbsolutePath());
        song.setFileSize(file.length());
        song.setFormat(AudioUtils.getFileExtension(file.getName()));
        song.setTitle(file.getName().replaceAll("\\.[^.]+$", ""));
        
        return song;
    }
    
    // 添加监听器
    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }
    
    // 移除监听器
    public void removeListener(FileChangeListener listener) {
        listeners.remove(listener);
    }
    
    // 通知监听器
    private void notifyListeners(FileChangeEvent event, Song song) {
        for (FileChangeListener listener : listeners) {
            listener.onFileChange(event, song);
        }
    }
    
    // 获取监控状态
    public boolean isRunning() {
        return isRunning;
    }
    
    public List<String> getWatchedDirectories() {
        List<String> dirs = new ArrayList<>();
        for (Path path : watchedDirectories.keySet()) {
            dirs.add(path.toString());
        }
        return dirs;
    }
    
    // 手动扫描目录更新
    public void scanDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isFile() && AudioUtils.isSupportedAudioFormat(file.getName())) {
                String filePath = file.getAbsolutePath();
                Song existing = null;
                for (Song s : libraryService.getSongs()) {
                    if (s.getFilePath() != null && s.getFilePath().equals(filePath)) {
                        existing = s;
                        break;
                    }
                }
                if (existing == null) {
                    Song song = createSongFromFile(file);
                    if (song != null) {
                        libraryService.addSong(song);
                        notifyListeners(FileChangeEvent.ADD, song);
                    }
                }
            }
        }
    }
    
    // 文件变更事件类型
    public enum FileChangeEvent {
        ADD, REMOVE, UPDATE
    }
    
    // 文件变更监听器接口
    public interface FileChangeListener {
        void onFileChange(FileChangeEvent event, Song song);
    }
}