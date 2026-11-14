package coralcove.config;

import java.nio.file.Path;

public final class AppConfig {

    public static final String APP_NAME = "CoralCove";
    public static final String WINDOW_TITLE = "CoralCove | Resort Management";
    public static final String DEFAULT_ADMIN = "admin";
    public static final String DEFAULT_PASSWORD = "Admin@123";

    private AppConfig() {
    }

    public static Path dataDir() {
        return Path.of("data");
    }

    public static Path databasePath() {
        String override = System.getenv("CORALCOVE_DB");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return dataDir().resolve("coralcove.db");
    }

    public static Path preferencesPath() {
        return dataDir().resolve("preferences.properties");
    }
}
