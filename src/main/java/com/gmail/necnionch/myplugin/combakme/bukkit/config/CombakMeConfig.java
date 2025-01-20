package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import com.gmail.necnionch.myplugin.combakme.bukkit.database.MySQLDatabase;
import com.gmail.necnionch.myplugin.combakme.common.BukkitConfigDriver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CombakMeConfig extends BukkitConfigDriver {

    private final List<TimeMessage> messages = new ArrayList<>();
    private @Nullable LoopMessage messageLoop;

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

        Optional.ofNullable(getConfigList(config, "messages"))
                .ifPresent(ls -> ls.stream()
                        .map(c -> new TimeMessage(
                                parseTime(c.getString("schedule-time")),
                                parseTimeRange(c.getString("schedule-time")),
                                c.getStringList("contents")))
                        .filter(c -> 0 < c.getScheduleMinutes())
                        .forEach(messages::add));
        messages.sort(Comparator.comparing(TimeMessage::getScheduleMinutes).thenComparing(m -> Math.abs(m.getScheduleMinutesRange())));

        messageLoop = new LoopMessage(
                config.getBoolean("message-2.enable", false),
                parseTime(config.getString("message-2.timer-time")),
                parseTimeRange(config.getString("message-2.timer-time")),
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

    public @Nullable LoopMessage getMessageLoop() {
        return messageLoop;
    }

    //

    private static final Pattern TIME_UNIT_REX = Pattern.compile("(\\d+)([hm])");

    public static int parseTime(@Nullable String string) {
        if (string == null || string.isEmpty())
            return 0;

        String[] sp = string.split(",", 2);

        try {
            return Math.max(0, Integer.parseInt(sp[0]));
        } catch (NumberFormatException ignored) {
        }

        Matcher m = TIME_UNIT_REX.matcher(sp[0]);
        int value = 0;
        while (m.find()) {
            if (m.group(2).equalsIgnoreCase("h")) {
                value += Integer.parseInt(m.group(1)) * 60;
            } else {
                value += Integer.parseInt(m.group(1));
            }
        }
        return value;
    }

    public static int parseTimeRange(@Nullable String string) {
        if (string == null || string.isEmpty())
            return 0;

        String[] sp = string.split(",", 3);
        if (sp.length < 3)
            return 0;

        try {
            return Math.max(0, Integer.parseInt(sp[1]));
        } catch (NumberFormatException ignored) {
        }

        boolean negative = sp[1].startsWith("-");
        Matcher m = TIME_UNIT_REX.matcher(negative ? sp[1].substring(1) : sp[1]);
        int value = 0;
        while (m.find()) {
            if (m.group(2).equalsIgnoreCase("h")) {
                value += Integer.parseInt(m.group(1)) * 60;
            } else {
                value += Integer.parseInt(m.group(1));
            }
        }
        return negative ? -value : value;
    }

}
