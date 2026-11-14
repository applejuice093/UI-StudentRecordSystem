package coralcove;

import coralcove.config.AppConfig;
import coralcove.database.DatabaseManager;
import coralcove.ui.LoginFrame;
import coralcove.util.AppLogger;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception ex) {
                AppLogger.error("FlatLaf did not load. Using the system look and feel.", ex);
            }
            try {
                DatabaseManager.init(AppConfig.databasePath());
            } catch (RuntimeException ex) {
                AppLogger.error("Startup failed", ex);
                JOptionPane.showMessageDialog(null, ex.getMessage(), "CoralCove", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new LoginFrame().setVisible(true);
        });
    }
}
