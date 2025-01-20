package com.gmail.necnionch.myplugin.combakme.bukkit;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;

public class CombakScheduler {

    private final Timer timer = new Timer("CombakMe-Scheduler", true);
    private final Multimap<UUID, TimerTask> playerTasks = ArrayListMultimap.create();
    private final Consumer<Runnable> caller;

    public CombakScheduler(Consumer<Runnable> caller) {
        this.caller = caller;
    }

    public void cancelAll() {
        playerTasks.values().forEach(TimerTask::cancel);
        playerTasks.clear();
        timer.cancel();
    }

    public void destroy() {
        cancelAll();
        timer.purge();
    }


    public void add(UUID playerId, long delay, Runnable task) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                playerTasks.values().remove(this);
                caller.accept(task);
            }
        };
        playerTasks.put(playerId, timerTask);
        timer.schedule(timerTask, delay);
    }

    public void cancel(UUID playerId) {
        if (playerTasks.containsKey(playerId)) {
            playerTasks.get(playerId).forEach(TimerTask::cancel);
            playerTasks.removeAll(playerId);
        }
    }

}
