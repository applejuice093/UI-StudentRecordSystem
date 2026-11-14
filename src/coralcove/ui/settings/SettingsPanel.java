package coralcove.ui.settings;

import coralcove.model.Resort;
import coralcove.model.Role;
import coralcove.model.User;
import coralcove.service.AuthenticationService;
import coralcove.service.DemoDataService;
import coralcove.service.ResortService;
import coralcove.service.Session;
import coralcove.service.SettingsService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class SettingsPanel extends JPanel implements Refreshable {
    private final AuthenticationService auth = new AuthenticationService();
    private final ResortService resorts = new ResortService();
    private final JComboBox<Resort> resort = new JComboBox<>();
    private final JTextField name = new JTextField(16);
    private final JTextField location = new JTextField(16);
    private final JTextField coastline = new JTextField(16);
    private final JTextField zone = new JTextField("Asia/Kolkata", 16);
    private final JTextField tax = new JTextField("12", 6);
    private final JCheckBox active = new JCheckBox("Active", true);
    private final DataTable users = new DataTable("Id", "Username", "Role", "Resort", "Active", "Must change password");
    private final JTextField username = new JTextField(12);
    private final JPasswordField password = new JPasswordField(12);
    private final JComboBox<Role> role = new JComboBox<>(Role.values());
    private List<User> rows = List.of();

    public SettingsPanel() {
        super(new BorderLayout(0, 12));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        active.setOpaque(false);
        resort.addActionListener(event -> showResort());
        form.add(new JLabel("Resort"), Ui.constraints(0, 0));
        form.add(resort, Ui.constraints(0, 1));
        form.add(new JLabel("Name"), Ui.constraints(1, 0));
        form.add(name, Ui.constraints(1, 1));
        form.add(new JLabel("Location"), Ui.constraints(2, 0));
        form.add(location, Ui.constraints(2, 1));
        form.add(new JLabel("Coastline"), Ui.constraints(3, 0));
        form.add(coastline, Ui.constraints(3, 1));
        form.add(new JLabel("Timezone"), Ui.constraints(4, 0));
        form.add(zone, Ui.constraints(4, 1));
        form.add(new JLabel("Tax percent"), Ui.constraints(5, 0));
        form.add(tax, Ui.constraints(5, 1));
        form.add(active, Ui.constraints(6, 1));
        JButton save = Ui.primary("Save resort");
        save.addActionListener(event -> saveResort(false));
        JButton addResort = Ui.secondary("Add resort");
        addResort.addActionListener(event -> saveResort(true));
        form.add(save, Ui.constraints(7, 0));
        form.add(addResort, Ui.constraints(7, 1));
        JLabel demoNote = new JLabel("Demonstration data is labeled Sample and is not loaded until you ask.");
        form.add(demoNote, Ui.constraints(8, 0));
        JButton load = Ui.secondary("Load demonstration data");
        load.addActionListener(event -> {
            if (Ui.confirm(this, "Load sample lodges, guests, bookings, and payments? Existing real records stay.")) {
                Ui.run(this, () -> {
                    new DemoDataService().load();
                    return null;
                }, result -> Ui.info(this, "Demonstration data loaded. Figures from these rows are samples."));
            }
        });
        JButton reset = Ui.secondary("Reset demonstration data");
        reset.addActionListener(event -> {
            if (Ui.confirm(this, "Remove only rows marked as demonstration data?")) {
                Ui.run(this, () -> {
                    new DemoDataService().reset();
                    return null;
                }, result -> Ui.info(this, "Demonstration rows were removed. Real records were kept."));
            }
        });
        form.add(load, Ui.constraints(9, 0));
        form.add(reset, Ui.constraints(9, 1));
        add(form, BorderLayout.NORTH);

        JPanel people = new JPanel(new BorderLayout(0, 8));
        people.setOpaque(false);
        people.add(users, BorderLayout.CENTER);
        JPanel create = new JPanel();
        create.setOpaque(false);
        create.add(new JLabel("Username"));
        create.add(username);
        create.add(new JLabel("Password"));
        create.add(password);
        create.add(role);
        JButton add = Ui.primary("Create user");
        add.addActionListener(event -> Ui.run(this, () -> {
            auth.createUser(username.getText(), new String(password.getPassword()), (Role) role.getSelectedItem());
            return null;
        }, result -> {
            password.setText("");
            refresh();
        }));
        JButton deactivate = Ui.secondary("Toggle active");
        deactivate.addActionListener(event -> toggle());
        create.add(add);
        create.add(deactivate);
        people.add(create, BorderLayout.SOUTH);
        add(people, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.run(this, () -> resorts.list(false), result -> {
            Resort selected = (Resort) resort.getSelectedItem();
            resort.removeAllItems();
            for (Resort row : (List<Resort>) result) {
                resort.addItem(row);
            }
            if (selected != null) {
                for (int i = 0; i < resort.getItemCount(); i++) {
                    if (resort.getItemAt(i).getId() == selected.getId()) {
                        resort.setSelectedIndex(i);
                    }
                }
            }
            showResort();
        });
        Ui.run(this, auth::listUsers, result -> {
            rows = (List<User>) result;
            List<String[]> data = new ArrayList<>();
            for (User user : rows) {
                data.add(new String[] {
                        String.valueOf(user.getId()), user.getUsername(), user.getRole().name(),
                        user.getResortId() == null ? "All" : String.valueOf(user.getResortId()),
                        user.isActive() ? "Yes" : "No", user.isMustChangePassword() ? "Yes" : "No"
                });
            }
            users.setRows(data);
        });
    }

    private void showResort() {
        Resort row = (Resort) resort.getSelectedItem();
        if (row == null) {
            return;
        }
        name.setText(row.getName());
        location.setText(row.getLocation());
        coastline.setText(row.getCoastline() == null ? "" : row.getCoastline());
        zone.setText(row.getTimezone());
        tax.setText(row.getTaxPercent());
        active.setSelected(row.isActive());
    }

    private void saveResort(boolean create) {
        Ui.run(this, () -> {
            Resort row = create || resort.getSelectedItem() == null ? new Resort() : (Resort) resort.getSelectedItem();
            if (create) {
                row = new Resort();
            }
            row.setName(name.getText());
            row.setLocation(location.getText());
            row.setCoastline(coastline.getText());
            row.setTimezone(zone.getText());
            row.setTaxPercent(tax.getText());
            row.setActive(active.isSelected());
            return resorts.save(row);
        }, result -> {
            Ui.info(this, "Resort saved. Inactive resorts stay in history and leave the top-bar list.");
            refresh();
        });
    }

    private void toggle() {
        int index = users.selectedRow();
        if (index < 0) {
            Ui.error(this, "Select a user first.");
            return;
        }
        User user = rows.get(index);
        user.setActive(!user.isActive());
        Ui.run(this, () -> {
            auth.updateUser(user);
            return null;
        }, result -> refresh());
    }
}
