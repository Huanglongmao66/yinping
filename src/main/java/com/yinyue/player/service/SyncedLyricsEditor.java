package com.yinyue.player.service;

import com.yinyue.player.util.LrcEditor;

import java.util.List;

public class SyncedLyricsEditor {
    private LrcEditor editor;
    private long currentTime = 0;

    public SyncedLyricsEditor() {
        editor = new LrcEditor();
    }

    public void loadFile(String path) {
        editor.load(path);
    }

    public void setCurrentTime(long timeMs) {
        this.currentTime = timeMs;
    }

    public int getCurrentLineIndex() {
        List<LrcEditor.LrcLine> lines = editor.getLines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).timeMs <= currentTime) {
                return i;
            }
        }
        return 0;
    }

    public void setLineTime(int index, long newTime) {
        List<LrcEditor.LrcLine> lines = editor.getLines();
        if (index >= 0 && index < lines.size()) {
            lines.get(index).timeMs = newTime;
        }
    }

    public void adjustAllTiming(long offsetMs) {
        for (LrcEditor.LrcLine line : editor.getLines()) {
            line.timeMs = Math.max(0, line.timeMs + offsetMs);
        }
    }

    public void save(String path) {
        editor.saveAs(path);
    }

    public List<LrcEditor.LrcLine> getLines() {
        return editor.getLines();
    }
}