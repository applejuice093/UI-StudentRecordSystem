package coralcove.dao;

import java.sql.Connection;
import java.sql.SQLException;

public final class SettingsDAO {

    public String get(Connection connection, String key) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT value FROM group_settings WHERE key = ?")) {
            ps.setString(1, key);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void put(Connection connection, String key, String value) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO group_settings(key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
