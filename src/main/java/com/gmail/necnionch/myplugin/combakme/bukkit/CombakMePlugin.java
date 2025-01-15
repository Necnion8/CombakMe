package com.gmail.necnionch.myplugin.combakme.bukkit;

import com.gmail.necnionch.myplugin.combakme.bukkit.config.CombakMeConfig;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombakMePlugin extends JavaPlugin {

    private final CombakMeConfig mainConfig = new CombakMeConfig(this);
    private final DiscordSRV srv = DiscordSRV.getPlugin();

    @Override
    public void onEnable() {
        mainConfig.load();
    }

    @Override
    public void onDisable() {
    }

}
