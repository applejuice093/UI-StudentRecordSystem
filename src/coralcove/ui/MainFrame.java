package coralcove.ui;

import coralcove.config.AppConfig;
import coralcove.model.AppModule;
import coralcove.service.Session;
import coralcove.ui.billing.BillingPanel;
import coralcove.ui.arrivals.ArrivalPanel;
import coralcove.ui.components.Sidebar;
import coralcove.ui.components.Theme;
import coralcove.ui.components.TopBar;
import coralcove.ui.dashboard.DashboardPanel;
import coralcove.ui.employees.EmployeePanel;
import coralcove.ui.guests.GuestPanel;
import coralcove.ui.lodgecare.LodgeCarePanel;
import coralcove.ui.inventory.InventoryPanel;
import coralcove.ui.reports.ReportsPanel;
import coralcove.ui.stays.StayPanel;
import coralcove.ui.experiences.ExperiencePanel;
import coralcove.ui.lodges.LodgeManagementPanel;
import coralcove.ui.settings.SettingsPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final TopBar topBar;
    private final Sidebar sidebar;
    private AppModule current = AppModule.DASHBOARD;

    public MainFrame() {
        super(AppConfig.WINDOW_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 750));
        setSize(1280, 800);
        content.setBackground(Theme.CONTENT);
        addModule(AppModule.DASHBOARD, new DashboardPanel());
        addModule(AppModule.STAYS, new StayPanel());
        addModule(AppModule.LODGES, new LodgeManagementPanel());
        addModule(AppModule.GUESTS, new GuestPanel());
        addModule(AppModule.ARRIVALS, new ArrivalPanel());
        addModule(AppModule.BILLING, new BillingPanel());
        addModule(AppModule.LODGE_CARE, new LodgeCarePanel());
        addModule(AppModule.EXPERIENCES, new ExperiencePanel());
        addModule(AppModule.EMPLOYEES, new EmployeePanel());
        addModule(AppModule.INVENTORY, new InventoryPanel());
        addModule(AppModule.REPORTS, new ReportsPanel());
        addModule(AppModule.SETTINGS, new SettingsPanel());
        topBar = new TopBar(this::changePassword, this::logout, () -> show(current));
        sidebar = new Sidebar(this::show);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(content, BorderLayout.CENTER);
        show(AppModule.DASHBOARD);
        Session.onDataChanged(() -> SwingUtilities.invokeLater(() -> {
            // Panels reload when shown. Dashboard also listens itself.
        }));
        setLocationRelativeTo(null);
    }

    private void addModule(AppModule module, JPanel panel) {
        content.add(panel, module.name());
    }

    private void show(AppModule module) {
        current = module;
        cards.show(content, module.name());
        topBar.setSection(module.label());
        sidebar.highlight(module);
        for (var component : content.getComponents()) {
            if (component.isVisible() && component instanceof Refreshable refreshable) {
                refreshable.refresh();
            }
        }
    }

    private void changePassword() {
        ChangePasswordDialog.show(this, Session.get(), false);
    }

    private void logout() {
        Session.clear();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
