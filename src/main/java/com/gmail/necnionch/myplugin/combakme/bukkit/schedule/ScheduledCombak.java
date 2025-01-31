package com.gmail.necnionch.myplugin.combakme.bukkit.schedule;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ScheduledCombak {

    private final UUID scheduleId;
    private final UUID playerId;
    private final int configuredMinutes;
    private final @Nullable Integer configuredMinutesMax;
    private final long lastPlayed;
    private final long notifySendTime;

    public ScheduledCombak(UUID scheduleId, UUID playerId, int configuredMinutes, @Nullable Integer configuredMinutesMax, long lastPlayed, long notifySendTime) {
        this.scheduleId = scheduleId;
        this.playerId = playerId;
        this.configuredMinutes = configuredMinutes;
        this.configuredMinutesMax = configuredMinutesMax;
        this.lastPlayed = lastPlayed;
        this.notifySendTime = notifySendTime;
    }

    /**
     * スケジュールID
     */
    public UUID getScheduleId() {
        return scheduleId;
    }

    /**
     * プレイヤーID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 通知する設定されたオフライン時間
     */
    public int getConfiguredMinutes() {
        return configuredMinutes;
    }

    /**
     * 通知する設定されたオフライン時間 (最長)
     */
    public @Nullable Integer getConfiguredMinutesMax() {
        return configuredMinutesMax;
    }

    /**
     * スケジュール時の最終ログインのエポック時間
     */
    public long getLastPlayed() {
        return lastPlayed;
    }

    /**
     * 送信するエポック時間
     */
    public long getNotifySendTime() {
        return notifySendTime;
    }

}
