package com.gmail.necnionch.myplugin.combakme.bukkit;

import com.gmail.necnionch.myplugin.combakme.bukkit.config.CombakMeConfig;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.LoopMessage;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.RandomMessage;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.TimeMessage;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.Database;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.MySQLDatabase;
import com.gmail.necnionch.myplugin.combakme.bukkit.schedule.CombakScheduler;
import com.gmail.necnionch.myplugin.combakme.bukkit.schedule.ScheduledCombak;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class CombakMePlugin extends JavaPlugin implements Listener {

    public static final String DISABLE_NOTIFY_PERMISSION = "combakme.disable-notify";

    private final Random random = new Random();
    private final CombakMeConfig mainConfig = new CombakMeConfig(this);
    private final CombakScheduler scheduler = new CombakScheduler(this, task -> getServer().getScheduler().runTask(this, task));
    private final Consumer<Runnable> asyncExecutor = task -> getServer().getScheduler().runTaskAsynchronously(this, task);
    private @Nullable Database database;
    private final DiscordSRV srv = DiscordSRV.getPlugin();
    private @Nullable Permission vaultPermission;
    //
    private final Map<UUID, ScheduledCombak> scheduledRandoms = Maps.newHashMap();

    public static String formatEpochTime(long time) {
        return new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date(time));
    }

    @Override
    public void onEnable() {
        setupVaultPermission();
        mainConfig.load();
        openDatabase();

        if (database == null) {
            setEnabled(false);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, new Consumer<BukkitTask>() {
            private int count;

            @Override
            public void accept(BukkitTask task) {
                if (60 <= count++) {
                    task.cancel();
                    getLogger().warning("Failed to get DiscordSRV Account Link Manager");
                    return;
                }

                if (srv.getAccountLinkManager() != null) {
                    task.cancel();
                    scheduleAll();
                }
            }
        }, 0, 20);
    }

    @Override
    public void onDisable() {
        if (!scheduledRandoms.isEmpty()) {
            // add to database
            try {
                // FIXME: scheduledRandomsから消されたスケジュールをDBからも消す (通知日が過ぎるか再ログインしていれば次回のscheduleAllで消える)
                Objects.requireNonNull(database, "Database not initialized").addScheduled(scheduledRandoms.values());
            } catch (Throwable e) {
                getLogger().severe("Failed to keep schedule to database: " + e.getMessage());
            }
        }

        scheduler.destroy();
        closeDatabase();
    }

    public void d(String message) {
        if (mainConfig.isDebug())
            getLogger().warning("[DEBUG]: " + message);
    }

    public void d(Supplier<String> message) {
        if (mainConfig.isDebug())
            getLogger().warning("[DEBUG]: " + message.get());
    }

    public void openDatabase() {
        if (database != null && !database.isClosed())
            return;

        database = new MySQLDatabase(mainConfig.getMySQLConfig());
        try {
            if (database.openConnection()) {
                try {
                    database.initDatabase();
                } catch (Throwable e) {
                    e.printStackTrace();
                    database.closeConnection();
                }
            } else {
                database = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            database = null;
        }

        if (database == null) {
            getLogger().warning("Failed to connect to database");
        } else {
            getLogger().info("Database connected!");
        }
    }

    public void closeDatabase() {
        if (database != null && !database.isClosed()) {
            try {
                database.closeConnection();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            getLogger().info("Database closed!");
        }
        database = null;
    }

    public Database getDatabase() {
        if (database == null || database.isClosed())
            throw new IllegalStateException("Database is not available");
        return database;
    }

    private void setupVaultPermission() {
        vaultPermission = null;
        try {
            Class.forName("net.milkbowl.vault.permission.Permission");
        } catch (ClassNotFoundException e) {
            return;
        }
        RegisteredServiceProvider<Permission> reg = getServer().getServicesManager().getRegistration(Permission.class);
        if (reg != null) {
            vaultPermission = reg.getProvider();
        }
    }

    //

    public boolean hasPermission(OfflinePlayer player, String node) {
        if (vaultPermission != null) {
            return vaultPermission.playerHas(null, player, node);
        }
        if (player.isOnline() && player instanceof Player) {
            return ((Player) player).hasPermission(node);
        }
        return false;
    }

    public boolean hasPermission(UUID playerId, String node) {
        return hasPermission(Optional.<OfflinePlayer>ofNullable(getServer().getPlayer(playerId))
                .orElseGet(() -> getServer().getOfflinePlayer(playerId)), node);
    }

    public Map<String, UUID> getDiscordLinkedPlayers() {
        AccountLinkManager links = srv.getAccountLinkManager();
        if (links == null)
            throw new IllegalStateException("DiscordSRV AccountLinkManager is not loaded");
        return links.getLinkedAccounts();
    }

    public void scheduleAll() {
        d("on scheduleAll");
        scheduler.cancelAll();

        if (!srv.isEnabled())
            return;

        long nowTime = System.currentTimeMillis();
        d(() -> "now time: " + formatEpochTime(nowTime));
        Set<UUID> links = Sets.newHashSet(getDiscordLinkedPlayers().values());

        d(() -> "linked players -> " + links.size());
        Map<UUID, OfflinePlayer> players = links.stream().collect(Collectors.toMap(id -> id, id -> getServer().getOfflinePlayer(id)));

        asyncExecutor.accept(() -> {
            try {
                List<ScheduledCombak> scheduledList = Objects.requireNonNull(database, "Database not initialized").getScheduledAll();
                Map<String, TimeMessage> times = mainConfig.getMessages().stream().collect(Collectors.toMap(
                        tim -> tim.getScheduleMinutes() + ":" + tim.getScheduleMinutesMax(), tim -> tim));

                Set<UUID> removeSchedules = Sets.newHashSet();

                d("== database stored schedules check ==");
                for (ScheduledCombak scheduled : scheduledList) {
                    OfflinePlayer player = players.get(scheduled.getPlayerId());
                    d(() -> "- player: " + scheduled.getPlayerId());

                    // SRVでリンクされていない OR オンライン
                    if (player == null || player.isOnline()) {
                        removeSchedules.add(scheduled.getScheduleId());
                        d("   cancelled - player is null OR online now");
                        continue;
                    }

                    // 既に過ぎている OR スケジュール時のlastPlayedより最近
                    if (scheduled.getNotifySendTime() < nowTime || scheduled.getLastPlayed() < player.getLastPlayed()) {
                        removeSchedules.add(scheduled.getScheduleId());
                        d("   cancelled - scheduledTime < nowTime OR scheduledLastPlayed < player.lastPlayed");
                        continue;
                    }

                    // 使用される設定が消えている
                    String key = scheduled.getConfiguredMinutes() + ":" + scheduled.getConfiguredMinutesMax();
                    TimeMessage timeMessage = times.get(key);
                    if (timeMessage == null) {
                        removeSchedules.add(scheduled.getScheduleId());
                        d("   cancelled - removed in config");
                        continue;
                    }

                    scheduledRandoms.put(scheduled.getScheduleId(), scheduled);
                    d("   resume schedule");
                    scheduler.add(scheduled.getPlayerId(), scheduled.getNotifySendTime() - nowTime, () -> {
                        scheduledRandoms.remove(scheduled.getScheduleId());
                        sendDiscordNotify(player, timeMessage);
                        scheduleLoopMessageWhenCompleteTimeMessages(player);
                    }, () -> scheduledRandoms.remove(scheduled.getScheduleId()));
                }

                database.removeScheduledByUUID(removeSchedules);
                getServer().getScheduler().runTask(this, () -> players.values().forEach(p -> schedule(p, nowTime)));

            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Failed to scheduling from database", e);
            }
        });
    }

    public void schedule(OfflinePlayer player, long nowTime) {
        d(() -> "on schedule : " + player.getUniqueId() + " (" + player.getName() + ")");
//        scheduler.cancel(player.getUniqueId());

        long lastPlayed = player.getLastPlayed();
        d(() -> "lastPlayed -> " + formatEpochTime(lastPlayed) + " (" + Math.round((nowTime - lastPlayed) / 1000d / 60) + "m)");
        if (player.isOnline() || lastPlayed == 0 || hasPermission(player, DISABLE_NOTIFY_PERMISSION)) {
            return;
        }

        List<TimeMessage> messages = mainConfig.getMessages();
        LoopMessage messageLoop = mainConfig.getMessageLoop();

        // 最終ログイン経過時間よりも後の最小時間を選択する
        TimeMessage timeMessage = messages.stream()
                .filter(time -> nowTime - lastPlayed < time.getScheduleMinutes() * 60L * 1000)
                .findFirst()
                .orElse(null);

        if (timeMessage != null) {
            d("-> time message");
            int scheduleMinutes = timeMessage.getScheduleMinutes();
            long delay = lastPlayed + (scheduleMinutes * 60L * 1000) - nowTime;
            List<TimeMessage> times = messages.stream().filter(m -> m.getScheduleMinutes() == scheduleMinutes).collect(Collectors.toList());
            scheduler.add(player.getUniqueId(), delay, () -> onTime(player, times));

        } else if (messageLoop != null && messageLoop.isEnable()) {
            if (scheduler.isScheduledPlayer(player.getUniqueId())) {
                d("-> scheduled any match");
            } else {
                d("-> loop message");
                long delay = messageLoop.getTimerMinutes() * 60L * 1000;
                if (messageLoop.getTimerMinutesMax() != null) {
                    delay += (long) messageLoop.getTimerMinutesMax() * 60d * 1000 * random.nextFloat();
                }
                scheduler.add(player.getUniqueId(), delay, () -> onTime(player, messageLoop));
            }
        } else {
            d("-> else");
        }
    }

    public void scheduleLoopMessageWhenCompleteTimeMessages(OfflinePlayer player) {
        if (scheduler.isScheduledPlayer(player.getUniqueId()))
            return;

        LoopMessage messageLoop = mainConfig.getMessageLoop();
        if (messageLoop == null || !messageLoop.isEnable())
            return;

        d("-> loop message");

        long delay = messageLoop.getTimerMinutes() * 60L * 1000;
        if (messageLoop.getTimerMinutesMax() != null) {
            delay += (long) messageLoop.getTimerMinutesMax() * 60d * 1000 * random.nextFloat();
        }
        scheduler.add(player.getUniqueId(), delay, () -> onTime(player, messageLoop));
    }

    public void sendDiscordNotify(OfflinePlayer player, RandomMessage message) {
        d(() -> "on send notify : " + player.getUniqueId());
        List<String> contents = message.getContents();
        if (!srv.isEnabled() || srv.getJda() == null || contents.isEmpty()) {
            d("cancelled -> JDA not available or empty contents");
            return;
        }

        String discordId = srv.getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            d("cancelled -> No linked player");
            return;
        }

        Consumer<User> sendMessage = user -> user.openPrivateChannel().queue(channel -> {
            String content = contents.get(new Random().nextInt(contents.size()));
            d(() -> "queue message : " + player.getUniqueId() + " : Discord " + user.getName());
            channel.sendMessage(content).queue();
        });

        User user = srv.getJda().getUserById(discordId);
        if (user == null) {
            srv.getJda().retrieveUserById(discordId).queue(sendMessage);
        } else {
            sendMessage.accept(user);
        }

    }

    // events

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduler.cancel(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // getLastPlayed() が設定されたプレイヤーオブジェクトを使用する (イベント後に設定される？)
        getServer().getScheduler().runTask(this, () -> {
            OfflinePlayer player = getServer().getOfflinePlayer(event.getPlayer().getUniqueId());
            // 切断した即座にスケジュールを処理するのはコストが高すぎる？
            schedule(player, System.currentTimeMillis());
        });
    }

    private void onTime(OfflinePlayer player, List<TimeMessage> messages) {
        d(() -> "on time (timeMessage): " + player.getUniqueId());
        long lastPlayed = player.getLastPlayed();
        long nowTime = System.currentTimeMillis();

        for (TimeMessage message : messages) {
            Integer rangeMinutes = message.getScheduleMinutesMax();
            if (rangeMinutes == null || rangeMinutes <= message.getScheduleMinutes()) {
                sendDiscordNotify(player, message);
                continue;
            }
            long delay = lastPlayed + (message.getScheduleMinutes() * 60L * 1000) - nowTime;
            delay += (long) ((rangeMinutes - message.getScheduleMinutes()) * 60d * 1000 * random.nextFloat());

            ScheduledCombak scheduledCombak = new ScheduledCombak(UUID.randomUUID(), player.getUniqueId(), message.getScheduleMinutes(), rangeMinutes, lastPlayed, System.currentTimeMillis() + delay);
            scheduledRandoms.put(scheduledCombak.getScheduleId(), scheduledCombak);
            scheduler.add(player.getUniqueId(), delay, () -> {
                scheduledRandoms.remove(scheduledCombak.getScheduleId());
                sendDiscordNotify(player, message);
                scheduleLoopMessageWhenCompleteTimeMessages(player);
            }, () -> scheduledRandoms.remove(scheduledCombak.getScheduleId()));
        }

        schedule(player, nowTime);

    }

    private void onTime(OfflinePlayer player, LoopMessage message) {
        d(() -> "on time (loopMessage): " + player.getUniqueId());
        sendDiscordNotify(player, message);
        schedule(player, System.currentTimeMillis());
    }

}
