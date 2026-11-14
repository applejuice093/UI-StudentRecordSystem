package coralcove.ui.arrivals;

import coralcove.model.Invoice;
import coralcove.model.StayBooking;
import coralcove.service.BillingService;
import coralcove.service.ArrivalService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import coralcove.util.PDFGenerator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ArrivalPanel extends JPanel implements Refreshable {
    private final ArrivalService stays = new ArrivalService();
    private final BillingService billing = new BillingService();
    private final JTextField reference = new JTextField(16);
    private final JLabel summary = new JLabel("Search a booking reference.");
    private final DataTable charges = new DataTable("Kind", "Description", "Qty", "Price", "Total");
    private StayBooking current;

    public ArrivalPanel() {
        super(new BorderLayout(8, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(new JLabel("Booking reference"));
        north.add(reference);
        var find = Ui.secondary("Find");
        find.addActionListener(event -> find());
        var in = Ui.primary("Check in");
        in.addActionListener(event -> act(true));
        var out = Ui.primary("Check out");
        out.addActionListener(event -> act(false));
        var bill = Ui.secondary("Print bill");
        bill.addActionListener(event -> printBill());
        north.add(find);
        north.add(in);
        north.add(out);
        north.add(bill);
        add(north, BorderLayout.NORTH);
        summary.setFont(Theme.BODY);
        add(summary, BorderLayout.SOUTH);
        add(charges, BorderLayout.CENTER);
    }

    private void find() {
        Ui.run(this, () -> stays.findByReference(reference.getText()), result -> show((StayBooking) result));
    }

    private void show(StayBooking reservation) {
        current = reservation;
        summary.setText(reservation.getBookingReference()
                + (reservation.isSample() ? " · Sample" : "")
                + " · " + reservation.getGuestName()
                + " · Lodge " + reservation.getUnitCode()
                + " · " + DateUtils.display(reservation.getCheckIn())
                + " to " + DateUtils.display(reservation.getCheckOut())
                + " · " + reservation.getStatus().name()
                + " · " + CurrencyUtils.format(reservation.getTotal()));
        Ui.run(this, () -> {
            Invoice invoice = billing.findForReservation(reservation.getId());
            return invoice == null ? List.of() : billing.items(invoice.getId());
        }, result -> {
            List<String[]> rows = new ArrayList<>();
            for (String[] item : (List<String[]>) result) {
                rows.add(item);
            }
            charges.setRows(rows);
        });
    }

    private void act(boolean checkIn) {
        if (current == null) {
            Ui.error(this, "Find a reservation first.");
            return;
        }
        String verb = checkIn ? "Check in " : "Check out ";
        if (!Ui.confirm(this, verb + current.getBookingReference() + "?")) {
            return;
        }
        Ui.run(this, () -> checkIn ? stays.checkIn(current.getId()) : stays.checkOut(current.getId()), result -> {
            StayBooking reservation = (StayBooking) result;
            Ui.info(this, checkIn
                    ? "Checked in. The room is now occupied."
                    : "Checked out. The room is waiting for housekeeping and is not available until it is cleaned.");
            show(reservation);
        });
    }

    private void printBill() {
        if (current == null) {
            return;
        }
        Ui.run(this, () -> billing.findForReservation(current.getId()), result -> {
            Invoice invoice = (Invoice) result;
            if (invoice == null) {
                Ui.error(this, "There is no invoice yet. Check the guest in first.");
                return;
            }
            List<String> lines = new ArrayList<>();
            lines.add("Invoice " + invoice.getInvoiceNumber() + (invoice.isSample() ? " (demonstration)" : ""));
            lines.add("Guest: " + invoice.getGuestName());
            lines.add("Status: " + invoice.getStatus().name());
            for (String[] item : billing.items(invoice.getId())) {
                lines.add(item[0] + " | " + item[1] + " | " + item[2] + " x " + item[3] + " = " + item[4]);
            }
            lines.add("Subtotal: " + CurrencyUtils.formatPlain(invoice.getSubtotal()));
            lines.add("Discount: " + CurrencyUtils.formatPlain(invoice.getDiscount()));
            lines.add("Tax: " + CurrencyUtils.formatPlain(invoice.getTax()));
            lines.add("Total: " + CurrencyUtils.formatPlain(invoice.getTotal()));
            lines.add("Paid: " + CurrencyUtils.formatPlain(invoice.getAmountPaid()));
            lines.add("Balance: " + CurrencyUtils.formatPlain(invoice.getBalance()));
            PDFGenerator.print("CoralCove invoice", lines);
        });
    }

    @Override
    public void refresh() {
        if (current != null) {
            find();
        }
    }
}
