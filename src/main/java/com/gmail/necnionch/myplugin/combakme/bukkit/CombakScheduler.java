package com.gmail.necnionch.myplugin.combakme.bukkit;

import java.util.*;

public class CombakScheduler {

    private final Timer timer = new Timer("CombakMe-Scheduler", true);
    private final Map<UUID, TimerTask> playerTasks = Collections.synchronizedMap(new HashMap<>());

    public CombakScheduler() {
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
        cancel(playerId);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                playerTasks.values().remove(this);
                task.run();
            }
        };
        playerTasks.put(playerId, timerTask);
        timer.schedule(timerTask, delay);
    }

    public void cancel(UUID playerId) {
        if (playerTasks.containsKey(playerId)) {
            playerTasks.remove(playerId).cancel();
        }
    }

}
