package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import java.util.List;

public class TimeMessage implements RandomMessage {

    private final int scheduleMinutes;
    private final int scheduleMinutesRange;
    private final List<String> contents;

    public TimeMessage(int scheduleMinutes, int scheduleMinutesRange, List<String> contents) {
        this.scheduleMinutes = scheduleMinutes;
        this.scheduleMinutesRange = scheduleMinutesRange;
        this.contents = contents;
    }

    public int getScheduleMinutes() {
        return scheduleMinutes;
    }

    public int getScheduleMinutesRange() {
        return scheduleMinutesRange;
    }

    @Override
    public List<String> getContents() {
        return contents;
    }

}
