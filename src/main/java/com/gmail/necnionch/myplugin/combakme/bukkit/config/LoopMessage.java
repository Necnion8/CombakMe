package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LoopMessage implements RandomMessage {

    private final boolean enable;
    private final List<String> contents;
    private final int timerMinutes;
    private final @Nullable Integer timerMinutesMax;

    public LoopMessage(boolean enable, int timerMinutes, @Nullable Integer timerMinutesMax, List<String> contents) {
        this.enable = enable;
        this.timerMinutes = timerMinutes;
        this.timerMinutesMax = timerMinutesMax;
        this.contents = contents;
    }

    public boolean isEnable() {
        return enable;
    }

    public int getTimerMinutes() {
        return timerMinutes;
    }

    public @Nullable Integer getTimerMinutesMax() {
        return timerMinutesMax;
    }

    @Override
    public List<String> getContents() {
        return contents;
    }

}
