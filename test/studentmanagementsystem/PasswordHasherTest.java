package studentmanagementsystem;

/**
 * Dependency-free tests for {@link PasswordHasher}. Run with `make test`.
 */
public class PasswordHasherTest {

    private static int failures = 0;
    private static int passed = 0;

    private static void check(String what, boolean condition) {
        if (condition) { passed++; } else { failures++; System.err.println("FAIL: " + what); }
    }

    public static void main(String[] args) {
        String h1 = PasswordHasher.hash("secret123");
        String h2 = PasswordHasher.hash("secret123");

        check("hash is not the plain password", !h1.contains("secret123"));
        check("hash has expected prefix", h1.startsWith("pbkdf2$"));
        check("hash fits in VARCHAR(255)", h1.length() <= 255);
        check("same password gets a different salt", !h1.equals(h2));
        check("correct password verifies", PasswordHasher.verify("secret123", h1));
        check("second hash also verifies", PasswordHasher.verify("secret123", h2));
        check("wrong password rejected", !PasswordHasher.verify("secret124", h1));
        check("empty password rejected", !PasswordHasher.verify("", h1));
        check("null password rejected", !PasswordHasher.verify(null, h1));
        check("null stored value rejected", !PasswordHasher.verify("secret123", null));
        check("legacy plain-text value rejected", !PasswordHasher.verify("secret123", "secret123"));
        check("malformed hash rejected", !PasswordHasher.verify("x", "pbkdf2$abc$def"));
        check("bad base64 rejected", !PasswordHasher.verify("x", "pbkdf2$1000$***$***"));
        check("zero iterations rejected", !PasswordHasher.verify("x", "pbkdf2$0$AAAA$AAAA"));
        check("isHashed true for hash", PasswordHasher.isHashed(h1));
        check("isHashed false for plain text", !PasswordHasher.isHashed("hunter2"));
        check("unicode password round-trips", PasswordHasher.verify("pässwörd✓", PasswordHasher.hash("pässwörd✓")));

        System.out.println("PasswordHasherTest: " + passed + " passed, " + failures + " failed");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
