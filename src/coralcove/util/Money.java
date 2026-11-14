package coralcove.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money is stored as a scale-2 decimal string and calculated with BigDecimal. */
public final class Money {

    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    private Money() {
    }

    public static BigDecimal zero() {
        return new BigDecimal("0.00");
    }

    public static BigDecimal of(String raw) {
        if (raw == null || raw.isBlank()) {
            return zero();
        }
        try {
            return new BigDecimal(raw.trim()).setScale(2, ROUND);
        } catch (NumberFormatException ex) {
            throw new AppException("Enter a valid amount.");
        }
    }

    public static BigDecimal of(double ignored) {
        throw new UnsupportedOperationException("Use BigDecimal for money");
    }

    public static String store(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, ROUND).toPlainString();
    }

    public static BigDecimal parse(String stored) {
        if (stored == null || stored.isBlank()) {
            return zero();
        }
        return new BigDecimal(stored).setScale(2, ROUND);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return parse(store(a)).add(parse(store(b))).setScale(2, ROUND);
    }

    public static BigDecimal multiply(BigDecimal amount, int qty) {
        if (qty < 0) {
            throw new AppException("Quantity cannot be negative.");
        }
        return amount.multiply(BigDecimal.valueOf(qty)).setScale(2, ROUND);
    }

    public static BigDecimal percent(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent).divide(new BigDecimal("100"), 2, ROUND);
    }

    public static void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new AppException(field + " cannot be negative.");
        }
    }
}
