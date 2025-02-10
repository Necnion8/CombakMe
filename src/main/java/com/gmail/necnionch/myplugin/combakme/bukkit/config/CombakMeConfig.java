package com.gmail.necnionch.myplugin.combakme.bukkit.config;

import com.gmail.necnionch.myplugin.combakme.bukkit.database.MySQLDatabase;
import com.gmail.necnionch.myplugin.combakme.common.BukkitConfigDriver;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
    private boolean debug;

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
        debug = config.getBoolean("debug", false);

        List<TimeMessage> loadMessages = Lists.newArrayList();
        Optional.ofNullable(getConfigList(config, "schedules"))
                .ifPresent(ls -> ls.stream()
                        .map(c -> new TimeMessage(
                                parseTime(c.getString("schedule-time")),
                                parseTimeMax(c.getString("schedule-time")),
                                c.getStringList("contents")))
                        .filter(c -> 0 < c.getScheduleMinutes())
                        .forEach(loadMessages::add));

        Map<String, TimeMessage> mapMessages = Maps.newHashMap();
        for (TimeMessage timeMessage : loadMessages) {
            String key = timeMessage.getScheduleMinutes() + ":" + timeMessage.getScheduleMinutesMax();
            mapMessages.merge(key, timeMessage, (m, m2) -> { m.contents().addAll(m2.contents()); return m; });
        }
        messages.addAll(mapMessages.values());
        messages.sort(Comparator.comparing(TimeMessage::getScheduleMinutes)
                .thenComparing(m -> Math.abs(Optional.ofNullable(m.getScheduleMinutesMax()).orElse(0))));

        messageLoop = new LoopMessage(
                config.getBoolean("unscheduled-message.enable", false),
                parseTime(config.getString("unscheduled-message.timer-time")),
                parseTimeMax(config.getString("unscheduled-message.timer-time")),
                config.getStringList("unscheduled-message.contents")
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

    public boolean isDebug() {
        return debug;
    }

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

    public static @Nullable Integer parseTimeMax(@Nullable String string) {
        if (string == null || string.isEmpty())
            return null;

        String[] sp = string.split(",", 3);
        if (sp.length < 2)
            return null;

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
        if (value == 0)
            return null;
        return negative ? -value : value;
    }

}
