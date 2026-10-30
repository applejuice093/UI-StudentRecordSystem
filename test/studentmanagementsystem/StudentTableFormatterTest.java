package studentmanagementsystem;

import java.math.BigDecimal;

/**
 * Dependency-free tests for {@link StudentTableFormatter}. Run with `make test`.
 */
public class StudentTableFormatterTest {

    private static int failures = 0;
    private static int passed = 0;

    private static void check(String what, boolean condition) {
        if (condition) { passed++; } else { failures++; System.err.println("FAIL: " + what); }
    }

    public static void main(String[] args) {
        String empty = new StudentTableFormatter().toString();
        String[] headerLines = empty.split("\\R");
        check("header has two lines", headerLines.length == 2);
        check("header has columns", headerLines[0].startsWith("Reg No") && headerLines[0].contains("CGPA"));

        String table = new StudentTableFormatter()
                .addRow("RA001", "Aniket Mahto", 2, 4, new BigDecimal("9.1"))
                .addRow(null, null, 1, 1, null)
                .toString();
        String[] lines = table.split("\\R");
        check("two data rows", lines.length == 4);
        check("row contains reg no", lines[2].startsWith("RA001"));
        check("cgpa printed with 2 decimals", lines[2].trim().endsWith("9.10"));
        check("rows align with header", lines[2].length() == lines[0].length());
        check("null values do not crash", lines[3].trim().startsWith("1"));

        check("short name untouched", StudentTableFormatter.truncate("Bob").equals("Bob"));
        String longName = "A".repeat(40);
        String cut = StudentTableFormatter.truncate(longName);
        check("long name truncated to column width", cut.length() == StudentTableFormatter.NAME_WIDTH);
        check("long name ends with ellipsis", cut.endsWith("..."));
        check("30-char name untouched", StudentTableFormatter.truncate("B".repeat(30)).equals("B".repeat(30)));

        check("cgpa rounding", StudentTableFormatter.formatCgpa(new BigDecimal("8.755")).equals("8.76"));
        check("cgpa 10", StudentTableFormatter.formatCgpa(BigDecimal.TEN).equals("10.00"));
        check("cgpa null blank", StudentTableFormatter.formatCgpa(null).isEmpty());

        System.out.println("StudentTableFormatterTest: " + passed + " passed, " + failures + " failed");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
