package coralcove.ui.lodges;

import coralcove.model.Role;
import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.model.LodgeCategory;
import coralcove.model.Resort;
import coralcove.service.LodgeService;
import coralcove.service.ResortService;
import coralcove.service.Session;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.LodgeCard;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

public class LodgeManagementPanel extends JPanel implements Refreshable {
    private final LodgeService service = new LodgeService();
    private final JTextField search = new JTextField(12);
    private final JComboBox<String> zone = new JComboBox<>(new String[] {"All zones", "BEACH", "LAGOON", "GARDEN", "CLIFF"});
    private final JComboBox<Object> type = new JComboBox<>();
    private final JComboBox<String> status = new JComboBox<>();
    private final DataTable table = new DataTable("Id", "Unit", "Resort", "Zone", "Type", "Guests", "Price", "Status", "Sample");
    private final JPanel grid = new JPanel(new GridLayout(0, 4, 10, 10));
    private List<Lodge> lodges = List.of();
    private List<LodgeCategory> types = List.of();
    private final boolean canWrite = Session.get().getRole() == Role.ADMIN || Session.get().getRole() == Role.MANAGER;

    public LodgeManagementPanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        status.addItem("All statuses");
        for (LodgeStatus value : LodgeStatus.values()) {
            status.addItem(value.name());
        }
        type.addItem("All types");
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.setOpaque(false);
        filters.add(new JLabel("Search"));
        filters.add(search);
        filters.add(zone);
        filters.add(type);
        filters.add(status);
        var refresh = Ui.secondary("Refresh");
        refresh.addActionListener(event -> refresh());
        filters.add(refresh);
        add(filters, BorderLayout.NORTH);
        grid.setBackground(Theme.CONTENT);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Table", table);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legend.setOpaque(false);
        for (LodgeStatus value : LodgeStatus.values()) {
            JLabel chip = new JLabel(" " + value.label() + " ");
            chip.setOpaque(true);
            chip.setBackground(Theme.status(value.name()));
            chip.setForeground(java.awt.Color.WHITE);
            legend.add(chip);
        }
        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setOpaque(false);
        gridWrap.add(legend, BorderLayout.NORTH);
        gridWrap.add(new JScrollPane(grid), BorderLayout.CENTER);
        tabs.addTab("Lodge grid", gridWrap);
        add(tabs, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        if (canWrite) {
            var add = Ui.primary("Add lodge");
            add.addActionListener(event -> edit(null));
            var edit = Ui.secondary("Edit");
            edit.addActionListener(event -> edit(selected()));
            var delete = Ui.secondary("Delete");
            delete.addActionListener(event -> remove());
            var change = Ui.secondary("Change status");
            change.addActionListener(event -> changeStatus());
            actions.add(add);
            actions.add(edit);
            actions.add(delete);
            actions.add(change);
        }
        var availability = Ui.secondary("Availability");
        availability.addActionListener(event -> showAvailability());
        actions.add(availability);
        add(actions, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        String zoneValue = "All zones".equals(zone.getSelectedItem()) ? null : zone.getSelectedItem().toString();
        Long typeId = type.getSelectedItem() instanceof LodgeCategory roomType ? roomType.getId() : null;
        String statusValue = "All statuses".equals(status.getSelectedItem()) ? null : status.getSelectedItem().toString();
        Ui.run(this, () -> {
            types = service.types();
            return service.search(search.getText(), zoneValue, typeId, statusValue);
        }, result -> {
            lodges = (List<Lodge>) result;
            Object selected = type.getSelectedItem();
            type.removeAllItems();
            type.addItem("All types");
            for (LodgeCategory roomType : types) {
                type.addItem(roomType);
            }
            type.setSelectedItem(selected == null ? "All types" : selected);
            List<String[]> rows = new ArrayList<>();
            grid.removeAll();
            for (Lodge room : lodges) {
                rows.add(new String[] {
                        String.valueOf(room.getId()), room.getUnitCode(),
                        room.getResortName() == null ? "" : room.getResortName(), room.getZone(),
                        room.getRoomTypeName(), String.valueOf(room.getMaxGuests()),
                        CurrencyUtils.format(room.getPricePerNight()), room.getStatus().name(),
                        room.isSample() ? "Sample" : ""
                });
                grid.add(new LodgeCard(room));
            }
            table.setRows(rows);
            grid.revalidate();
            grid.repaint();
        });
    }

    private Lodge selected() {
        int row = table.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select a lodge first.");
            return null;
        }
        return lodges.get(row);
    }

    private void edit(Lodge existing) {
        if (existing == null && !canWrite) {
            return;
        }
        if (types.isEmpty()) {
            Ui.error(this, "Lodge types are not loaded yet.");
            return;
        }
        List<Resort> resorts = new ResortService().list(true);
        Lodge draft = LodgeDialog.show((Frame) javax.swing.SwingUtilities.getWindowAncestor(this), existing, types, resorts);
        if (draft == null) {
            return;
        }
        boolean create = existing == null;
        Ui.run(this, () -> service.save(draft, create), result -> refresh());
    }

    private void remove() {
        Lodge room = selected();
        if (room == null || !Ui.confirm(this, "Delete room " + room.getUnitCode() + "?")) {
            return;
        }
        Ui.run(this, () -> {
            service.delete(room.getId());
            return null;
        }, result -> refresh());
    }

    private void changeStatus() {
        Lodge room = selected();
        if (room == null) {
            return;
        }
        LodgeStatus chosen = (LodgeStatus) javax.swing.JOptionPane.showInputDialog(this, "New status", "Lodge status",
                javax.swing.JOptionPane.PLAIN_MESSAGE, null, LodgeStatus.values(), room.getStatus());
        if (chosen == null) {
            return;
        }
        Ui.run(this, () -> {
            service.changeStatus(room.getId(), chosen);
            return null;
        }, result -> refresh());
    }

    private void showAvailability() {
        JSpinner from = Ui.dateSpinner(DateUtils.today());
        JSpinner to = Ui.dateSpinner(DateUtils.today().plusDays(1));
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Check-in"));
        form.add(from);
        form.add(new JLabel("Check-out"));
        form.add(to);
        if (javax.swing.JOptionPane.showConfirmDialog(this, form, "Lodge availability", javax.swing.JOptionPane.OK_CANCEL_OPTION) != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }
        Ui.run(this, () -> service.availableForStay(Ui.date(from), Ui.date(to), null), result -> {
            StringBuilder text = new StringBuilder();
            for (Lodge room : (List<Lodge>) result) {
                text.append(room.getUnitCode()).append("  ").append(room.getRoomTypeName()).append("  ")
                        .append(CurrencyUtils.format(room.getPricePerNight())).append('\n');
            }
            if (text.isEmpty()) {
                text.append("No lodges are free for those dates.");
            }
            Ui.info(this, text.toString());
        });
    }
}
