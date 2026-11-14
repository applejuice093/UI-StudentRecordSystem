package coralcove;

import coralcove.config.AppConfig;
import coralcove.model.Role;
import coralcove.model.User;
import coralcove.service.AuthenticationService;
import coralcove.service.Session;
import coralcove.util.AppException;
import org.junit.Assert;
import org.junit.Test;

public class AuthenticationServiceTest extends DbFixture {
    private final AuthenticationService auth = new AuthenticationService();

    @Test
    public void loginRejectsBadPasswordAndHonoursMustChange() {
        User admin = auth.login(AppConfig.DEFAULT_ADMIN, AppConfig.DEFAULT_PASSWORD);
        Assert.assertEquals(Role.ADMIN, admin.getRole());
        Assert.assertTrue(admin.isMustChangePassword());
        try {
            auth.login(AppConfig.DEFAULT_ADMIN, "wrong-pass");
            Assert.fail("expected failure");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("incorrect"));
        }
    }

    @Test
    public void changePasswordClearsFlagAndInactiveUserCannotLogin() {
        User admin = auth.login(AppConfig.DEFAULT_ADMIN, AppConfig.DEFAULT_PASSWORD);
        auth.changePassword(admin.getId(), AppConfig.DEFAULT_PASSWORD, "Newpass12");
        User again = auth.login(AppConfig.DEFAULT_ADMIN, "Newpass12");
        Assert.assertFalse(again.isMustChangePassword());
        Session.set(again);
        auth.createUser("desk", "Deskpass1", Role.FRONT_DESK);
        User desk = auth.login("desk", "Deskpass1");
        Session.set(again);
        desk.setActive(false);
        auth.updateUser(desk);
        try {
            auth.login("desk", "Deskpass1");
            Assert.fail("inactive login");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("incorrect"));
        }
    }

    @Test
    public void receptionistCannotManageUsers() {
        User desk = new User();
        desk.setId(Session.get().getId());
        desk.setRole(Role.FRONT_DESK);
        desk.setActive(true);
        Session.set(desk);
        try {
            auth.listUsers();
            Assert.fail("receptionist listed users");
        } catch (AppException ex) {
            Assert.assertTrue(ex.getMessage().contains("access"));
        }
    }
}
