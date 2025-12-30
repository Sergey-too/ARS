package com.example.ars.data_base;

import java.sql.*;

public class DatabaseHelper {

    private static final String URL = "jdbc:sqlserver://192.168.0.110,1433;databaseName=GardenDB;encrypt=true;trustServerCertificate=true";
    private static final String USER = "sa";
    private static final String PASSWORD = "KBiPgardeN1";

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("SQL Server Driver not found: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Регистрация - возвращает сообщение об ошибке или null если успешно
    public String registerUser(String username, String email, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return "Пароли не совпадают";
        }

        String checkSql = "SELECT id FROM users WHERE username = ? OR email = ?";
        String insertSql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            // Проверяем существование пользователя
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                return "Пользователь с таким логином или email уже существует";
            }

            // Регистрируем
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, email);
                insertStmt.setString(3, password); // В реальном приложении нужно хешировать!

                int rows = insertStmt.executeUpdate();
                if (rows > 0) {
                    return null; // Успешно, ошибок нет
                } else {
                    return "Ошибка при создании пользователя";
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка подключения к базе данных: " + e.getMessage();
        }
    }

    // Вход - возвращает true/false
    public boolean loginUser(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            return rs.next(); // Если есть результат - вход успешен

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
