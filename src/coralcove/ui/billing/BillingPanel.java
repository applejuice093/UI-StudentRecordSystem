package coralcove.ui.billing;

import coralcove.model.Invoice;
import coralcove.model.Payment;
import coralcove.model.PaymentMethod;
import coralcove.service.BillingService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CurrencyUtils;
import coralcove.util.Money;
import coralcove.util.PDFGenerator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BillingPanel extends JPanel implements Refreshable {
    private final BillingService billing = new BillingService();
    private final JTextField guest = new JTextField(12);
    private final JComboBox<String> status = new JComboBox<>(new String[] {"", "UNPAID", "PARTIALLY_PAID", "PAID", "REFUNDED"});
    private final DataTable invoices = new DataTable("Id", "Number", "Guest", "Total", "Paid", "Balance", "Status", "Sample");
    private final DataTable items = new DataTable("Kind", "Description", "Qty", "Price", "Line");
    private final DataTable payments = new DataTable("When", "Method", "Reference", "Amount", "Note");
    private List<Invoice> rows = List.of();

    public BillingPanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(new JLabel("Guest"));
        north.add(guest);
        north.add(status);
        var search = Ui.secondary("Search");
        search.addActionListener(event -> refresh());
        north.add(search);
        add(north, BorderLayout.NORTH);
        JPanel tables = new JPanel(new GridLayout(1, 2, 8, 0));
        tables.setOpaque(false);
        tables.add(invoices);
        JPanel detail = new JPanel(new GridLayout(2, 1, 0, 8));
        detail.setOpaque(false);
        detail.add(items);
        detail.add(payments);
        tables.add(detail);
        add(tables, BorderLayout.CENTER);
        invoices.table().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadDetail();
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        JComboBox<String> kind = new JComboBox<>(new String[] {"RESTAURANT", "LAUNDRY", "MINIBAR", "MISC"});
        JTextField description = new JTextField(12);
        JTextField qty = new JTextField("1", 3);
        JTextField price = new JTextField(6);
        var add = Ui.primary("Add charge");
        add.addActionListener(event -> addCharge(kind, description, qty, price));
        JTextField amount = new JTextField(6);
        JTextField reference = new JTextField(8);
        JComboBox<PaymentMethod> method = new JComboBox<>(PaymentMethod.values());
        var pay = Ui.primary("Take payment");
        pay.addActionListener(event -> pay(amount, reference, method, false));
        var refund = Ui.secondary("Refund");
        refund.addActionListener(event -> pay(amount, reference, method, true));
        var print = Ui.secondary("Print invoice");
        print.addActionListener(event -> printInvoice());
        actions.add(kind);
        actions.add(description);
        actions.add(qty);
        actions.add(price);
        actions.add(add);
        actions.add(amount);
        actions.add(method);
        actions.add(reference);
        actions.add(pay);
        actions.add(refund);
        actions.add(print);
        add(actions, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        Ui.run(this, () -> billing.list(guest.getText(), (String) status.getSelectedItem()), result -> {
            rows = (List<Invoice>) result;
            List<String[]> data = new ArrayList<>();
            for (Invoice invoice : rows) {
                data.add(new String[] {
                        String.valueOf(invoice.getId()), invoice.getInvoiceNumber(), invoice.getGuestName(),
                        CurrencyUtils.format(invoice.getTotal()), CurrencyUtils.format(invoice.getAmountPaid()),
                        CurrencyUtils.format(invoice.getBalance()), invoice.getStatus().name(),
                        invoice.isSample() ? "Sample" : ""
                });
            }
            invoices.setRows(data);
        });
    }

    private Invoice selected() {
        int row = invoices.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select an invoice first.");
            return null;
        }
        return rows.get(row);
    }

    private void loadDetail() {
        int row = invoices.selectedRow();
        if (row < 0) {
            return;
        }
        Invoice invoice = rows.get(row);
        Ui.run(this, () -> new Object[] {billing.items(invoice.getId()), billing.payments(invoice.getId())}, result -> {
            Object[] pair = (Object[]) result;
            items.setRows((List<String[]>) pair[0]);
            List<String[]> paid = new ArrayList<>();
            for (Payment payment : (List<Payment>) pair[1]) {
                paid.add(new String[] {
                        payment.getPaidAt(), payment.getMethod().label(), payment.getReference(),
                        CurrencyUtils.format(payment.getAmount()), payment.getNote()
                });
            }
            payments.setRows(paid);
        });
    }

    private void addCharge(JComboBox<String> kind, JTextField description, JTextField qty, JTextField price) {
        Invoice invoice = selected();
        if (invoice == null) {
            return;
        }
        Ui.run(this, () -> {
            billing.addCharge(invoice.getId(), (String) kind.getSelectedItem(), description.getText(),
                    Integer.parseInt(qty.getText().trim()), Money.of(price.getText()));
            return null;
        }, result -> {
            refresh();
            Ui.info(this, "Charge added. Tax is recalculated from the resort tax percent.");
        });
    }

    private void pay(JTextField amount, JTextField reference, JComboBox<PaymentMethod> method, boolean refund) {
        Invoice invoice = selected();
        if (invoice == null) {
            return;
        }
        Ui.run(this, () -> {
            billing.takePayment(invoice.getId(), Money.of(amount.getText()), (PaymentMethod) method.getSelectedItem(),
                    reference.getText(), refund);
            return null;
        }, result -> refresh());
    }

    private void printInvoice() {
        Invoice invoice = selected();
        if (invoice == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add(invoice.getInvoiceNumber() + (invoice.isSample() ? " DEMONSTRATION" : ""));
        lines.add("Guest: " + invoice.getGuestName());
        for (String[] item : billing.items(invoice.getId())) {
            lines.add(String.join(" | ", item));
        }
        lines.add("Total " + CurrencyUtils.formatPlain(invoice.getTotal()));
        lines.add("Paid " + CurrencyUtils.formatPlain(invoice.getAmountPaid()));
        lines.add("Balance " + CurrencyUtils.formatPlain(invoice.getBalance()));
        PDFGenerator.print("CoralCove invoice", lines);
    }
}
