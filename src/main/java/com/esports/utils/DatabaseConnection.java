package com.esports.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/pi_webjava"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true"
            + "&characterEncoding=UTF-8";
    private static final String USER     = "root";   // ← à adapter
    private static final String PASSWORD = "";       // ←  adapter

    private static Connection instance = null;

    private DatabaseConnection() {}

    public static Connection getInstance() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] ✔ Connexion MySQL établie.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL introuvable.", e);
            }
        }
        return instance;
    }

    public static void closeConnection() {
        if (instance != null) {
            try {
                instance.close();
                instance = null;
                System.out.println("[DB] Connexion MySQL fermée.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}