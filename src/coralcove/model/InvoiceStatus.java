package coralcove.model;

public enum InvoiceStatus {
    UNPAID, PARTIALLY_PAID, PAID, REFUNDED;

    public static InvoiceStatus parse(String raw) {
        return InvoiceStatus.valueOf(raw.trim().toUpperCase());
    }
}
