package coralcove;

import coralcove.dao.UserDAO;
import coralcove.database.DatabaseManager;
import coralcove.model.User;
import coralcove.service.Session;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;

public abstract class DbFixture {
    protected Path dir;

    @Before
    public void openDatabase() throws Exception {
        dir = Files.createTempDirectory("coralcove");
        DatabaseManager.init(dir.resolve("coralcove.db"));
        try (var connection = DatabaseManager.getConnection()) {
            User admin = new UserDAO().findByUsername(connection, "admin");
            Session.set(admin);
        }
    }

    @After
    public void closeDatabase() throws Exception {
        Session.clear();
        coralcove.service.SettingsService.clearCache();
        if (dir != null) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // Windows may keep the SQLite file until the connection is fully released.
                    }
                });
            }
        }
    }
}
