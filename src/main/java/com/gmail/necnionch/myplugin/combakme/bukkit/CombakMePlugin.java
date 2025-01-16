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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public final class CombakMePlugin extends JavaPlugin {

    public static final String DISABLE_NOTIFY_PERMISSION = "combakme.disable-notify";

    private final CombakMeConfig mainConfig = new CombakMeConfig(this);
    private final CombakScheduler scheduler = new CombakScheduler();
    private @Nullable Database database;
    private final DiscordSRV srv = DiscordSRV.getPlugin();
    private @Nullable Permission vaultPermission;

    @Override
    public void onEnable() {
        setupVaultPermission();
        mainConfig.load();
        openDatabase();
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

        List<TimeMessage> messages = mainConfig.getMessages();
        TimeMessage2 message2 = mainConfig.getMessage2();

        Map<UUID, CombakPlayer> players;
        try {
            players = getDatabase().getPlayerAll()
                    .stream()
                    .collect(Collectors.toMap(CombakPlayer::getId, p -> p));

        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().warning("Unable to lookup combak players from database");
            return;
        }

        for (UUID playerId : getDiscordLinkedPlayers().values()) {
            if (hasPermission(playerId, DISABLE_NOTIFY_PERMISSION)) {
                continue;  // TODO: test me offline
            }

            TimeMessage timeMessage = messages.isEmpty() ? null : messages.get(0);

            if (players.containsKey(playerId)) {
                OptionalInt val = players.get(playerId).getLastNotifyHours();
                if (val.isPresent()) {
                    int hours = val.getAsInt();
                    timeMessage = null;
                    for (TimeMessage timeMsg : messages) {
                        if (hours < timeMsg.getElapsedHours()) {
                            timeMessage = timeMsg;  // next time
                            break;
                        }
                    }
                }
            }

            if (timeMessage != null) {
                // set time schedule
            } else {
                // set loop send (messages empty)
            }
        }


    }

}
