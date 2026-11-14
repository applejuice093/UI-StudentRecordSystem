package coralcove.dao;

import coralcove.model.Role;
import coralcove.model.User;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class UserDAO {

    public User findByUsername(Connection connection, String username) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findById(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> list(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM users ORDER BY username");
                var rs = ps.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        }
    }

    public long insert(Connection connection, User user) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO users(username, password_hash, role, employee_id, resort_id, must_change_password, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            Jdbc.nullableLong(ps, 4, user.getEmployeeId());
            Jdbc.nullableLong(ps, 5, user.getResortId());
            ps.setInt(6, user.isMustChangePassword() ? 1 : 0);
            ps.setInt(7, user.isActive() ? 1 : 0);
            ps.setString(8, user.getCreatedAt());
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updatePassword(Connection connection, long id, String hash, boolean mustChange) throws SQLException {
        try (var ps = connection.prepareStatement(
                "UPDATE users SET password_hash = ?, must_change_password = ? WHERE id = ?")) {
            ps.setString(1, hash);
            ps.setInt(2, mustChange ? 1 : 0);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void update(Connection connection, User user) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE users SET role = ?, employee_id = ?, resort_id = ?, active = ? WHERE id = ?
                """)) {
            ps.setString(1, user.getRole().name());
            Jdbc.nullableLong(ps, 2, user.getEmployeeId());
            Jdbc.nullableLong(ps, 3, user.getResortId());
            ps.setInt(4, user.isActive() ? 1 : 0);
            ps.setLong(5, user.getId());
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.parse(rs.getString("role")));
        user.setEmployeeId(Jdbc.longOrNull(rs, "employee_id"));
        user.setResortId(Jdbc.longOrNull(rs, "resort_id"));
        user.setMustChangePassword(rs.getInt("must_change_password") == 1);
        user.setActive(rs.getInt("active") == 1);
        user.setCreatedAt(rs.getString("created_at"));
        return user;
    }
}
