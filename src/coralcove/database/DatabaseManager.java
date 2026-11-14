package coralcove.database;

import coralcove.util.AppException;
import coralcove.util.AppLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens SQLite connections and creates the database file on first use. */
public final class DatabaseManager {

    private static Path databaseFile;

    private DatabaseManager() {
    }

    public static synchronized void init(Path dbFile) {
        try {
            Class.forName("org.sqlite.JDBC");
            if (dbFile.getParent() != null) {
                Files.createDirectories(dbFile.getParent());
            }
            databaseFile = dbFile.toAbsolutePath().normalize();
            try (Connection connection = open()) {
                DatabaseMigration.migrate(connection);
            }
            AppLogger.info("Database ready at " + databaseFile);
        } catch (Exception ex) {
            throw new AppException("Could not initialize the database: " + ex.getMessage(), ex);
        }
    }

    public static Path databaseFile() {
        if (databaseFile == null) {
            throw new AppException("Database is not initialized.");
        }
        return databaseFile;
    }

    public static Connection getConnection() throws SQLException {
        if (databaseFile == null) {
            throw new SQLException("Database is not initialized.");
        }
        return open();
    }

    private static Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }
}
