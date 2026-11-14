package coralcove.ui.components;

import coralcove.model.Resort;
import coralcove.model.Role;
import coralcove.service.ResortService;
import coralcove.service.Session;
import coralcove.service.SettingsService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class TopBar extends JPanel {
    private final JLabel title = new JLabel("Dashboard");
    private final Runnable onPassword;
    private final Runnable onLogout;
    private final Runnable onResort;

    public TopBar(Runnable onPassword, Runnable onLogout, Runnable onResort) {
        this.onPassword = onPassword;
        this.onLogout = onLogout;
        this.onResort = onResort;
        setLayout(new BorderLayout());
        setBackground(java.awt.Color.WHITE);
        setPreferredSize(new Dimension(0, 64));
        setBorder(new EmptyBorder(8, 16, 8, 16));
        title.setFont(Theme.SECTION);
        add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(resortControl());
        JLabel who = new JLabel(Session.get().getUsername() + " · " + Session.get().getRole().label());
        who.setFont(Theme.SMALL);
        who.setForeground(Theme.MUTED);
        JButton password = Ui.secondary("Change password");
        password.addActionListener(event -> onPassword.run());
        JButton logout = Ui.primary("Logout");
        logout.addActionListener(event -> onLogout.run());
        actions.add(who);
        actions.add(password);
        actions.add(logout);
        add(actions, BorderLayout.EAST);
    }

    private java.awt.Component resortControl() {
        boolean admin = Session.get().getRole() == Role.ADMIN;
        List<Resort> resorts = new ResortService().list(true);
        if (!admin) {
            String name = SettingsService.hotelName();
            JLabel fixed = new JLabel(name);
            fixed.setFont(Theme.BODY);
            return fixed;
        }
        JComboBox<String> picker = new JComboBox<>();
        picker.addItem("All resorts");
        int select = 0;
        Long filter = Session.resortFilter();
        for (int i = 0; i < resorts.size(); i++) {
            picker.addItem(resorts.get(i).getName());
            if (filter != null && resorts.get(i).getId() == filter) {
                select = i + 1;
            }
        }
        picker.setSelectedIndex(select);
        picker.addActionListener(event -> {
            int index = picker.getSelectedIndex();
            Session.setResortFilter(index <= 0 ? null : resorts.get(index - 1).getId());
            onResort.run();
        });
        return picker;
    }

    public void setSection(String text) {
        title.setText(text);
    }
}
