package com.gmail.necnionch.myplugin.combakme.bukkit.database;

import com.gmail.necnionch.myplugin.combakme.bukkit.schedule.ScheduledCombak;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SQLiteDatabase implements CombakDatabase {

    private final Config config;
    private final File dbDirectory;
    private @Nullable HikariDataSource hikari;

    public static class Config {

        private final String filename;
        private final Map<String, Object> options;

        public Config(String filename, Map<String, Object> options) {
            this.filename = filename;
            this.options = options;
        }

        public String getFilename() {
            return filename;
        }

        public Map<String, Object> options() {
            return options;
        }
    }

    public SQLiteDatabase(File dbDirectory, Config config) {
        this.config = config;
        this.dbDirectory = dbDirectory;
    }

    public Config getConfig() {
        return config;
    }

    public File getParentDir() {
        return dbDirectory;
    }

    @Override
    public boolean openConnection() {
        if (!isClosed())
            throw new IllegalStateException("Already connection available");

        File dbFile = new File(dbDirectory, config.filename);
        File dbParent = dbFile.getParentFile();
        if (!dbParent.exists() && !dbParent.mkdirs()) {
            throw new RuntimeException("Failed to create database parent directory: " + dbParent);
        }

        String url = "jdbc:sqlite:" + dbFile.toURI();
        HikariConfig dbConf = new HikariConfig();
        dbConf.setPoolName("CombakMe-HikariPool");
        dbConf.setDriverClassName("org.sqlite.JDBC");
        dbConf.setJdbcUrl(url);
        dbConf.setAutoCommit(true);
        config.options.forEach(dbConf::addDataSourceProperty);
        dbConf.setConnectionInitSql("SELECT 1");

        hikari = new HikariDataSource(dbConf);
        return true;
    }

    @Override
    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    @Override
    public void closeConnection() {
        if (hikari != null && !hikari.isClosed())
            hikari.close();
        hikari = null;
    }

    private Connection getConnection(boolean reconnect) throws SQLException {
        if ((hikari == null || isClosed()) && (!reconnect || !openConnection())) {
            throw new IllegalStateException("Connection is closed");
        }
        return hikari.getConnection();
    }

    private Connection getConnectionTry() throws SQLException {
        return getConnection(true);
    }

    @Override
    public void initDatabase() throws SQLException {
        try (Connection connection = getConnection(false)) {
            String sql = "CREATE TABLE IF NOT EXISTS `scheduled` (`id` VARCHAR(36) UNIQUE, `player` VARCHAR(36), `c_minutes` INT, `c_minutes_max` INT, `last_played` BIGINT, `notify_time` BIGINT)";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

    private ScheduledCombak deserializeScheduledCombak(ResultSet resultSet) throws SQLException {
        int mMax = resultSet.getInt("c_minutes_max");
        return new ScheduledCombak(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("player")),
                resultSet.getInt("c_minutes"),
                0 < mMax ? mMax : null,
                resultSet.getLong("last_played"),
                resultSet.getLong("notify_time")
        );
    }

    @Override
    public List<ScheduledCombak> getScheduledAll() throws SQLException {
        String sql = "SELECT * FROM `scheduled`";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet resultSet = stmt.executeQuery()) {

            List<ScheduledCombak> scheduledList = new ArrayList<>();
            while (resultSet.next()) {
                scheduledList.add(deserializeScheduledCombak(resultSet));
            }
            return scheduledList;
        }
    }

    @Override
    public void addScheduled(Collection<ScheduledCombak> scheduledList) throws SQLException {
        String sql = "INSERT OR REPLACE INTO `scheduled` VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (ScheduledCombak scheduled : scheduledList) {
                stmt.setString(1, scheduled.getScheduleId().toString());
                stmt.setString(2, scheduled.getPlayerId().toString());
                stmt.setInt(3, scheduled.getConfiguredMinutes());
                stmt.setInt(4, Optional.ofNullable(scheduled.getConfiguredMinutesMax()).orElse(-1));
                stmt.setLong(5, scheduled.getLastPlayed());
                stmt.setLong(6, scheduled.getNotifySendTime());
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public void removeScheduledByUUID(Collection<UUID> scheduledList) throws SQLException {
        String sql = "DELETE FROM `scheduled` WHERE `id` = ?";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (UUID scheduledId : scheduledList) {
                stmt.setString(1, scheduledId.toString());
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public void removeScheduledByPlayer(Collection<UUID> players) throws SQLException {
        String sql = "DELETE FROM `scheduled` WHERE `player` = ?";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (UUID playerId : players) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            }
        }
    }

}
