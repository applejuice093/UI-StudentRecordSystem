package studentmanagementsystem;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Builds the fixed-width text table shown in {@link ViewStudent}.
 * Kept separate from the Swing form so the layout can be unit tested.
 */
public final class StudentTableFormatter {

    static final String ROW_FORMAT = "%-18s %-30s %6s %8s %6s%n";
    static final int NAME_WIDTH = 30;

    private final StringBuilder sb = new StringBuilder();

    public StudentTableFormatter() {
        sb.append(String.format(ROW_FORMAT, "Reg No", "Full Name", "Year", "Semester", "CGPA"));
        sb.append(String.format(ROW_FORMAT, "-".repeat(18), "-".repeat(NAME_WIDTH), "----", "--------", "----"));
    }

    /** Appends one student row. Null text values are shown as blanks. */
    public StudentTableFormatter addRow(String regNo, String fullName, int year, int semester, BigDecimal cgpa) {
        sb.append(String.format(ROW_FORMAT,
                regNo == null ? "" : regNo,
                truncate(fullName == null ? "" : fullName),
                year,
                semester,
                formatCgpa(cgpa)));
        return this;
    }

    /** Shortens names longer than the column so alignment stays intact. */
    static String truncate(String name) {
        return name.length() > NAME_WIDTH ? name.substring(0, NAME_WIDTH - 3) + "..." : name;
    }

    /** Formats CGPA with two decimals; exact for DECIMAL(4,2) values. */
    static String formatCgpa(BigDecimal cgpa) {
        return cgpa == null ? "" : cgpa.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
