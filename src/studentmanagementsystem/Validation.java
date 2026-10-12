package studentmanagementsystem;

/**
 * Pure input-validation helpers used by the Swing forms.
 * Each method returns null when the input is valid, or a user-facing
 * error message otherwise. Keeping this logic out of the forms makes it
 * unit-testable without a GUI or database.
 */
public final class Validation {

    public static final int MIN_YEAR = 1;
    public static final int MAX_YEAR = 5;
    public static final int MIN_SEMESTER = 1;
    public static final int MAX_SEMESTER = 10;
    public static final double MIN_CGPA = 0.0;
    public static final double MAX_CGPA = 10.0;
    public static final int MIN_PASSWORD_LENGTH = 6;

    private Validation() {
    }

    public static String validateRegNo(String regNo) {
        if (regNo == null || regNo.isBlank()) {
            return "Registration Number is required.";
        }
        if (!regNo.trim().matches("[A-Za-z0-9-]{1,20}")) {
            return "Registration Number may only contain letters, digits and '-' (max 20 characters).";
        }
        return null;
    }

    public static String validateFullName(String name) {
        if (name == null || name.isBlank()) {
            return "Full Name is required.";
        }
        if (!name.trim().matches("[\\p{L} .'-]{2,100}")) {
            return "Full Name must be 2-100 characters and contain only letters, spaces, '.', ''' or '-'.";
        }
        return null;
    }

    public static String validateYear(String year) {
        return validateIntRange(year, "Year", MIN_YEAR, MAX_YEAR);
    }

    public static String validateSemester(String semester) {
        return validateIntRange(semester, "Semester", MIN_SEMESTER, MAX_SEMESTER);
    }

    public static String validateCgpa(String cgpa) {
        if (cgpa == null || cgpa.isBlank()) {
            return "CGPA is required.";
        }
        double value;
        try {
            value = Double.parseDouble(cgpa.trim());
        } catch (NumberFormatException ex) {
            return "CGPA must be a number.";
        }
        if (Double.isNaN(value) || value < MIN_CGPA || value > MAX_CGPA) {
            return "CGPA must be between " + MIN_CGPA + " and " + MAX_CGPA + ".";
        }
        return null;
    }

    public static String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            return "Username is required.";
        }
        if (!username.trim().matches("[A-Za-z0-9_.]{3,30}")) {
            return "Username must be 3-30 characters: letters, digits, '_' or '.'.";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        }
        return null;
    }

    private static String validateIntRange(String raw, String label, int min, int max) {
        if (raw == null || raw.isBlank()) {
            return label + " is required.";
        }
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return label + " must be a whole number.";
        }
        if (value < min || value > max) {
            return label + " must be between " + min + " and " + max + ".";
        }
        return null;
    }
}
