package config;

import util.EnvLoader;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConfig {

    private static final String HOST = require("DB_HOST");
    private static final String PORT = require("DB_PORT");
    private static final String DB = require("DB_DB");
    private static final String USER = require("DB_USER");
    private static final String PASS = require("DB_PASS");
    private static final String SSL_MODE = optional("DB_SSL_MODE", "REQUIRED");
    private static final String CONNECT_TIMEOUT_MS = optional("DB_CONNECT_TIMEOUT_MS", "7000");
    private static final String SOCKET_TIMEOUT_MS = optional("DB_SOCKET_TIMEOUT_MS", "10000");
    private static final long RETRY_COOLDOWN_MS = optionalLong("DB_RETRY_COOLDOWN_MS", 5000L);

    private static volatile long blockedUntilMs = 0L;
    private static volatile String lastFailureMessage = "Database is unreachable. Check MySQL server and DB settings.";

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB +
            "?sslMode=" + SSL_MODE +
            "&allowPublicKeyRetrieval=true" +
            "&serverTimezone=UTC" +
            "&connectTimeout=" + CONNECT_TIMEOUT_MS +
            "&socketTimeout=" + SOCKET_TIMEOUT_MS +
            "&tcpKeepAlive=true";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                int seconds = Math.max(2, Integer.parseInt(CONNECT_TIMEOUT_MS) / 1000);
                DriverManager.setLoginTimeout(seconds);
            } catch (Exception ignored) {
                DriverManager.setLoginTimeout(3);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
        
    }

    public static Connection getConnection() throws SQLException {
        long now = System.currentTimeMillis();
        if (now < blockedUntilMs) {
            throw new SQLException(lastFailureMessage, "08001");
        }

        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASS);
            blockedUntilMs = 0L;
            lastFailureMessage = "Database is reachable.";
            return connection;
        } catch (SQLException e) {
            if (isConnectionUnavailable(e)) {
                lastFailureMessage = userFriendlyMessage(e);
                blockedUntilMs = System.currentTimeMillis() + RETRY_COOLDOWN_MS;
                throw new SQLException(lastFailureMessage, e);
            }
            throw e;
        }
    }

    private static String require(String key) {
        String value = EnvLoader.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing DB config key: " + key);
        }

        return value;
    }

    private static String optional(String key, String fallback) {
        String value = EnvLoader.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static long optionalLong(String key, long fallback) {
        String value = EnvLoader.get(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static boolean isConnectionUnavailable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException) {
                String state = ((SQLException) current).getSQLState();
                if (state != null && state.startsWith("08")) {
                    return true;
                }
            }

            if (current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException) {
                return true;
            }

            String typeName = current.getClass().getName();
            if (typeName.contains("CommunicationsException")
                    || typeName.contains("CJCommunicationsException")) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    public static String userFriendlyMessage(Throwable error) {
        Throwable root = rootCause(error);
        if (root instanceof UnknownHostException) {
            return "Database host could not be resolved. Check DB_HOST.";
        }
        if (root instanceof ConnectException) {
            return "Database connection was refused. Make sure MySQL is running and reachable.";
        }
        if (root instanceof SocketTimeoutException) {
            return "Database connection timed out. Check MySQL reachability, firewall, or DB_HOST/DB_PORT.";
        }
        if (isConnectionUnavailable(error)) {
            return "Database is unreachable. Check MySQL server and DB settings.";
        }

        String message = root == null ? null : root.getMessage();
        return message == null || message.isBlank() ? "Database request failed." : message.trim();
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private DBConfig() {
    }
}
