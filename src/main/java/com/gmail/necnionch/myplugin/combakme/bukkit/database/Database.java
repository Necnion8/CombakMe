package com.gmail.necnionch.myplugin.combakme.bukkit.database;

import com.gmail.necnionch.myplugin.combakme.bukkit.schedule.ScheduledCombak;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface Database {

    boolean openConnection() throws SQLException;

    boolean isClosed();

    void closeConnection() throws Exception;

    void initDatabase() throws SQLException;


    List<ScheduledCombak> getScheduledAll() throws SQLException;

    void addScheduled(Collection<ScheduledCombak> scheduledList) throws SQLException;

    void removeScheduledByUUID(Collection<UUID> scheduledList) throws SQLException;

    void removeScheduledByPlayer(Collection<UUID> players) throws SQLException;

}
