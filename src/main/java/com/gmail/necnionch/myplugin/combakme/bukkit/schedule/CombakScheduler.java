package com.gmail.necnionch.myplugin.combakme.bukkit.schedule;

import com.gmail.necnionch.myplugin.combakme.bukkit.CombakMePlugin;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;

public class CombakScheduler {

    public final CombakMePlugin plugin;
    private final Timer timer = new Timer("CombakMe-Scheduler", true);
    private final Multimap<UUID, TimerTask> playerTasks = ArrayListMultimap.create();
    private final Consumer<Runnable> caller;

    public CombakScheduler(CombakMePlugin plugin, Consumer<Runnable> caller) {
        this.plugin = plugin;
        this.caller = caller;
    }

    public void cancelAll() {
        plugin.d("cancel all");
        playerTasks.values().forEach(TimerTask::cancel);
        playerTasks.clear();
    }

    public void destroy() {
        cancelAll();
        timer.cancel();
        timer.purge();
    }

    public void add(UUID playerId, long delay, Runnable task) {
        add(playerId, delay, task, null);
    }

    public void add(UUID playerId, long delay, Runnable task, Runnable onCancel) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    plugin.d(() -> "on schedule task : " + playerId);
                    playerTasks.values().remove(this);
                    caller.accept(task);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean cancel() {
                if (onCancel != null) {
                    try {
                        onCancel.run();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                return super.cancel();
            }
        };
        plugin.d(() -> "add schedule : " + playerId + " : delay=" + CombakMePlugin.formatEpochTime(System.currentTimeMillis() + delay) + " (" + Math.round(delay / 1000d / 60) + "m)");
        playerTasks.put(playerId, timerTask);
        timer.schedule(timerTask, delay);
    }

    public void cancel(UUID playerId) {
        if (playerTasks.containsKey(playerId)) {
            plugin.d(() -> "cancel schedule : " + playerId);
            playerTasks.get(playerId).forEach(TimerTask::cancel);
            playerTasks.removeAll(playerId);
        }
    }

}
