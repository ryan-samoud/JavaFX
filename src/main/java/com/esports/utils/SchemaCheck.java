package com.esports.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class SchemaCheck {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getInstance();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("--- Table: jeu ---");
            ResultSet rs = stmt.executeQuery("SELECT * FROM jeu LIMIT 0");
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                System.out.println(meta.getColumnName(i) + " : " + meta.getColumnTypeName(i) + "(" + meta.getPrecision(i) + ")");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
