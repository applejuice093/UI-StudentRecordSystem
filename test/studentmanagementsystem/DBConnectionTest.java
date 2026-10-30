package studentmanagementsystem;

import java.sql.SQLException;

/**
 * Tests for {@link DBConnection} that need no database: they run without
 * the MySQL driver on the classpath. Run with `make test`.
 */
public class DBConnectionTest {

    private static int failures = 0;
    private static int passed = 0;

    private static void check(String what, boolean condition) {
        if (condition) { passed++; } else { failures++; System.err.println("FAIL: " + what); }
    }

    public static void main(String[] args) {
        check("unset variable falls back", DBConnection.setting("SRS_TEST_SURELY_UNSET_VAR", "fb").equals("fb"));

        boolean threw = false;
        try {
            DBConnection.getConnection().close();
        } catch (SQLException ex) {
            threw = true;
            String msg = String.valueOf(ex.getMessage());
            // Only meaningful when no driver is present (the case under `make test`).
            check("missing driver gives a helpful message", msg.contains("mysql-connector-j"));
        }
        check("connection fails without a driver", threw);

        System.out.println("DBConnectionTest: " + passed + " passed, " + failures + " failed");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
