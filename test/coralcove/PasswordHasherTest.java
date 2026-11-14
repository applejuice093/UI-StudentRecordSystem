package coralcove;

import coralcove.util.PasswordHasher;
import org.junit.Assert;
import org.junit.Test;

public class PasswordHasherTest {
    @Test
    public void hashVerifiesAndRejectsWrongOrMalformed() {
        String stored = PasswordHasher.hash("Admin@123");
        Assert.assertTrue(PasswordHasher.verify("Admin@123", stored));
        Assert.assertFalse(PasswordHasher.verify("admin@123", stored));
        Assert.assertFalse(PasswordHasher.verify("Admin@123", "plaintext"));
        Assert.assertFalse(PasswordHasher.verify(null, stored));
        Assert.assertTrue(stored.startsWith("pbkdf2$"));
    }
}
