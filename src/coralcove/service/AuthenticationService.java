package coralcove.service;

import coralcove.dao.AuditDAO;
import coralcove.dao.UserDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.Role;
import coralcove.model.User;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.PasswordHasher;
import coralcove.util.ValidationUtils;
import java.sql.SQLException;
import java.util.List;

public final class AuthenticationService {

    private final UserDAO users = new UserDAO();
    private final AuditDAO audit = new AuditDAO();

    public User login(String username, String password) {
        ValidationUtils.required(username, "Username");
        ValidationUtils.required(password, "Password");
        try (var connection = DatabaseManager.getConnection()) {
            User user = users.findByUsername(connection, username.trim());
            if (user == null || !user.isActive() || !PasswordHasher.verify(password, user.getPasswordHash())) {
                throw new AppException("Username or password is incorrect.");
            }
            audit.log(connection, user.getId(), "LOGIN", "users", String.valueOf(user.getId()), user.getUsername());
            applyResort(connection, user);
            return user;
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void changePassword(long userId, String currentPassword, String newPassword) {
        ValidationUtils.password(newPassword);
        Transactions.in(connection -> {
            User user = users.findById(connection, userId);
            if (user == null || !PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
                throw new AppException("Current password is incorrect.");
            }
            if (currentPassword.equals(newPassword)) {
                throw new AppException("Choose a password that is different from the current one.");
            }
            users.updatePassword(connection, userId, PasswordHasher.hash(newPassword), false);
            user.setMustChangePassword(false);
            if (Session.get() != null && Session.get().getId() == userId) {
                Session.set(user);
            }
            audit.log(connection, userId, "PASSWORD", "users", String.valueOf(userId), "changed");
            return null;
        });
    }

    public List<User> listUsers() {
        AuthorizationService.require(coralcove.model.AppModule.SETTINGS);
        try (var connection = DatabaseManager.getConnection()) {
            return users.list(connection);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public void createUser(String username, String password, Role role) {
        AuthorizationService.require(coralcove.model.AppModule.SETTINGS);
        ValidationUtils.username(username);
        ValidationUtils.password(password);
        if (role == null) {
            throw new AppException("Role is required.");
        }
        Transactions.in(connection -> {
            if (users.findByUsername(connection, username.trim()) != null) {
                throw new AppException("That username is already in use.");
            }
            User user = new User();
            user.setUsername(username.trim());
            user.setPasswordHash(PasswordHasher.hash(password));
            user.setRole(role);
            if (role != Role.ADMIN) {
                user.setResortId(ResortScope.resolve(connection, 0));
            }
            user.setMustChangePassword(true);
            user.setActive(true);
            user.setCreatedAt(DateUtils.nowStamp());
            long id = users.insert(connection, user);
            audit.log(connection, Session.get().getId(), "CREATE", "users", String.valueOf(id), username.trim());
            return id;
        });
        Session.dataChanged();
    }

    public void updateUser(User user) {
        AuthorizationService.require(coralcove.model.AppModule.SETTINGS);
        if (user.getId() == Session.get().getId() && !user.isActive()) {
            throw new AppException("You cannot deactivate the account you are using.");
        }
        if (user.getId() == Session.get().getId() && user.getRole() != Role.ADMIN) {
            throw new AppException("You cannot remove your own administrator role.");
        }
        if (user.getRole() != Role.ADMIN && user.getResortId() == null) {
            throw new AppException("Assign this account to a resort.");
        }
        Transactions.in(connection -> {
            users.update(connection, user);
            return null;
        });
    }

    private void applyResort(java.sql.Connection connection, User user) throws SQLException {
        if (user.getRole() != Role.ADMIN) {
            if (user.getResortId() == null) {
                throw new AppException("This account is not assigned to a resort.");
            }
            Session.setResortFilter(user.getResortId());
            return;
        }
        try (var ps = connection.prepareStatement(
                "SELECT id FROM resorts WHERE active=1 ORDER BY id LIMIT 1");
                var rs = ps.executeQuery()) {
            Session.setResortFilter(rs.next() ? rs.getLong(1) : null);
        }
    }
}
