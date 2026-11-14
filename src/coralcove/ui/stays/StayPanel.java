package coralcove.ui.stays;

import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.service.StayService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import coralcove.util.PDFGenerator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class StayPanel extends JPanel implements Refreshable {
    private final StayService service = new StayService();
    private final JTextField reference = new JTextField(10);
    private final JTextField guest = new JTextField(12);
    private final JComboBox<String> status = new JComboBox<>();
    private final DataTable table = new DataTable(
            "Id", "Reference", "Guest", "Lodge", "Check-in", "Check-out", "Nights", "Total", "Status", "Sample");
    private List<StayBooking> rows = List.of();

    public StayPanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        status.addItem("All statuses");
        for (StayStatus value : StayStatus.values()) {
            status.addItem(value.name());
        }
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.setOpaque(false);
        filters.add(reference);
        filters.add(guest);
        filters.add(status);
        var search = Ui.secondary("Search");
        search.addActionListener(event -> refresh());
        var create = Ui.primary("New reservation");
        create.addActionListener(event -> edit(null));
        var edit = Ui.secondary("Edit");
        edit.addActionListener(event -> {
            StayBooking reservation = selected();
            if (reservation != null) {
                edit(reservation);
            }
        });
        var cancel = Ui.secondary("Cancel booking");
        cancel.addActionListener(event -> cancelStay());
        var print = Ui.secondary("Print confirmation");
        print.addActionListener(event -> printSelected(false));
        var pdf = Ui.secondary("Save PDF");
        pdf.addActionListener(event -> printSelected(true));
        filters.add(search);
        filters.add(create);
        filters.add(edit);
        filters.add(cancel);
        filters.add(print);
        filters.add(pdf);
        add(filters, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        String statusValue = "All statuses".equals(status.getSelectedItem()) ? null : status.getSelectedItem().toString();
        Ui.run(this, () -> service.search(reference.getText(), guest.getText(), null, null, statusValue), result -> {
            rows = (List<StayBooking>) result;
            List<String[]> data = new ArrayList<>();
            for (StayBooking reservation : rows) {
                data.add(new String[] {
                        String.valueOf(reservation.getId()), reservation.getBookingReference(), reservation.getGuestName(),
                        reservation.getUnitCode(), DateUtils.display(reservation.getCheckIn()),
                        DateUtils.display(reservation.getCheckOut()), String.valueOf(reservation.getNights()),
                        CurrencyUtils.format(reservation.getTotal()), reservation.getStatus().name(),
                        reservation.isSample() ? "Sample" : ""
                });
            }
            table.setRows(data);
        });
    }

    private StayBooking selected() {
        int row = table.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select a reservation first.");
            return null;
        }
        return rows.get(row);
    }

    private void edit(StayBooking existing) {
        StayBooking draft = StayDialog.show((Frame) SwingUtilities.getWindowAncestor(this), existing);
        if (draft == null) {
            return;
        }
        Ui.run(this, () -> existing == null ? service.create(draft) : service.update(draft), result -> {
            StayBooking saved = (StayBooking) result;
            Ui.info(this, "Saved " + saved.getBookingReference() + ". Total " + CurrencyUtils.format(saved.getTotal())
                    + " for " + saved.getNights() + " night(s).");
            refresh();
        });
    }

    private void cancelStay() {
        StayBooking reservation = selected();
        if (reservation == null || !Ui.confirm(this, "Cancel " + reservation.getBookingReference() + "?")) {
            return;
        }
        Ui.run(this, () -> {
            service.cancel(reservation.getId());
            return null;
        }, result -> refresh());
    }

    private void printSelected(boolean pdf) {
        StayBooking reservation = selected();
        if (reservation == null) {
            return;
        }
        List<String> lines = List.of(
                "Booking " + reservation.getBookingReference() + (reservation.isSample() ? " (demonstration)" : ""),
                "Guest: " + reservation.getGuestName(),
                "Lodge: " + reservation.getUnitCode(),
                "Check-in: " + DateUtils.display(reservation.getCheckIn()),
                "Check-out: " + DateUtils.display(reservation.getCheckOut()),
                "Nights: " + reservation.getNights(),
                "Source: " + reservation.getSource().label(),
                "Status: " + reservation.getStatus().name(),
                "Total: " + CurrencyUtils.formatPlain(reservation.getTotal()),
                "Requests: " + (reservation.getSpecialRequests() == null ? "" : reservation.getSpecialRequests())
        );
        if (pdf) {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File(reservation.getBookingReference() + ".pdf"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                PDFGenerator.write(Path.of(chooser.getSelectedFile().getAbsolutePath()), "CoralCove stay", lines);
                Ui.info(this, "Saved the confirmation PDF.");
            }
        } else {
            PDFGenerator.print("CoralCove stay", lines);
        }
    }
}
