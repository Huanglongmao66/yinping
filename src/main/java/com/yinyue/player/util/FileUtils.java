package com.yinyue.player.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    private static final String APP_NAME = "YinYuePlayer";

    public static String getConfigDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        
        if (os.contains("win")) {
            return System.getenv("APPDATA") + File.separator + APP_NAME;
        } else if (os.contains("mac")) {
            return home + "/Library/Application Support/" + APP_NAME;
        } else {
            return home + "/.config/" + APP_NAME;
        }
    }

    public static String getPlaylistsDirectory() {
        return getConfigDirectory() + File.separator + "playlists";
    }

    public static String getLibraryFilePath() {
        return getConfigDirectory() + File.separator + "library.json";
    }

    public static String getConfigFilePath() {
        return getConfigDirectory() + File.separator + "config.json";
    }

    public static String getCacheDirectory() {
        return getConfigDirectory() + File.separator + "cache";
    }

    public static void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void ensureAllDirectories() {
        ensureDirectoryExists(getConfigDirectory());
        ensureDirectoryExists(getPlaylistsDirectory());
        ensureDirectoryExists(getCacheDirectory());
    }

    public static boolean fileExists(String path) {
        return path != null && new File(path).exists();
    }

    public static boolean deleteFile(String path) {
        if (path == null) {
            return false;
        }
        return new File(path).delete();
    }

    public static long getFileSize(String path) {
        if (path == null) {
            return 0;
        }
        File file = new File(path);
        return file.exists() ? file.length() : 0;
    }

    public static String getFileName(String path) {
        if (path == null) {
            return "";
        }
        int lastSeparator = path.lastIndexOf(File.separator);
        if (lastSeparator >= 0) {
            return path.substring(lastSeparator + 1);
        }
        return path;
    }

    public static String getFileBaseName(String path) {
        String fileName = getFileName(path);
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    public static String getParentDirectory(String path) {
        if (path == null) {
            return "";
        }
        int lastSeparator = path.lastIndexOf(File.separator);
        if (lastSeparator >= 0) {
            return path.substring(0, lastSeparator);
        }
        return "";
    }

    public static boolean isDirectory(String path) {
        if (path == null) {
            return false;
        }
        return new File(path).isDirectory();
    }

    public static void openFileLocation(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            Runtime.getRuntime().exec("explorer /select," + filePath);
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec(new String[]{"open", "-R", filePath});
        } else {
            Runtime.getRuntime().exec(new String[]{"/usr/bin/xdg-open", getParentDirectory(filePath)});
        }
    }

    public static String getUniqueFileName(String directory, String baseName, String extension) {
        Path dirPath = Paths.get(directory);
        Path filePath = dirPath.resolve(baseName + "." + extension);
        
        if (!Files.exists(filePath)) {
            return filePath.toString();
        }
        
        int counter = 1;
        while (true) {
            filePath = dirPath.resolve(baseName + "-" + counter + "." + extension);
            if (!Files.exists(filePath)) {
                return filePath.toString();
            }
            counter++;
        }
    }
}