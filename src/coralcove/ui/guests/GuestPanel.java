package coralcove.ui.guests;

import coralcove.model.Guest;
import coralcove.model.StayBooking;
import coralcove.service.GuestService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class GuestPanel extends JPanel implements Refreshable {
    private final GuestService service = new GuestService();
    private final JTextField search = new JTextField(16);
    private final DataTable table = new DataTable("Id", "Name", "Phone", "Email", "Class", "Sample");
    private final DataTable history = new DataTable("Reference", "Lodge", "Check-in", "Check-out", "Status", "Total");
    private List<Guest> guests = List.of();

    public GuestPanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(search);
        var find = Ui.secondary("Search");
        find.addActionListener(event -> refresh());
        var add = Ui.primary("Register");
        add.addActionListener(event -> edit(null));
        var edit = Ui.secondary("Edit");
        edit.addActionListener(event -> {
            Guest guest = selected();
            if (guest != null) {
                edit(guest);
            }
        });
        var delete = Ui.secondary("Delete");
        delete.addActionListener(event -> remove());
        var vip = Ui.secondary("Recalculate VIP");
        vip.addActionListener(event -> vip());
        north.add(find);
        north.add(add);
        north.add(edit);
        north.add(delete);
        north.add(vip);
        add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        center.setOpaque(false);
        center.add(table);
        center.add(history);
        add(center, BorderLayout.CENTER);
        table.table().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadHistory();
            }
        });
    }

    @Override
    public void refresh() {
        Ui.run(this, () -> service.search(search.getText()), result -> {
            guests = (List<Guest>) result;
            List<String[]> rows = new ArrayList<>();
            for (Guest guest : guests) {
                rows.add(new String[] {
                        String.valueOf(guest.getId()), guest.getFullName(), guest.getPhone(), guest.getEmail(),
                        guest.getVip(), guest.isSample() ? "Sample" : ""
                });
            }
            table.setRows(rows);
            history.setRows(List.of());
        });
    }

    private void loadHistory() {
        Guest guest = selectedSilent();
        if (guest == null) {
            return;
        }
        Ui.run(this, () -> service.history(guest.getId()), result -> {
            GuestService.GuestHistory data = (GuestService.GuestHistory) result;
            List<String[]> rows = new ArrayList<>();
            for (StayBooking stay : data.stays()) {
                rows.add(new String[] {
                        stay.getBookingReference(), stay.getUnitCode(), DateUtils.display(stay.getCheckIn()),
                        DateUtils.display(stay.getCheckOut()), stay.getStatus().name(), CurrencyUtils.format(stay.getTotal())
                });
            }
            rows.add(new String[] {"", "", "", "Visits " + data.visits(), "Spent", CurrencyUtils.format(data.spent())});
            history.setRows(rows);
        });
    }

    private Guest selectedSilent() {
        int row = table.selectedRow();
        return row < 0 ? null : guests.get(row);
    }

    private Guest selected() {
        Guest guest = selectedSilent();
        if (guest == null) {
            Ui.error(this, "Select a guest first.");
        }
        return guest;
    }

    private void edit(Guest existing) {
        Guest draft = GuestDialog.show((Frame) SwingUtilities.getWindowAncestor(this), existing);
        if (draft == null) {
            return;
        }
        Ui.run(this, () -> service.save(draft, existing == null), result -> refresh());
    }

    private void remove() {
        Guest guest = selected();
        if (guest == null || !Ui.confirm(this, "Delete " + guest.getFullName() + "?")) {
            return;
        }
        Ui.run(this, () -> {
            service.delete(guest.getId());
            return null;
        }, result -> refresh());
    }

    private void vip() {
        Guest guest = selected();
        if (guest == null) {
            return;
        }
        Ui.run(this, () -> {
            service.applyVipRule(guest.getId());
            return null;
        }, result -> {
            Ui.info(this, "VIP is applied when stays are at least 3 or spend is at least ₹50,000.");
            refresh();
        });
    }
}
