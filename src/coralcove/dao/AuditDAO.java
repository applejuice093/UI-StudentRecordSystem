package coralcove.dao;

import coralcove.database.DatabaseManager;
import coralcove.util.AppLogger;
import coralcove.util.DateUtils;
import java.sql.Connection;
import java.sql.SQLException;

public final class AuditDAO {

    public void log(Long userId, String action, String entity, String entityId, String detail) {
        try (Connection connection = DatabaseManager.getConnection()) {
            log(connection, userId, action, entity, entityId, detail);
        } catch (SQLException ex) {
            AppLogger.error("Audit log failed", ex);
        }
    }

    public void log(Connection connection, Long userId, String action, String entity, String entityId, String detail)
            throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO audit_logs(user_id, action, entity, entity_id, detail, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            Jdbc.nullableLong(ps, 1, userId);
            ps.setString(2, action);
            ps.setString(3, entity);
            ps.setString(4, entityId);
            ps.setString(5, detail);
            ps.setString(6, DateUtils.nowStamp());
            ps.executeUpdate();
        }
    }
}
