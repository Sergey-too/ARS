package com.example.ars.data_base;

import java.sql.*;

public class DatabaseHelper {

    // Данные подключения
    private static final String SERVER = "192.168.0.110,1433";
    private static final String DATABASE = "GardenDB";
    private static final String USER = "sa";
    private static final String PASSWORD = "KBiPgardeN1";

    private static final String URL =
            "jdbc:sqlserver://" + SERVER +
                    ";databaseName=" + DATABASE +
                    ";user=" + USER +
                    ";password=" + PASSWORD +
                    ";encrypt=true;trustServerCertificate=true;" +
                    "loginTimeout=30;";

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("✅ SQL Server Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQL Server Driver not found: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL);
            System.out.println("✅ Connected to SQL Server successfully");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ Connection error: " + e.getMessage());
            throw e;
        }
    }

    // Регистрация - ИСПРАВЛЕНО под твою таблицу
    public String registerUser(String username, String email, String password, String confirmPassword) {
        // Проверяем на клиенте
        if (!password.equals(confirmPassword)) {
            return "Пароли не совпадают";
        }

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return "Заполните все поля";
        }

        // ВАЖНО: Имена столбцов из твоей таблицы: login, password_hash, email, registration_date
        String checkSql = "SELECT id FROM users WHERE login = ? OR email = ?";
        String insertSql = "INSERT INTO users (login, email, password_hash) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            // Проверяем существование пользователя
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                return "Пользователь с таким логином или email уже существует";
            }

            // Регистрируем нового пользователя
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                // В твоей таблице: login, email, password_hash
                insertStmt.setString(1, username);  // login
                insertStmt.setString(2, email);     // email
                insertStmt.setString(3, password);  // password_hash (пока без хеширования)

                int rows = insertStmt.executeUpdate();

                if (rows > 0) {
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            System.out.println("✅ User registered with ID: " + generatedKeys.getInt(1));
                        }
                    }
                    return null; // Успех
                } else {
                    return "Ошибка при создании пользователя";
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Register SQL error: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка базы данных: " + e.getMessage();
        }
    }
    public boolean loginUser(String username, String password) {
        // В твоей таблице столбец называется login, а не username
        String sql = "SELECT id FROM users WHERE login = ? AND password_hash = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);  // login
            stmt.setString(2, password);  // password_hash

            ResultSet rs = stmt.executeQuery();

            boolean success = rs.next();
            System.out.println("🔐 Login attempt for " + username + ": " + (success ? "SUCCESS" : "FAILED"));
            return success;

        } catch (SQLException e) {
            System.err.println("❌ Login SQL error: " + e.getMessage());
            return false;
        }
    }
}