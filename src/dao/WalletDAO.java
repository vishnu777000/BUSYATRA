package dao;

import config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletDAO {

    /* ================= CREATE WALLET ================= */

    private void createWalletIfNotExists(int userId){

        String sql =
                "INSERT IGNORE INTO wallets(user_id,balance) VALUES (?,0)";

        try(Connection con = DBConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setInt(1,userId);
            ps.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /* ================= BALANCE ================= */

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
            e.printStackTrace();
        }

        return 0;
    }

    /* ================= ADD MONEY ================= */

    public boolean addMoney(int userId, double amount) {

        createWalletIfNotExists(userId);

        String updateSql =
                "UPDATE wallets SET balance = balance + ? WHERE user_id=?";

        String logSql =
                "INSERT INTO wallet_transactions " +
                "(user_id, type, amount, status, created_at) " +
                "VALUES (?,?,?,?,NOW())";

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
                log.setString(4, "SUCCESS");

                log.executeUpdate();

                con.commit();
                return true;

            } catch (Exception e) {

                con.rollback();
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= DEDUCT MONEY ================= */

    public boolean deductMoney(int userId, double amount) {

        createWalletIfNotExists(userId);

        String updateSql =
                "UPDATE wallets SET balance = balance - ? " +
                "WHERE user_id=? AND balance >= ?";

        String logSql =
                "INSERT INTO wallet_transactions " +
                "(user_id, type, amount, status, created_at) " +
                "VALUES (?,?,?,?,NOW())";

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
                log.setString(4, "SUCCESS");

                log.executeUpdate();

                con.commit();
                return true;

            } catch (Exception e) {

                con.rollback();
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= CHECK BALANCE ================= */

    public boolean hasEnoughBalance(int userId, double amount){

        return getBalance(userId) >= amount;
    }

    /* ================= TRANSACTION HISTORY ================= */

    public List<String[]> getTransactions(int userId) {

        List<String[]> list = new ArrayList<>();

        String sql =
                "SELECT created_at, type, amount, status " +
                "FROM wallet_transactions " +
                "WHERE user_id=? " +
                "ORDER BY created_at DESC";

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
            e.printStackTrace();
        }

        return list;
    }
}