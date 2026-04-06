package dao;

import config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletDAO {

    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean walletSchemaChecked = false;

    private void ensureWalletSchema() {

        if (walletSchemaChecked) return;

        synchronized (SCHEMA_LOCK) {
            if (walletSchemaChecked) return;

            String walletSql =
                    "CREATE TABLE IF NOT EXISTS wallets (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL UNIQUE," +
                    "balance DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")";

            String txSql =
                    "CREATE TABLE IF NOT EXISTS wallet_transactions (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL," +
                    "type VARCHAR(20) NOT NULL," +
                    "amount DECIMAL(12,2) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            try (Connection con = DBConfig.getConnection();
                 PreparedStatement ps1 = con.prepareStatement(walletSql);
                 PreparedStatement ps2 = con.prepareStatement(txSql)) {

                ps1.executeUpdate();
                ps2.executeUpdate();
                walletSchemaChecked = true;

            } catch (Exception ignored) {

            }
        }
    }

    

    private boolean columnExists(String table, String column) {
        String sql =
                "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void createWalletIfNotExists(int userId){

        ensureWalletSchema();

        String sql =
                "INSERT IGNORE INTO wallets(user_id,balance) VALUES (?,0)";

        try(Connection con = DBConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setInt(1,userId);
            ps.executeUpdate();

        }catch(Exception e){
            
        }
    }

    

    public double getBalance(int userId) {

        createWalletIfNotExists(userId);

        String sql = "SELECT balance FROM wallets WHERE user_id=?";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("balance");
            }

        } catch (Exception e) {
            
        }

        return 0;
    }

    

    public boolean addMoney(int userId, double amount) {

        createWalletIfNotExists(userId);

        String updateSql =
                "UPDATE wallets SET balance = balance + ? WHERE user_id=?";

        boolean hasStatus = columnExists("wallet_transactions", "status");
        String logSql = hasStatus
                ? "INSERT INTO wallet_transactions (user_id, type, amount, status, created_at) VALUES (?,?,?,?,NOW())"
                : "INSERT INTO wallet_transactions (user_id, type, amount, created_at) VALUES (?,?,?,NOW())";

        try (Connection con = DBConfig.getConnection()) {

            con.setAutoCommit(false);

            try (
                    PreparedStatement update = con.prepareStatement(updateSql);
                    PreparedStatement log = con.prepareStatement(logSql)
            ) {

                update.setDouble(1, amount);
                update.setInt(2, userId);

                if (update.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }

                log.setInt(1, userId);
                log.setString(2, "CREDIT");
                log.setDouble(3, amount);
                if (hasStatus) {
                    log.setString(4, "SUCCESS");
                }

                log.executeUpdate();

                con.commit();
                return true;

            } catch (Exception e) {

                con.rollback();
            }

        } catch (Exception e) {
            
        }

        return false;
    }

    

    public boolean deductMoney(int userId, double amount) {

        createWalletIfNotExists(userId);

        String updateSql =
                "UPDATE wallets SET balance = balance - ? " +
                "WHERE user_id=? AND balance >= ?";

        boolean hasStatus = columnExists("wallet_transactions", "status");
        String logSql = hasStatus
                ? "INSERT INTO wallet_transactions (user_id, type, amount, status, created_at) VALUES (?,?,?,?,NOW())"
                : "INSERT INTO wallet_transactions (user_id, type, amount, created_at) VALUES (?,?,?,NOW())";

        try (Connection con = DBConfig.getConnection()) {

            con.setAutoCommit(false);

            try (
                    PreparedStatement update = con.prepareStatement(updateSql);
                    PreparedStatement log = con.prepareStatement(logSql)
            ) {

                update.setDouble(1, amount);
                update.setInt(2, userId);
                update.setDouble(3, amount);

                if (update.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }

                log.setInt(1, userId);
                log.setString(2, "DEBIT");
                log.setDouble(3, -amount);
                if (hasStatus) {
                    log.setString(4, "SUCCESS");
                }

                log.executeUpdate();

                con.commit();
                return true;

            } catch (Exception e) {

                con.rollback();
            }

        } catch (Exception e) {
            
        }

        return false;
    }

    

    public boolean hasEnoughBalance(int userId, double amount){

        return getBalance(userId) >= amount;
    }

    

    public List<String[]> getTransactions(int userId) {

        List<String[]> list = new ArrayList<>();

        boolean hasStatus = columnExists("wallet_transactions", "status");
        String sql = hasStatus
                ? "SELECT created_at, type, amount, status FROM wallet_transactions WHERE user_id=? ORDER BY created_at DESC"
                : "SELECT created_at, type, amount, 'SUCCESS' AS status FROM wallet_transactions WHERE user_id=? ORDER BY created_at DESC";

        try (Connection con = DBConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                list.add(new String[]{
                        rs.getString("created_at"),
                        rs.getString("type"),
                        rs.getString("amount"),
                        rs.getString("status")
                });
            }

        } catch (Exception e) {
            
        }

        return list;
    }
}
