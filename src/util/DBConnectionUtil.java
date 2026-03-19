package util;

import config.DBConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBConnectionUtil
 * ------------------------------------
 * Central JDBC connection helper.
 * Reuses a single connection safely
 * and provides proper close methods.
 */
public class DBConnectionUtil {

    private static Connection con;

    private DBConnectionUtil() {
        // utility class
    }

    /* ================= GET CONNECTION ================= */

    public static Connection getConnection() {

        try {
            if (con == null || con.isClosed()) {
                con = DBConfig.getConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return con;
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

        try {
            if (con != null && !con.isClosed()) {
                con.close();
                con = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
