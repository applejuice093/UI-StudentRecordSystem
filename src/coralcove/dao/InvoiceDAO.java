package coralcove.dao;

import coralcove.model.Invoice;
import coralcove.model.InvoiceStatus;
import coralcove.model.Payment;
import coralcove.model.PaymentMethod;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class InvoiceDAO {

    public Invoice findByReservation(Connection connection, long reservationId) throws SQLException {
        try (var ps = connection.prepareStatement(SELECT + " WHERE i.reservation_id=? ORDER BY i.id DESC")) {
            ps.setLong(1, reservationId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Invoice findById(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement(SELECT + " WHERE i.id=?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Invoice> list(Connection connection, String guestText, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (guestText != null && !guestText.isBlank()) {
            sql.append(" AND g.full_name LIKE ?");
            args.add("%" + guestText.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND i.status=?");
            args.add(status);
        }
        sql.append(" ORDER BY i.id DESC");
        try (var ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setString(i + 1, args.get(i));
            }
            try (var rs = ps.executeQuery()) {
                List<Invoice> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public long insert(Connection connection, Invoice invoice) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO invoices(invoice_number, reservation_id, guest_id, subtotal, discount, tax, total,
                    amount_paid, balance, status, issued_at, is_sample)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, invoice.getInvoiceNumber());
            ps.setLong(2, invoice.getReservationId());
            ps.setLong(3, invoice.getGuestId());
            Jdbc.money(ps, 4, invoice.getSubtotal());
            Jdbc.money(ps, 5, invoice.getDiscount());
            Jdbc.money(ps, 6, invoice.getTax());
            Jdbc.money(ps, 7, invoice.getTotal());
            Jdbc.money(ps, 8, invoice.getAmountPaid());
            Jdbc.money(ps, 9, invoice.getBalance());
            ps.setString(10, invoice.getStatus().name());
            ps.setString(11, invoice.getIssuedAt());
            ps.setInt(12, invoice.isSample() ? 1 : 0);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updateTotals(Connection connection, Invoice invoice) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE invoices SET subtotal=?, discount=?, tax=?, total=?, amount_paid=?, balance=?, status=?
                WHERE id=?
                """)) {
            Jdbc.money(ps, 1, invoice.getSubtotal());
            Jdbc.money(ps, 2, invoice.getDiscount());
            Jdbc.money(ps, 3, invoice.getTax());
            Jdbc.money(ps, 4, invoice.getTotal());
            Jdbc.money(ps, 5, invoice.getAmountPaid());
            Jdbc.money(ps, 6, invoice.getBalance());
            ps.setString(7, invoice.getStatus().name());
            ps.setLong(8, invoice.getId());
            ps.executeUpdate();
        }
    }

    public long addItem(Connection connection, long invoiceId, String kind, String description, int qty,
            BigDecimal unit, BigDecimal line) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO invoice_items(invoice_id, kind, description, qty, unit_price, line_total)
                VALUES (?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, invoiceId);
            ps.setString(2, kind);
            ps.setString(3, description);
            ps.setInt(4, qty);
            Jdbc.money(ps, 5, unit);
            Jdbc.money(ps, 6, line);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<String[]> items(Connection connection, long invoiceId) throws SQLException {
        try (var ps = connection.prepareStatement(
                "SELECT kind, description, qty, unit_price, line_total FROM invoice_items WHERE invoice_id=? ORDER BY id")) {
            ps.setLong(1, invoiceId);
            try (var rs = ps.executeQuery()) {
                List<String[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new String[] {
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)
                    });
                }
                return rows;
            }
        }
    }

    public BigDecimal sumItems(Connection connection, long invoiceId) throws SQLException {
        try (var ps = connection.prepareStatement(
                "SELECT line_total FROM invoice_items WHERE invoice_id=?")) {
            ps.setLong(1, invoiceId);
            BigDecimal sum = MoneyZero();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    sum = sum.add(Jdbc.money(rs, "line_total"));
                }
            }
            return sum;
        }
    }

    public boolean numberExists(Connection connection, String number) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM invoices WHERE invoice_number=?")) {
            ps.setString(1, number);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public long addPayment(Connection connection, Payment payment) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO payments(invoice_id, amount, method, reference, paid_at, received_by, note)
                VALUES (?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, payment.getInvoiceId());
            Jdbc.money(ps, 2, payment.getAmount());
            ps.setString(3, payment.getMethod().name());
            ps.setString(4, payment.getReference());
            ps.setString(5, payment.getPaidAt());
            Jdbc.nullableLong(ps, 6, payment.getReceivedBy());
            ps.setString(7, payment.getNote());
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<Payment> payments(Connection connection, long invoiceId) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM payments WHERE invoice_id=? ORDER BY id")) {
            ps.setLong(1, invoiceId);
            try (var rs = ps.executeQuery()) {
                List<Payment> rows = new ArrayList<>();
                while (rs.next()) {
                    Payment payment = new Payment();
                    payment.setId(rs.getLong("id"));
                    payment.setInvoiceId(rs.getLong("invoice_id"));
                    payment.setAmount(Jdbc.money(rs, "amount"));
                    payment.setMethod(PaymentMethod.parse(rs.getString("method")));
                    payment.setReference(rs.getString("reference"));
                    payment.setPaidAt(rs.getString("paid_at"));
                    payment.setReceivedBy(Jdbc.longOrNull(rs, "received_by"));
                    payment.setNote(rs.getString("note"));
                    rows.add(payment);
                }
                return rows;
            }
        }
    }

    public BigDecimal sumPayments(Connection connection, long invoiceId) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT amount FROM payments WHERE invoice_id=?")) {
            ps.setLong(1, invoiceId);
            BigDecimal sum = MoneyZero();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    sum = sum.add(Jdbc.money(rs, "amount"));
                }
            }
            return sum;
        }
    }

    public boolean paymentReferenceExists(Connection connection, String reference) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM payments WHERE reference=?")) {
            ps.setString(1, reference);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static final String SELECT = """
            SELECT i.*, g.full_name AS guest_name FROM invoices i
            JOIN guests g ON g.id = i.guest_id
            """;

    private Invoice map(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getLong("id"));
        invoice.setInvoiceNumber(rs.getString("invoice_number"));
        invoice.setReservationId(rs.getLong("reservation_id"));
        invoice.setGuestId(rs.getLong("guest_id"));
        invoice.setGuestName(rs.getString("guest_name"));
        invoice.setSubtotal(Jdbc.money(rs, "subtotal"));
        invoice.setDiscount(Jdbc.money(rs, "discount"));
        invoice.setTax(Jdbc.money(rs, "tax"));
        invoice.setTotal(Jdbc.money(rs, "total"));
        invoice.setAmountPaid(Jdbc.money(rs, "amount_paid"));
        invoice.setBalance(Jdbc.money(rs, "balance"));
        invoice.setStatus(InvoiceStatus.parse(rs.getString("status")));
        invoice.setIssuedAt(rs.getString("issued_at"));
        invoice.setSample(rs.getInt("is_sample") == 1);
        return invoice;
    }

    private BigDecimal MoneyZero() {
        return coralcove.util.Money.zero();
    }
}
