package com.gmail.necnionch.myplugin.combakme.bukkit;

import java.util.OptionalInt;
import java.util.UUID;

public class CombakPlayer {

    private final UUID playerId;
    private int lastNotifyHours = -1;

    public CombakPlayer(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getId() {
        return this.playerId;
    }

    public OptionalInt getLastNotifyHours() {
        if (lastNotifyHours <= -1)
            return OptionalInt.empty();
        return OptionalInt.of(lastNotifyHours);
    }

    public void setLastNotifyHours(int hours) {
        this.lastNotifyHours = hours;
    }

    public void clearLastNotifyHours() {
        this.lastNotifyHours = -1;
    }

}
