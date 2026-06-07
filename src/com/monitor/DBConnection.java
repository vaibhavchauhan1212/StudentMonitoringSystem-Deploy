package com.monitor;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:mysql://host.docker.internal:3306/student_monitor_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";    private static final String USER = "root"; 
    private static final String PASSWORD = "root"; 

    public static Connection getConnection() {
        Connection connectionInstance = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connectionInstance = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.err.println("JDBC Connection Error: " + e.getMessage());
        }
        return connectionInstance;
    }
}