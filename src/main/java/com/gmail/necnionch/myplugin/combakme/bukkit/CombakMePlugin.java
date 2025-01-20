package com.gmail.necnionch.myplugin.combakme.bukkit;

import com.gmail.necnionch.myplugin.combakme.bukkit.config.CombakMeConfig;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.TimeMessage;
import com.gmail.necnionch.myplugin.combakme.bukkit.config.TimeMessage2;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.Database;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.MySQLDatabase;
import github.scarsz.discordsrv.DiscordSRV;
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
        return links.getLinkedAccounts();
    }

    public void scheduleAll() {
        scheduler.cancelAll();

        long nowTime = System.currentTimeMillis();
        for (UUID playerId : getDiscordLinkedPlayers().values()) {
            OfflinePlayer player = getServer().getOfflinePlayer(playerId);
            schedule(player, nowTime);
        }

    }

    public @Nullable TimeMessage schedule(OfflinePlayer player, long nowTime) {
        scheduler.cancel(player.getUniqueId());

        long lastPlayed = player.getLastPlayed();
        if (player.isOnline() || lastPlayed == 0 || hasPermission(player, DISABLE_NOTIFY_PERMISSION)) {
            return null;
        }

        List<TimeMessage> messages = mainConfig.getMessages();
        TimeMessage2 message2 = mainConfig.getMessage2();

        // 最終ログイン経過時間よりも後の時間を選択する
        TimeMessage timeMessage = messages.stream()
                .filter(time -> nowTime - lastPlayed < time.getElapsedHours() * 60L * 60 * 1000)
                .findFirst()
                .orElse(null);

        if (timeMessage != null) {
            // set time schedule
            scheduler.add(
                    player.getUniqueId(),
                    lastPlayed + (timeMessage.getElapsedHours() * 60L * 60 * 1000) - nowTime,
                    () -> onTime(player, timeMessage)
            );

        } else if (message2 != null && message2.isEnable()) {
            // set loop send (messages empty)
            float delay = message2.getDelayMinutesMin();
            delay += (message2.getDelayMinutesMax() - message2.getDelayMinutesMin()) * random.nextFloat();
            scheduler.add(
                    player.getUniqueId(),
                    (long) delay * 60 * 1000,
                    () -> onTime(player, message2)
            );
        }

        return timeMessage;
    }

    public void sendDiscordNotify(OfflinePlayer player, TimeMessage message) {

    }

    public void sendDiscordNotify(OfflinePlayer player, TimeMessage2 message) {

    }

    public void sendDiscordNotify(OfflinePlayer player, TimeMessage.SubMessage message) {

    }

    // events

    public void onJoin(PlayerJoinEvent event) {
        scheduler.cancel(event.getPlayer().getUniqueId());
    }

    public void onQuit(PlayerQuitEvent event) {
        // 切断した即座ではなく、最初の通知時間が近づいた時にスケジュールするべき？
        schedule(event.getPlayer(), System.currentTimeMillis());
    }

    private void onTime(OfflinePlayer player, TimeMessage message) {
        sendDiscordNotify(player, message);

        long nowTime = System.currentTimeMillis();
        TimeMessage nextMessage = schedule(player, nowTime);

        TimeMessage.SubMessage sub = message.getSubMessage();
        if (sub != null && !sub.getContents().isEmpty()) {
            int min = sub.getElapsedMinutesMin();
            int max = sub.getElapsedMinutesMax();
            long minMillis = min * 60L * 1000;
            long maxMillis = max * 60L * 1000;

            int nextHours = Optional.ofNullable(nextMessage).map(TimeMessage::getElapsedHours).orElse(0);
            if (nextMessage == null && (min < 0 || max < 0)) {
                return;
            }

            long delay = player.getLastPlayed() + (nextHours * 60L * 60 * 1000) - nowTime;
            if (min < 0) {
                minMillis = delay + (min * 60L * 1000);
            }
            if (max < 0) {
                maxMillis = delay + (max * 60L * 1000);
            }

            long subDelay = (long) (minMillis + ((maxMillis - minMillis) * random.nextDouble()));
            scheduler.add(player.getUniqueId(), subDelay, () -> sendDiscordNotify(player, sub));
        }

    }

    private void onTime(OfflinePlayer player, TimeMessage2 message) {
        sendDiscordNotify(player, message);
        schedule(player, System.currentTimeMillis());
    }

}
