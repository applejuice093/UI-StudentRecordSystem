package coralcove.service;

import coralcove.dao.SettingsDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.util.AppException;
import java.math.BigDecimal;
import java.sql.SQLException;

public final class SettingsService {

    private static final SettingsDAO DAO = new SettingsDAO();
    private static volatile String timezoneCache;
    private static volatile Long timezoneResort;

    private SettingsService() {
    }

    public static String timezoneOrNull() {
        Long resortId = Session.resortFilter();
        if (timezoneCache != null && java.util.Objects.equals(timezoneResort, resortId)) {
            return timezoneCache;
        }
        try (var connection = DatabaseManager.getConnection()) {
            timezoneCache = readColumn(connection, resortId, "timezone");
            timezoneResort = resortId;
            return timezoneCache;
        } catch (SQLException ex) {
            return null;
        }
    }

    public static void clearCache() {
        timezoneCache = null;
        timezoneResort = null;
    }

    public static BigDecimal taxPercent() {
        try (var connection = DatabaseManager.getConnection()) {
            String raw = readColumn(connection, Session.resortFilter(), "tax_percent");
            if (raw == null || raw.isBlank()) {
                return new BigDecimal("12");
            }
            return new BigDecimal(raw);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    /** Name shown in the top bar. All resorts uses the group name. */
    public static String hotelName() {
        Long resortId = Session.resortFilter();
        if (resortId == null) {
            return "CoralCove";
        }
        try (var connection = DatabaseManager.getConnection()) {
            String name = readColumn(connection, resortId, "name");
            return name == null ? "CoralCove" : name;
        } catch (SQLException ex) {
            return "CoralCove";
        }
    }

    public static boolean demoLoaded() {
        try (var connection = DatabaseManager.getConnection()) {
            return "1".equals(DAO.get(connection, "demo_loaded"));
        } catch (SQLException ex) {
            return false;
        }
    }

    static void setDemoLoaded(boolean loaded) {
        Transactions.in(connection -> {
            DAO.put(connection, "demo_loaded", loaded ? "1" : "0");
            return null;
        });
    }

    private static String readColumn(java.sql.Connection connection, Long resortId, String column) throws SQLException {
        String sql = resortId == null
                ? "SELECT " + column + " FROM resorts WHERE active=1 ORDER BY id LIMIT 1"
                : "SELECT " + column + " FROM resorts WHERE id=?";
        try (var ps = connection.prepareStatement(sql)) {
            if (resortId != null) {
                ps.setLong(1, resortId);
            }
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
