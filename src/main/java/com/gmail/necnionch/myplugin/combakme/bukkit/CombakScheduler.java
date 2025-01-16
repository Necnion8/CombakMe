package com.gmail.necnionch.myplugin.combakme.bukkit;

import java.util.Timer;

public class CombakScheduler {

    private final Timer timer = new Timer("CombakMe-Scheduler", true);

    public CombakScheduler() {
    }

    public void cancelAll() {
        timer.cancel();
    }

    public void destroy() {
        timer.cancel();
        timer.purge();
    }

}
