package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import java.util.List;

public class LoopMessage {

    private final boolean enable;
    private final List<String> contents;
    private final int timerMinutes;
    private final int timerMinutesRange;

    public LoopMessage(boolean enable, int timerMinutes, int timerMinutesRange, List<String> contents) {
        this.enable = enable;
        this.timerMinutes = timerMinutes;
        this.timerMinutesRange = timerMinutesRange;
        this.contents = contents;
    }

    public boolean isEnable() {
        return enable;
    }

    public int getTimerMinutes() {
        return timerMinutes;
    }

    public int getTimerMinutesRange() {
        return timerMinutesRange;
    }

    public List<String> getContents() {
        return contents;
    }

}
