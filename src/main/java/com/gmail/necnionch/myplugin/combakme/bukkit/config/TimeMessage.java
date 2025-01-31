package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TimeMessage implements RandomMessage {

    private final int scheduleMinutes;
    private final @Nullable Integer scheduleMinutesMax;
    private final List<String> contents;

    public TimeMessage(int scheduleMinutes, @Nullable Integer scheduleMinutesMax, List<String> contents) {
        this.scheduleMinutes = scheduleMinutes;
        this.scheduleMinutesMax = scheduleMinutesMax;
        this.contents = contents;
    }

    public int getScheduleMinutes() {
        return scheduleMinutes;
    }

    public @Nullable Integer getScheduleMinutesMax() {
        return scheduleMinutesMax;
    }

    @Override
    public List<String> getContents() {
        return contents;
    }

    public List<String> contents() {
        return contents;
    }

}
