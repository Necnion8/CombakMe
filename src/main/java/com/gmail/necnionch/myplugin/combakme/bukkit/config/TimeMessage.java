package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TimeMessage {

    private final int elapsedHours;
    private final List<String> contents;
    private final @Nullable SubMessage subMessage;

    public TimeMessage(int elapsedHours, List<String> contents, @Nullable SubMessage subMessage) {
        this.elapsedHours = elapsedHours;
        this.contents = contents;
        this.subMessage = subMessage;
    }

    public int getElapsedHours() {
        return elapsedHours;
    }

    public List<String> getContents() {
        return contents;
    }

    public @Nullable SubMessage getSubMessage() {
        return subMessage;
    }


    public static class SubMessage {

        private final int elapsedMinutesMin;
        private final int elapsedMinutesMax;
        private final List<String> contents;

        public SubMessage(int elapsedMinutesMin, int elapsedMinutesMax, List<String> contents) {
            this.elapsedMinutesMin = elapsedMinutesMin;
            this.elapsedMinutesMax = elapsedMinutesMax;
            this.contents = contents;
        }

        public int getElapsedMinutesMin() {
            return elapsedMinutesMin;
        }

        public int getElapsedMinutesMax() {
            return elapsedMinutesMax;
        }

        public List<String> getContents() {
            return contents;
        }

    }
}
