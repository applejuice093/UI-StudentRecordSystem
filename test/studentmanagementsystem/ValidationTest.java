package studentmanagementsystem;

/**
 * Dependency-free tests for {@link Validation}. Run with `make test`.
 */
public class ValidationTest {

    private static int failures = 0;
    private static int passed = 0;

    private static void expectValid(String what, String result) {
        if (result == null) { passed++; } else { failures++; System.err.println("FAIL (expected valid): " + what + " -> " + result); }
    }

    private static void expectInvalid(String what, String result) {
        if (result != null) { passed++; } else { failures++; System.err.println("FAIL (expected invalid): " + what); }
    }

    public static void main(String[] args) {
        expectValid("regNo RA2311", Validation.validateRegNo("RA2311"));
        expectValid("regNo with dash", Validation.validateRegNo("21-CS-007"));
        expectInvalid("regNo empty", Validation.validateRegNo(" "));
        expectInvalid("regNo null", Validation.validateRegNo(null));
        expectInvalid("regNo symbols", Validation.validateRegNo("RA'; DROP"));
        expectInvalid("regNo too long", Validation.validateRegNo("A".repeat(21)));

        expectValid("name", Validation.validateFullName("Aniket Mahto"));
        expectValid("name apostrophe", Validation.validateFullName("D'Souza"));
        expectInvalid("name digits", Validation.validateFullName("R2D2"));
        expectInvalid("name 1 char", Validation.validateFullName("A"));

        expectValid("year 1", Validation.validateYear("1"));
        expectValid("year 5", Validation.validateYear("5"));
        expectInvalid("year 0", Validation.validateYear("0"));
        expectInvalid("year 6", Validation.validateYear("6"));
        expectInvalid("year text", Validation.validateYear("two"));

        expectValid("sem 8", Validation.validateSemester(" 8 "));
        expectInvalid("sem 11", Validation.validateSemester("11"));
        expectInvalid("sem decimal", Validation.validateSemester("2.5"));

        expectValid("cgpa 0", Validation.validateCgpa("0"));
        expectValid("cgpa 9.75", Validation.validateCgpa("9.75"));
        expectValid("cgpa 10", Validation.validateCgpa("10"));
        expectInvalid("cgpa 10.1", Validation.validateCgpa("10.1"));
        expectInvalid("cgpa negative", Validation.validateCgpa("-1"));
        expectInvalid("cgpa NaN", Validation.validateCgpa("NaN"));
        expectInvalid("cgpa text", Validation.validateCgpa("abc"));
        expectInvalid("cgpa exponent", Validation.validateCgpa("1e0"));
        expectInvalid("cgpa hex float", Validation.validateCgpa("0x1p3"));
        expectInvalid("cgpa double suffix", Validation.validateCgpa("9.5d"));
        expectInvalid("cgpa 3 decimals", Validation.validateCgpa("9.755"));
        expectValid("cgpa padded", Validation.validateCgpa(" 7.5 "));

        expectValid("username", Validation.validateUsername("aniket_12"));
        expectInvalid("username short", Validation.validateUsername("ab"));
        expectInvalid("username space", Validation.validateUsername("a b c"));
        expectValid("password", Validation.validatePassword("secret1"));
        expectInvalid("password short", Validation.validatePassword("12345"));
        expectInvalid("password empty", Validation.validatePassword(""));

        System.out.println("ValidationTest: " + passed + " passed, " + failures + " failed");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
