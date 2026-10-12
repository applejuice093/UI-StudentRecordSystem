package studentmanagementsystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates JDBC connections to the MySQL database.
 *
 * Connection settings are read from environment variables so that no
 * credentials need to be stored in source control:
 *   SRS_DB_URL  (default: jdbc:mysql://localhost:3306/studentdb)
 *   SRS_DB_USER (default: root)
 *   SRS_DB_PASS (default: empty)
 */
public class DBConnection {

    static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/studentdb";
    static final String DEFAULT_USER = "root";

    private DBConnection() {
    }

    static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                setting("SRS_DB_URL", DEFAULT_URL),
                setting("SRS_DB_USER", DEFAULT_USER),
                setting("SRS_DB_PASS", ""));
    }
}
