package coralcove.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {

    private static final Logger LOG = Logger.getLogger("coralcove");

    static {
        try {
            Path dir = Path.of("data");
            Files.createDirectories(dir);
            FileHandler handler = new FileHandler(dir.resolve("coralcove.log").toString(), 1_000_000, 2, true);
            handler.setFormatter(new SimpleFormatter());
            LOG.addHandler(handler);
            LOG.setLevel(Level.INFO);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "File logging is unavailable", ex);
        }
    }

    private AppLogger() {
    }

    public static void info(String message) {
        LOG.info(message);
    }

    public static void error(String message, Throwable error) {
        LOG.log(Level.SEVERE, message, error);
    }
}
