package coralcove.model;

public enum PaymentMethod {
    CASH, CARD, UPI, BANK_TRANSFER;

    public String label() {
        return name().replace('_', ' ');
    }

    public static PaymentMethod parse(String raw) {
        return PaymentMethod.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
    }
}
