package coralcove.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Applies schema version 1. Later versions should add columns, not drop the file. */
public final class DatabaseMigration {

    public static final int CURRENT = 1;

    private DatabaseMigration() {
    }

    public static void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version INTEGER PRIMARY KEY,
                        applied_at TEXT NOT NULL
                    )
                    """);
        }
        int version = 0;
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            if (rs.next()) {
                version = rs.getInt(1);
            }
        }
        if (version < 1) {
            DatabaseInitializer.createSchema(connection);
            DatabaseInitializer.seed(connection);
            try (var ps = connection.prepareStatement(
                    "INSERT INTO schema_version(version, applied_at) VALUES (1, datetime('now'))")) {
                ps.executeUpdate();
            }
        }
    }
}
