package com.instiasset;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnectionTest {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("Connected to Oracle successfully.");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'DEPARTMENT' ORDER BY COLUMN_ID")) {
                while (rs.next()) {
                    System.out.println("COLUMN: " + rs.getString("COLUMN_NAME"));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Connection failed: " + ex.getMessage());
            System.exit(1);
        }
    }
}
