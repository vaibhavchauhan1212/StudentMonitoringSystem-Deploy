package com.monitor;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
        "jdbc:mysql://acela.proxy.rlwy.net:22448/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String USER = "root";

    private static final String PASSWORD = "qNpglkNeTjPBoejzhqgmnauiGgLdxoWp";

    public static Connection getConnection() {
        Connection connectionInstance = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connectionInstance = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.err.println("JDBC Connection Error: " + e.getMessage());
            e.printStackTrace();
        }
        return connectionInstance;
    }
}