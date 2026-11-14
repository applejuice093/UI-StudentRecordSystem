package coralcove.ui;

import coralcove.config.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

final class PreferencesStore {
    private PreferencesStore() {
    }

    static String rememberedUsername() {
        Properties properties = load();
        return properties.getProperty("rememberedUsername", "");
    }

    static void remember(String username) {
        Properties properties = load();
        if (username == null || username.isBlank()) {
            properties.remove("rememberedUsername");
        } else {
            properties.setProperty("rememberedUsername", username.trim());
        }
        try {
            Files.createDirectories(AppConfig.dataDir());
            try (OutputStream out = Files.newOutputStream(AppConfig.preferencesPath())) {
                properties.store(out, "CoralCove local preferences. Passwords are never stored here.");
            }
        } catch (IOException ex) {
            coralcove.util.AppLogger.error("Could not save preferences", ex);
        }
    }

    private static Properties load() {
        Properties properties = new Properties();
        if (Files.exists(AppConfig.preferencesPath())) {
            try (InputStream in = Files.newInputStream(AppConfig.preferencesPath())) {
                properties.load(in);
            } catch (IOException ex) {
                coralcove.util.AppLogger.error("Could not read preferences", ex);
            }
        }
        return properties;
    }
}
