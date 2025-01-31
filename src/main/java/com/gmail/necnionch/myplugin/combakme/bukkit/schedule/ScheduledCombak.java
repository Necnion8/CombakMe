package com.gmail.necnionch.myplugin.combakme.bukkit.schedule;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ScheduledCombak {

    private final UUID scheduleId;
    private final UUID playerId;
    private final int configuredMinutes;
    private final @Nullable Integer configuredMinutesMax;
    private final long scheduledTime;

    public ScheduledCombak(UUID scheduleId, UUID playerId, int configuredMinutes, @Nullable Integer configuredMinutesMax, long scheduledTime) {
        this.scheduleId = scheduleId;
        this.playerId = playerId;
        this.configuredMinutes = configuredMinutes;
        this.configuredMinutesMax = configuredMinutesMax;
        this.scheduledTime = scheduledTime;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getConfiguredMinutes() {
        return configuredMinutes;
    }

    public @Nullable Integer getConfiguredMinutesMax() {
        return configuredMinutesMax;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

}
