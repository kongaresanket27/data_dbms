package com.instiasset;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles the JDBC connection to Oracle XE.
 */
public class DBConnection {
    private static final String URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";
    private static final String USER = "hr";
    private static final String PASS = "hr";

    public static Connection getConnection() throws SQLException {
        try {
            // Load Oracle Driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found!");
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
