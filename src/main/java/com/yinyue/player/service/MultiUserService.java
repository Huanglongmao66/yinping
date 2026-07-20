package com.yinyue.player.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

/**
 * 多用户支持服务
 * 管理多个用户配置文件
 */
public class MultiUserService {
    private static MultiUserService instance;
    
    private Map<String, UserProfile> users;
    private String currentUser;
    private Gson gson;
    private String usersDir;
    
    public static MultiUserService getInstance() {
        if (instance == null) {
            instance = new MultiUserService();
        }
        return instance;
    }
    
    private MultiUserService() {
        users = new HashMap<>();
        gson = new Gson();
        usersDir = System.getProperty("user.home") + "/.yinyue/users";
        new File(usersDir).mkdirs();
        loadUsers();
    }
    
    // 创建用户
    public boolean createUser(String username, String displayName) {
        if (username == null || username.isEmpty() || users.containsKey(username)) {
            return false;
        }
        
        UserProfile profile = new UserProfile();
        profile.username = username;
        profile.displayName = displayName != null ? displayName : username;
        profile.createTime = System.currentTimeMillis();
        
        users.put(username, profile);
        saveUsers();
        
        // 创建用户数据目录
        new File(usersDir, username).mkdirs();
        
        return true;
    }
    
    // 删除用户
    public boolean deleteUser(String username) {
        if ("default".equals(username) || !users.containsKey(username)) {
            return false;
        }
        
        users.remove(username);
        saveUsers();
        
        // 删除用户数据目录
        File userDir = new File(usersDir, username);
        deleteDirectory(userDir);
        
        return true;
    }
    
    // 切换用户
    public boolean switchUser(String username) {
        if (!users.containsKey(username)) return false;
        
        // 保存当前用户数据
        if (currentUser != null) {
            saveUserData(currentUser);
        }
        
        currentUser = username;
        
        // 加载新用户数据
        loadUserData(username);
        
        return true;
    }
    
    // 获取当前用户
    public UserProfile getCurrentUser() {
        return users.get(currentUser);
    }
    
    public String getCurrentUsername() {
        return currentUser;
    }
    
    // 获取所有用户
    public List<UserProfile> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    // 获取用户
    public UserProfile getUser(String username) {
        return users.get(username);
    }
    
    // 更新用户显示名称
    public void updateDisplayName(String username, String displayName) {
        UserProfile profile = users.get(username);
        if (profile != null) {
            profile.displayName = displayName;
            saveUsers();
        }
    }
    
    // 更新用户设置
    public void updateSettings(String username, Map<String, Object> settings) {
        UserProfile profile = users.get(username);
        if (profile != null) {
            profile.settings.putAll(settings);
            saveUsers();
        }
    }
    
    // 获取用户设置
    public Map<String, Object> getSettings(String username) {
        UserProfile profile = users.get(username);
        if (profile != null) {
            return new HashMap<>(profile.settings);
        }
        return new HashMap<>();
    }
    
    // 保存用户数据
    private void saveUserData(String username) {
        // 保存各个服务的数据到用户目录
        String userDir = usersDir + "/" + username;
        new File(userDir).mkdirs();
    }
    
    // 加载用户数据
    private void loadUserData(String username) {
        // 从用户目录加载各个服务的数据
    }
    
    // 导出用户数据
    public boolean exportUserData(String username, String exportPath) {
        try {
            UserProfile profile = users.get(username);
            if (profile == null) return false;
            
            Map<String, Object> data = new HashMap<>();
            data.put("profile", profile);
            
            String json = gson.toJson(data);
            java.nio.file.Files.write(java.nio.file.Paths.get(exportPath), json.getBytes());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 导入用户数据
    public boolean importUserData(String importPath) {
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(importPath)));
            Map<String, Object> data = gson.fromJson(json, Map.class);
            
            if (data.containsKey("profile")) {
                UserProfile profile = gson.fromJson(gson.toJson(data.get("profile")), UserProfile.class);
                if (profile != null && profile.username != null) {
                    users.put(profile.username, profile);
                    saveUsers();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 用户是否存在
    public boolean userExists(String username) {
        return users.containsKey(username);
    }
    
    // 获取用户数量
    public int getUserCount() {
        return users.size();
    }
    
    private void loadUsers() {
        try {
            File file = new File(usersDir, "users.json");
            if (!file.exists()) {
                // 创建默认用户
                createUser("default", "默认用户");
                currentUser = "default";
                return;
            }
            
            String json = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            Map<String, UserProfile> loaded = gson.fromJson(json, new TypeToken<Map<String, UserProfile>>(){}.getType());
            if (loaded != null) {
                users.putAll(loaded);
            }
            
            if (users.isEmpty()) {
                createUser("default", "默认用户");
            }
            
            currentUser = "default";
        } catch (Exception e) {
            e.printStackTrace();
            createUser("default", "默认用户");
            currentUser = "default";
        }
    }
    
    private void saveUsers() {
        try {
            File file = new File(usersDir, "users.json");
            file.getParentFile().mkdirs();
            String json = gson.toJson(users);
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    // 用户配置文件类
    public static class UserProfile {
        public String username;
        public String displayName;
        public long createTime;
        public long lastLoginTime;
        public Map<String, Object> settings;
        
        public UserProfile() {
            settings = new HashMap<>();
        }
        
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public long getCreateTime() { return createTime; }
        public long getLastLoginTime() { return lastLoginTime; }
    }
}