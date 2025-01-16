package com.gmail.necnionch.myplugin.combakme.bukkit.database;

import com.gmail.necnionch.myplugin.combakme.bukkit.CombakPlayer;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public interface Database {

    boolean openConnection() throws SQLException;

    boolean isClosed();

    void closeConnection() throws Exception;

    void initDatabase() throws SQLException;


    List<CombakPlayer> getPlayerAll() throws SQLException;

    void setPlayers(Collection<CombakPlayer> players) throws SQLException;

}
