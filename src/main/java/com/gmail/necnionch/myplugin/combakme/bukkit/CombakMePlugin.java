package com.gmail.necnionch.myplugin.combakme.bukkit;

import com.gmail.necnionch.myplugin.combakme.bukkit.config.CombakMeConfig;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.Database;
import com.gmail.necnionch.myplugin.combakme.bukkit.database.MySQLDatabase;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class CombakMePlugin extends JavaPlugin {

    private final CombakMeConfig mainConfig = new CombakMeConfig(this);
    private @Nullable Database database;
    private final DiscordSRV srv = DiscordSRV.getPlugin();

    @Override
    public void onEnable() {
        mainConfig.load();
        openDatabase();
    }

    @Override
    public void onDisable() {
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

}
