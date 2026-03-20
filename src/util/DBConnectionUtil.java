package util;

import config.DBConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBConnectionUtil
 * ------------------------------------
 * Central JDBC connection helper.
 * Returns a fresh connection each call.
 */
public class DBConnectionUtil {

    private DBConnectionUtil() {
        // utility class
    }

    /* ================= GET CONNECTION ================= */

    public static Connection getConnection() {

        try {
            return DBConfig.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    /* ================= CLOSE SINGLE ================= */

    public static void close(Connection c) {

        if (c == null) return;

        try {
            if (!c.isClosed()) {
                c.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* ================= CLOSE GLOBAL ================= */

    public static void closeAll() {
        // No-op. Connections are not globally cached anymore.
    }
}
