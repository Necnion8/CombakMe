package com.gmail.necnionch.myplugin.combakme.bukkit;

import com.gmail.necnionch.myplugin.combakme.bukkit.config.CombakMeConfig;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.LoopMessage;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.RandomMessage;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.TimeMessage;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.Database;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.MySQLDatabase;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class CombakMePlugin extends JavaPlugin implements Listener {

    public static final String DISABLE_NOTIFY_PERMISSION = "combakme.disable-notify";

    private final Random random = new Random();
    private final CombakMeConfig mainConfig = new CombakMeConfig(this);
    private final CombakScheduler scheduler = new CombakScheduler(task -> getServer().getScheduler().runTask(this, task));
    private @Nullable Database database;
    private final DiscordSRV srv = DiscordSRV.getPlugin();
    private @Nullable Permission vaultPermission;

    @Override
    public void onEnable() {
        setupVaultPermission();
        mainConfig.load();
        openDatabase();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTask(this, this::scheduleAll);  // wait for srv load
    }

    @Override
    public void onDisable() {
        scheduler.destroy();
        closeDatabase();
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
        scheduler.cancelAll();

        if (!srv.isEnabled())
            return;

        long nowTime = System.currentTimeMillis();
        for (UUID playerId : getDiscordLinkedPlayers().values()) {
            OfflinePlayer player = getServer().getOfflinePlayer(playerId);
            schedule(player, nowTime);
        }

    }

    public void schedule(OfflinePlayer player, long nowTime) {
        scheduler.cancel(player.getUniqueId());

        long lastPlayed = player.getLastPlayed();
        if (player.isOnline() || lastPlayed == 0 || hasPermission(player, DISABLE_NOTIFY_PERMISSION)) {
            return;
        }

        List<TimeMessage> messages = mainConfig.getMessages();
        LoopMessage messageLoop = mainConfig.getMessageLoop();

        // 最終ログイン経過時間よりも後の最小時間を選択する
        TimeMessage timeMessage = messages.stream()
                .filter(time -> nowTime - lastPlayed < time.getScheduleMinutes() * 60L * 60 * 1000)
                .findFirst()
                .orElse(null);

        if (timeMessage != null) {
            int scheduleMinutes = timeMessage.getScheduleMinutes();
            long delay = lastPlayed + (scheduleMinutes * 60L * 60 * 1000) - nowTime;
            List<TimeMessage> times = messages.stream().filter(m -> m.getScheduleMinutes() == scheduleMinutes).collect(Collectors.toList());
            scheduler.add(player.getUniqueId(), delay, () -> onTime(player, times));

        } else if (messageLoop != null && messageLoop.isEnable()) {
            long delay = messageLoop.getTimerMinutes() * 60L * 60 * 1000;
            delay += (long) messageLoop.getTimerMinutesRange() * 60d * 1000 * random.nextFloat();
            scheduler.add(player.getUniqueId(), delay, () -> onTime(player, messageLoop));
        }
    }

    public void sendDiscordNotify(OfflinePlayer player, RandomMessage message) {
        List<String> contents = message.getContents();
        if (!srv.isEnabled() || srv.getJda() == null || contents.isEmpty())
            return;

        String discordId = srv.getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null)
            return;

        Consumer<User> sendMessage = user -> user.openPrivateChannel().queue(channel -> {
            String content = contents.get(new Random().nextInt(contents.size()));
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

    public void onJoin(PlayerJoinEvent event) {
        scheduler.cancel(event.getPlayer().getUniqueId());
    }

    public void onQuit(PlayerQuitEvent event) {
        // 切断した即座ではなく、最初の通知時間が近づいた時にスケジュールするべき？
        schedule(event.getPlayer(), System.currentTimeMillis());
    }

    private void onTime(OfflinePlayer player, List<TimeMessage> messages) {
        long lastPlayed = player.getLastPlayed();
        long nowTime = System.currentTimeMillis();

        schedule(player, nowTime);

        for (TimeMessage message : messages) {
            int rangeMinutes = message.getScheduleMinutesRange();
            if (rangeMinutes == 0) {
                sendDiscordNotify(player, message);
                continue;
            }
            long delay = lastPlayed + (message.getScheduleMinutes() * 60L * 60 * 1000) - nowTime;
            delay += (long) rangeMinutes * 60d * 1000 * random.nextFloat();
            scheduler.add(player.getUniqueId(), delay, () -> sendDiscordNotify(player, message));  // TODO: db check
        }

    }

    private void onTime(OfflinePlayer player, LoopMessage message) {
        sendDiscordNotify(player, message);
        schedule(player, System.currentTimeMillis());
    }

}
