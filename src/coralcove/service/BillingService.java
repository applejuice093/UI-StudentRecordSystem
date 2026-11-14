package coralcove.service;

import coralcove.dao.AuditDAO;
import coralcove.dao.InvoiceDAO;
import coralcove.dao.StayBookingDAO;
import coralcove.dao.ExperienceDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.Invoice;
import coralcove.model.InvoiceStatus;
import coralcove.model.Payment;
import coralcove.model.PaymentMethod;
import coralcove.model.StayBooking;
import coralcove.util.AppException;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import coralcove.util.ValidationUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class BillingService {

    private final InvoiceDAO invoices = new InvoiceDAO();
    private final AuditDAO audit = new AuditDAO();

    public record Quote(BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total) {
    }

    public Quote quote(BigDecimal roomSubtotal, BigDecimal discount) {
        Money.requireNonNegative(roomSubtotal, "Lodge charge");
        if (discount == null) {
            discount = Money.zero();
        }
        Money.requireNonNegative(discount, "Discount");
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(roomSubtotal) > 0) {
            throw new AppException("Discount cannot exceed the pre-tax amount.");
        }
        BigDecimal taxable = roomSubtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = Money.percent(taxable, SettingsService.taxPercent());
        BigDecimal total = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);
        return new Quote(roomSubtotal.setScale(2, RoundingMode.HALF_UP), discount, tax, total);
    }

    public Invoice ensureForReservation(Connection connection, StayBooking reservation) throws SQLException {
        Invoice existing = invoices.findByReservation(connection, reservation.getId());
        if (existing != null) {
            return existing;
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextNumber(connection));
        invoice.setReservationId(reservation.getId());
        invoice.setGuestId(reservation.getGuestId());
        invoice.setDiscount(reservation.getDiscount());
        invoice.setIssuedAt(DateUtils.nowStamp());
        invoice.setSample(reservation.isSample());
        invoice.setSubtotal(Money.zero());
        invoice.setTax(Money.zero());
        invoice.setTotal(Money.zero());
        invoice.setAmountPaid(Money.zero());
        invoice.setBalance(Money.zero());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setId(invoices.insert(connection, invoice));
        BigDecimal line = Money.multiply(reservation.getRoomPrice(), reservation.getNights());
        invoices.addItem(connection, invoice.getId(), "ROOM",
                "Lodge " + reservation.getUnitCode() + " × " + reservation.getNights() + " night(s)",
                reservation.getNights(), reservation.getRoomPrice(), line);
        recalc(connection, invoice.getId());
        return invoices.findById(connection, invoice.getId());
    }

    public void addCharge(long invoiceId, String kind, String description, int qty, BigDecimal unitPrice) {
        AuthorizationService.require(coralcove.model.AppModule.BILLING);
        ValidationUtils.required(description, "Description");
        if (qty <= 0) {
            throw new AppException("Quantity must be greater than zero.");
        }
        Money.requireNonNegative(unitPrice, "Price");
        if (unitPrice.signum() == 0) {
            throw new AppException("Price must be greater than zero.");
        }
        Transactions.in(connection -> {
            invoices.addItem(connection, invoiceId, kind, description.trim(), qty, unitPrice,
                    Money.multiply(unitPrice, qty));
            recalc(connection, invoiceId);
            return null;
        });
        Session.dataChanged();
    }

    public void setDiscount(long invoiceId, BigDecimal discount) {
        AuthorizationService.require(coralcove.model.AppModule.BILLING);
        Transactions.in(connection -> {
            Invoice invoice = invoices.findById(connection, invoiceId);
            invoice.setDiscount(discount == null ? Money.zero() : discount);
            invoices.updateTotals(connection, invoice);
            recalc(connection, invoiceId);
            return null;
        });
        Session.dataChanged();
    }

    public Payment takePayment(long invoiceId, BigDecimal amount, PaymentMethod method, String reference, boolean refund) {
        AuthorizationService.require(coralcove.model.AppModule.BILLING);
        if (amount == null || amount.signum() <= 0) {
            throw new AppException("Amount must be greater than zero.");
        }
        String ref = ValidationUtils.required(reference, "Payment reference");
        Payment saved = Transactions.in(connection -> {
            if (invoices.paymentReferenceExists(connection, ref)) {
                throw new AppException("Payment reference " + ref + " already exists.");
            }
            Invoice invoice = invoices.findById(connection, invoiceId);
            BigDecimal paid = invoices.sumPayments(connection, invoiceId);
            Payment payment = new Payment();
            payment.setInvoiceId(invoiceId);
            payment.setMethod(method);
            payment.setReference(ref);
            payment.setPaidAt(DateUtils.nowStamp());
            payment.setReceivedBy(Session.get() == null ? null : Session.get().getId());
            if (refund) {
                if (amount.compareTo(paid) > 0) {
                    throw new AppException("Refund cannot exceed the amount already paid.");
                }
                payment.setAmount(amount.negate());
                payment.setNote("Refund");
            } else {
                BigDecimal balance = invoice.getTotal().subtract(paid);
                if (amount.compareTo(balance) > 0) {
                    throw new AppException("Payment exceeds the remaining balance of "
                            + CurrencyUtils.format(balance) + ".");
                }
                payment.setAmount(amount);
                payment.setNote("Payment");
            }
            payment.setId(invoices.addPayment(connection, payment));
            recalc(connection, invoiceId);
            audit.log(connection, Session.get() == null ? null : Session.get().getId(),
                    refund ? "REFUND" : "PAYMENT", "payments", payment.getReference(),
                    Money.store(payment.getAmount()));
            return payment;
        });
        Session.dataChanged();
        return saved;
    }

    public void attachServedOrders(Connection connection, long reservationId) throws SQLException {
        Invoice invoice = invoices.findByReservation(connection, reservationId);
        if (invoice == null) {
            return;
        }
        ExperienceDAO orders = new ExperienceDAO();
        for (var order : orders.unbilledServed(connection, reservationId)) {
            String kind = "RESTAURANT".equals(order.getOrderType()) ? "RESTAURANT" : "MISC";
            long itemId = invoices.addItem(connection, invoice.getId(), kind, order.getDescription(), order.getQty(),
                    order.getUnitPrice(), order.getLineTotal());
            orders.updateStatus(connection, order.getId(), "SERVED", itemId);
        }
        recalc(connection, invoice.getId());
    }

    public Invoice findForReservation(long reservationId) {
        AuthorizationService.require(coralcove.model.AppModule.BILLING);
        try (var connection = DatabaseManager.getConnection()) {
            return invoices.findByReservation(connection, reservationId);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<Invoice> list(String guest, String status) {
        AuthorizationService.require(coralcove.model.AppModule.BILLING);
        try (var connection = DatabaseManager.getConnection()) {
            return invoices.list(connection, guest, status);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<String[]> items(long invoiceId) {
        AuthorizationService.require(coralcove.model.AppModule.BILLING);
        try (var connection = DatabaseManager.getConnection()) {
            return invoices.items(connection, invoiceId);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public List<Payment> payments(long invoiceId) {
        AuthorizationService.require(coralcove.model.AppModule.BILLING);
        try (var connection = DatabaseManager.getConnection()) {
            return invoices.payments(connection, invoiceId);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void recalc(Connection connection, long invoiceId) throws SQLException {
        Invoice invoice = invoices.findById(connection, invoiceId);
        BigDecimal subtotal = invoices.sumItems(connection, invoiceId).setScale(2, RoundingMode.HALF_UP);
        Quote quote = quote(subtotal, invoice.getDiscount());
        BigDecimal paid = invoices.sumPayments(connection, invoiceId).setScale(2, RoundingMode.HALF_UP);
        invoice.setSubtotal(quote.subtotal());
        invoice.setDiscount(quote.discount());
        invoice.setTax(quote.tax());
        invoice.setTotal(quote.total());
        invoice.setAmountPaid(paid);
        invoice.setBalance(quote.total().subtract(paid).setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus(statusFor(connection, invoiceId, quote.total(), paid));
        invoices.updateTotals(connection, invoice);
    }

    private InvoiceStatus statusFor(Connection connection, long invoiceId, BigDecimal total, BigDecimal paid)
            throws SQLException {
        boolean refunded = false;
        for (Payment payment : invoices.payments(connection, invoiceId)) {
            if (payment.getAmount().signum() < 0) {
                refunded = true;
            }
        }
        if (paid.compareTo(total) == 0 && total.signum() > 0) {
            return InvoiceStatus.PAID;
        }
        if (paid.signum() == 0 && refunded) {
            return InvoiceStatus.REFUNDED;
        }
        if (paid.signum() == 0) {
            return InvoiceStatus.UNPAID;
        }
        return InvoiceStatus.PARTIALLY_PAID;
    }

    private String nextNumber(Connection connection) throws SQLException {
        String number;
        do {
            number = "INV-" + DateUtils.today().toString().replace("-", "") + "-"
                    + (1000 + ThreadLocalRandom.current().nextInt(9000));
        } while (invoices.numberExists(connection, number));
        return number;
    }

    /** Used by reservation pricing before an invoice exists. */
    public StayBooking priceStay(StayBooking reservation) {
        new StayBookingDAO();
        int nights = DateUtils.nights(reservation.getCheckIn(), reservation.getCheckOut());
        BigDecimal room = Money.multiply(reservation.getRoomPrice(), nights);
        Quote quote = quote(room, reservation.getDiscount());
        reservation.setNights(nights);
        reservation.setTax(quote.tax());
        reservation.setTotal(quote.total());
        reservation.setDiscount(quote.discount());
        return reservation;
    }
}
