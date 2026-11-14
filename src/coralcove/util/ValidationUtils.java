package coralcove.util;

import java.math.BigDecimal;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new AppException(field + " is required.");
        }
        return value.trim();
    }

    public static String optional(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static void maxLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new AppException(field + " must be at most " + max + " characters.");
        }
    }

    public static void username(String value) {
        String name = required(value, "Username");
        maxLength(name, 30, "Username");
        if (name.length() < 3 || !name.matches("[A-Za-z0-9._-]+")) {
            throw new AppException("Username must be 3-30 letters, digits, dot, underscore, or hyphen.");
        }
    }

    public static void password(String value) {
        if (value == null || value.length() < 8) {
            throw new AppException("Password must be at least 8 characters.");
        }
        boolean letter = false;
        boolean digit = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            letter |= Character.isLetter(c);
            digit |= Character.isDigit(c);
        }
        if (!letter || !digit) {
            throw new AppException("Password must include a letter and a digit.");
        }
    }

    public static void email(String value, boolean required) {
        String email = value == null ? "" : value.trim();
        if (email.isEmpty()) {
            if (required) {
                throw new AppException("Email is required.");
            }
            return;
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new AppException("Enter a valid email address.");
        }
    }

    public static int positiveInt(String raw, String field) {
        try {
            int n = Integer.parseInt(required(raw, field));
            if (n <= 0) {
                throw new AppException(field + " must be greater than zero.");
            }
            return n;
        } catch (NumberFormatException ex) {
            throw new AppException(field + " must be a whole number.");
        }
    }

    public static int nonNegativeInt(String raw, String field) {
        try {
            int n = Integer.parseInt(required(raw, field));
            if (n < 0) {
                throw new AppException(field + " cannot be negative.");
            }
            return n;
        } catch (NumberFormatException ex) {
            throw new AppException(field + " must be a whole number.");
        }
    }

    public static BigDecimal money(String raw, String field, boolean allowZero) {
        BigDecimal value = Money.of(required(raw, field));
        if (value.signum() < 0 || (!allowZero && value.signum() == 0)) {
            throw new AppException(allowZero
                    ? field + " cannot be negative."
                    : field + " must be greater than zero.");
        }
        return value;
    }
}
