package coralcove.ui.stays;

import coralcove.model.BookingSource;
import coralcove.model.Guest;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.model.Lodge;
import coralcove.service.GuestService;
import coralcove.service.LodgeService;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;

public final class StayDialog extends JDialog {
    private StayBooking result;

    private StayDialog(java.awt.Frame owner, StayBooking existing) {
        super(owner, existing == null ? "New reservation" : "Edit reservation", true);
        JTextField reference = new JTextField(existing == null ? "" : existing.getBookingReference(), 16);
        JComboBox<Guest> guest = new JComboBox<>();
        JComboBox<Lodge> room = new JComboBox<>();
        JSpinner in = Ui.dateSpinner(existing == null ? DateUtils.today() : existing.getCheckIn());
        JSpinner out = Ui.dateSpinner(existing == null ? DateUtils.today().plusDays(1) : existing.getCheckOut());
        JSpinner adults = Ui.numberSpinner(existing == null ? 1 : existing.getAdults(), 1, 12);
        JSpinner children = Ui.numberSpinner(existing == null ? 0 : existing.getChildren(), 0, 12);
        JTextField discount = new JTextField(existing == null ? "0.00" : existing.getDiscount().toPlainString(), 10);
        JComboBox<BookingSource> source = new JComboBox<>(BookingSource.values());
        JComboBox<StayStatus> status = new JComboBox<>(new StayStatus[] {
                StayStatus.PENDING, StayStatus.CONFIRMED
        });
        JTextField requests = new JTextField(existing == null ? "" : nullToEmpty(existing.getSpecialRequests()), 18);
        if (existing != null) {
            source.setSelectedItem(existing.getSource());
            if (existing.getStatus() == StayStatus.PENDING || existing.getStatus() == StayStatus.CONFIRMED) {
                status.setSelectedItem(existing.getStatus());
            }
        }
        Runnable reloadRooms = () -> Ui.run(this, () -> new LodgeService().availableForStay(Ui.date(in), Ui.date(out),
                existing == null ? null : existing.getId()), rows -> {
            room.removeAllItems();
            for (Lodge value : (List<Lodge>) rows) {
                room.addItem(value);
            }
            if (existing != null) {
                for (int i = 0; i < room.getItemCount(); i++) {
                    if (room.getItemAt(i).getId() == existing.getLodgeId()) {
                        room.setSelectedIndex(i);
                    }
                }
            }
        });
        in.addChangeListener(event -> reloadRooms.run());
        out.addChangeListener(event -> reloadRooms.run());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CONTENT);
        int rowIndex = 0;
        form.add(new JLabel("Reference"), Ui.constraints(rowIndex, 0));
        form.add(reference, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Guest"), Ui.constraints(rowIndex, 0));
        form.add(guest, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Check-in"), Ui.constraints(rowIndex, 0));
        form.add(in, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Check-out"), Ui.constraints(rowIndex, 0));
        form.add(out, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Lodge"), Ui.constraints(rowIndex, 0));
        form.add(room, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Adults"), Ui.constraints(rowIndex, 0));
        form.add(adults, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Children"), Ui.constraints(rowIndex, 0));
        form.add(children, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Discount"), Ui.constraints(rowIndex, 0));
        form.add(discount, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Source"), Ui.constraints(rowIndex, 0));
        form.add(source, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Status"), Ui.constraints(rowIndex, 0));
        form.add(status, Ui.constraints(rowIndex++, 1));
        form.add(new JLabel("Requests"), Ui.constraints(rowIndex, 0));
        form.add(requests, Ui.constraints(rowIndex, 1));
        var save = Ui.primary("Save");
        save.addActionListener(event -> {
            if (guest.getSelectedItem() == null || room.getSelectedItem() == null) {
                Ui.error(this, "Choose a guest and a free room.");
                return;
            }
            StayBooking reservation = existing == null ? new StayBooking() : existing;
            reservation.setBookingReference(reference.getText());
            reservation.setGuestId(((Guest) guest.getSelectedItem()).getId());
            reservation.setLodgeId(((Lodge) room.getSelectedItem()).getId());
            reservation.setCheckIn(Ui.date(in));
            reservation.setCheckOut(Ui.date(out));
            reservation.setAdults((Integer) adults.getValue());
            reservation.setChildren((Integer) children.getValue());
            reservation.setDiscount(Money.of(discount.getText()));
            reservation.setSource((BookingSource) source.getSelectedItem());
            reservation.setStatus((StayStatus) status.getSelectedItem());
            reservation.setSpecialRequests(requests.getText());
            result = reservation;
            dispose();
        });
        JPanel south = new JPanel();
        south.add(save);
        var cancel = Ui.secondary("Cancel");
        cancel.addActionListener(event -> dispose());
        south.add(cancel);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        Ui.run(this, () -> new GuestService().search(""), rows -> {
            for (Guest value : (List<Guest>) rows) {
                guest.addItem(value);
            }
            if (existing != null) {
                for (int i = 0; i < guest.getItemCount(); i++) {
                    if (guest.getItemAt(i).getId() == existing.getGuestId()) {
                        guest.setSelectedIndex(i);
                    }
                }
            }
            reloadRooms.run();
        });
        pack();
        setLocationRelativeTo(owner);
    }

    public static StayBooking show(java.awt.Frame owner, StayBooking existing) {
        StayDialog dialog = new StayDialog(owner, existing);
        dialog.setVisible(true);
        return dialog.result;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
