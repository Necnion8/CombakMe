package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import com.gmail.necnionch.myplugin.combakme.bukkit.database.MySQLDatabase;
import com.gmail.necnionch.myplugin.combakme.common.BukkitConfigDriver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CombakMeConfig extends BukkitConfigDriver {

    private final List<TimeMessage> messages = new ArrayList<>();
    private @Nullable TimeMessage2 message2;

    public CombakMeConfig(JavaPlugin plugin) {
        super(plugin);
    }

    private static List<ConfigurationSection> getConfigList(ConfigurationSection parent, String key) {
        /*
          https://bukkit.org/threads/getting-a-list-of-configurationsections.157524/
         */
        List<?> list = parent.getList(key);
        return list != null ? list.stream()
                .filter(obj -> obj instanceof Map)
                .map(obj -> createMemoryConfigurationFromMap((Map<?, ?>) obj))
                .collect(Collectors.toList()) : null;
    }

    private static void putMapToMemoryConfiguration(MemoryConfiguration configuration, Map<?, ?> map) {
        map.forEach((k, v) -> {
            if (v instanceof Map) {
                configuration.set((String) k, createMemoryConfigurationFromMap((Map<?, ?>) v));
            } else {
                configuration.set((String) k, v);
            }
        });
    }

    private static MemoryConfiguration createMemoryConfigurationFromMap(Map<?, ?> map) {
        MemoryConfiguration nest = new MemoryConfiguration();
        putMapToMemoryConfiguration(nest, map);
        return nest;
    }

    //

    @Override
    public boolean onLoaded(FileConfiguration config) {
        messages.clear();

        Optional.ofNullable(getConfigList(config, "messages")).ifPresent(ls -> ls.forEach(e -> {
            TimeMessage.SubMessage sub = Optional.ofNullable(e.getConfigurationSection("sub-message")).map(s -> new TimeMessage.SubMessage(
                    s.getInt("elapsed-minutes-min", 1),
                    s.getInt("elapsed-minutes-max", 5),
                    s.getStringList("contents")
            )).orElse(null);

            messages.add(new TimeMessage(
                    e.getInt("elapsed-hours", 1),
                    e.getStringList("contents"),
                    sub
            ));
        }));
        messages.sort(Comparator.comparingInt(TimeMessage::getElapsedHours));

        message2 = new TimeMessage2(
                config.getBoolean("message-2.enable", false),
                config.getInt("message-2.delay-minutes-min", 1),
                config.getInt("message-2.delay-minutes-max", 5),
                config.getStringList("message-2.contents")
        );
        return true;
    }

    public MySQLDatabase.Config getMySQLConfig() {
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "password");
        String address = config.getString("database.mysql.address", "localhost:3306");
        String database = config.getString("database.mysql.database", "combakme");

        @SuppressWarnings("DataFlowIssue")
        Map<String, Object> options = Optional.ofNullable(config.getConfigurationSection("database.mysql.options"))
                .map(c -> c.getKeys(false).stream().collect(Collectors.toMap(k -> k, c::get)))
                .orElseGet(Collections::emptyMap);

        return new MySQLDatabase.Config(address, database, username, password, options);
    }

    //

    public boolean isEnabledMessages() {
        return config.getBoolean("enable-messages", false);
    }

    public List<TimeMessage> getMessages() {
        return messages;
    }

    public @Nullable TimeMessage2 getMessage2() {
        return message2;
    }

}
