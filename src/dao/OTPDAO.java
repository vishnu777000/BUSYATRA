package dao;

import config.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OTPDAO {

    /* ================= SAVE OTP ================= */

    public boolean saveOTP(String email, String otp) {

        String deleteOld =
                "DELETE FROM password_otps WHERE email=?";

        String insert =
                "INSERT INTO password_otps (email, otp_code, expires_at, used) " +
                "VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE), 0)";

        try (Connection con = DBConfig.getConnection()) {

            con.setAutoCommit(false); // 🔥 transaction

            try (
                    PreparedStatement ps1 = con.prepareStatement(deleteOld);
                    PreparedStatement ps2 = con.prepareStatement(insert)
            ) {

                ps1.setString(1, email);
                ps1.executeUpdate();

                ps2.setString(1, email);
                ps2.setString(2, otp);

                int inserted = ps2.executeUpdate();

                con.commit();

                return inserted > 0;

            } catch (Exception e) {
                con.rollback();
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= VERIFY OTP ================= */

    public boolean verifyOTP(String email, String otp) {

        String sql =
                "SELECT id FROM password_otps " +
                "WHERE email=? AND otp_code=? " +
                "AND used=0 AND expires_at > NOW() " +
                "ORDER BY id DESC LIMIT 1";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, email);
            ps.setString(2, otp);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                // 🔥 mark used immediately
                markUsed(email, otp);

                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /* ================= MARK USED ================= */

    private void markUsed(String email, String otp) {

        String sql =
                "UPDATE password_otps SET used=1 " +
                "WHERE email=? AND otp_code=?";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, email);
            ps.setString(2, otp);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= CLEAN EXPIRED ================= */

    public void deleteExpiredOTPs() {

        String sql =
                "DELETE FROM password_otps WHERE expires_at < NOW()";

        try (
                Connection con = DBConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}