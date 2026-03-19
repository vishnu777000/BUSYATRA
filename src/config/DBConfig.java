package config;

import java.sql.Connection;
import java.sql.DriverManager;
import util.EnvLoader;
public class DBConfig {

    private static final String HOST = EnvLoader.get("DB_HOST");
    private static final String PORT = EnvLoader.get("DB_PORT");
    private static final String DB   = EnvLoader.get("DB_DB");

    private static final String USER = EnvLoader.get("DB_USER");
    private static final String PASS = EnvLoader.get("DB_PASS");

  private static final String URL =
    "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB
    + "?sslMode=REQUIRED"
    + "&allowPublicKeyRetrieval=true"
    + "&useSSL=true"
    + "&serverTimezone=UTC";


    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found!", e);
        }
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            System.out.println("❌ DB Connection Failed: " + e.getMessage());
            return null;
        }
    }

    private DBConfig() {}
}
