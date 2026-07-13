package com.yinyue.player.model;

public enum PlayMode {
    SEQUENCE("顺序播放"),
    REPEAT_ONE("单曲循环"),
    REPEAT_ALL("列表循环"),
    SHUFFLE("随机播放");

    private final String displayName;

    PlayMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}