package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import java.util.List;

public class TimeMessage2 {

    private final boolean enable;
    private final int delayMinutesMin;
    private final int delayMinutesMax;
    private final List<String> contents;

    public TimeMessage2(boolean enable, int delayMinutesMin, int delayMinutesMax, List<String> contents) {
        this.enable = enable;
        this.delayMinutesMin = delayMinutesMin;
        this.delayMinutesMax = delayMinutesMax;
        this.contents = contents;
    }

    public boolean isEnable() {
        return enable;
    }

    public int getDelayMinutesMin() {
        return delayMinutesMin;
    }

    public int getDelayMinutesMax() {
        return delayMinutesMax;
    }

    public List<String> getContents() {
        return contents;
    }

}
