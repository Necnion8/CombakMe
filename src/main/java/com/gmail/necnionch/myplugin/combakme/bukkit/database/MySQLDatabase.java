package com.gmail.necnionch.myplugin.combakme.bukkit.database;

import com.gmail.necnionch.myplugin.combakme.bukkit.CombakPlayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

public class MySQLDatabase implements Database {

    private final Config config;
    private @Nullable HikariDataSource hikari;

    public static class Config {

        private final String address;
        private final String database;
        private final String username;
        private final String password;
        private final Map<String, Object> options;

        public Config(String address, String database, String username, String password, Map<String, Object> options) {
            this.address = address;
            this.database = database;
            this.username = username;
            this.password = password;
            this.options = options;
        }

        public String getAddress() {
            return address;
        }

        public String getDatabase() {
            return database;
        }

        public String getUsername() {
            return username;
        }

        public Map<String, Object> options() {
            return options;
        }
    }

    public MySQLDatabase(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }


    @Override
    public boolean openConnection() {
        if (!isClosed())
            throw new IllegalStateException("Already connection available");

        String url = "jdbc:mysql://" + config.address + "/" + config.database;
        HikariConfig dbConf = new HikariConfig();
        dbConf.setPoolName("CombakMe-HikariPool");
        dbConf.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dbConf.setJdbcUrl(url);
        dbConf.addDataSourceProperty("user", config.username);
        dbConf.addDataSourceProperty("password", config.password);
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
            String sql = "CREATE TABLE IF NOT EXISTS `notified` (`uuid` VARCHAR(36) UNIQUE, `lastHours` INT)";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

    @Override
    public List<CombakPlayer> getPlayerAll() throws SQLException {
        String sql = "SELECT * FROM `notified`";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet resultSet = stmt.executeQuery()) {

            List<CombakPlayer> players = new ArrayList<>();
            while (resultSet.next()) {
                CombakPlayer p = new CombakPlayer(UUID.fromString(resultSet.getString("uuid")));
                p.setLastNotifyHours(resultSet.getInt("lastHours"));
                players.add(p);
            }
            return players;
        }
    }

    @Override
    public void setPlayers(Collection<CombakPlayer> players) throws SQLException {
        if (players.isEmpty())
            return;

        String sql = "INSERT INTO `notified` VALUES (?, ?) ON DUPLICATE KEY UPDATE `lastHours` = ?";
        try (Connection conn = getConnectionTry();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (CombakPlayer player : players) {
                stmt.setString(1, player.getId().toString());
                stmt.setInt(2, player.getLastNotifyHours().orElse(-1));
                stmt.setInt(3, player.getLastNotifyHours().orElse(-1));
                stmt.executeUpdate();
            }
        }
    }

}
