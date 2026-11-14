package coralcove.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2-HMAC-SHA256 hashes. Stored form:
 * {@code pbkdf2$<iterations>$<base64 salt>$<base64 hash>}.
 */
public final class PasswordHasher {

    static final String PREFIX = "pbkdf2";
    static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    static final int ITERATIONS = 210_000;
    static final int SALT_BYTES = 16;
    static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(key);
    }

    /** Returns false for a null password or a malformed stored hash. */
    public static boolean verify(String password, String stored) {
        if (password == null || !isHashed(stored)) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations <= 0) {
                return false;
            }
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] salt = b64.decode(parts[2]);
            byte[] expected = b64.decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX + "$");
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations, int keyBits) {
        if (keyBits <= 0) {
            throw new IllegalArgumentException("invalid key length");
        }
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 is not available on this JVM", ex);
        }
    }
}
