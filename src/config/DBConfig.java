package config;

import util.EnvLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConfig {

    private static final String HOST = require("DB_HOST");
    private static final String PORT = require("DB_PORT");
    private static final String DB = require("DB_DB");
    private static final String USER = require("DB_USER");
    private static final String PASS = require("DB_PASS");

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB +
            "?sslMode=REQUIRED" +
            "&allowPublicKeyRetrieval=true" +
            "&useSSL=true" +
            "&serverTimezone=UTC";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static String require(String key) {
        String value = EnvLoader.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing DB config key: " + key);
        }

        return value;
    }

    private DBConfig() {
    }
}
