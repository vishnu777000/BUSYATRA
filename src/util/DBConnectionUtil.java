package util;

import config.DBConfig;

import java.sql.Connection;
import java.sql.SQLException;







public class DBConnectionUtil {

    private DBConnectionUtil() {
        
    }

    

    public static Connection getConnection() {

        try {
            return DBConfig.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(DBConfig.userFriendlyMessage(e), e);
        }
    }

    public static boolean isConnectionUnavailable(Throwable error) {
        return DBConfig.isConnectionUnavailable(error);
    }

    public static String userMessage(Throwable error) {
        return DBConfig.userFriendlyMessage(error);
    }

    public static void logIfUnexpected(Throwable error) {
        if (error != null && !isConnectionUnavailable(error)) {
            error.printStackTrace();
        }
    }

    

    public static void close(Connection c) {

        if (c == null) return;

        try {
            if (!c.isClosed()) {
                c.close();
            }
        } catch (SQLException e) {
            logIfUnexpected(e);
        }
    }

    

    public static void closeAll() {
        
    }
}
