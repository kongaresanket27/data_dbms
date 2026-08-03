package com.instiasset;

public class TestDriver {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            System.out.println("Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found");
        }
    }
}