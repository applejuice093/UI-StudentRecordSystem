package coralcove.dao;

import coralcove.model.Guest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class GuestDAO {

    public Guest findById(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM guests WHERE id=?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Guest> search(Connection connection, String text) throws SQLException {
        String sql = "SELECT * FROM guests";
        if (text != null && !text.isBlank()) {
            sql += " WHERE full_name LIKE ? OR phone LIKE ? OR email LIKE ? OR id_doc_number LIKE ?";
        }
        sql += " ORDER BY full_name";
        try (var ps = connection.prepareStatement(sql)) {
            if (text != null && !text.isBlank()) {
                String like = "%" + text.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
            }
            try (var rs = ps.executeQuery()) {
                List<Guest> guests = new ArrayList<>();
                while (rs.next()) {
                    guests.add(map(rs));
                }
                return guests;
            }
        }
    }

    public long insert(Connection connection, Guest guest) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO guests(full_name, phone, email, gender, nationality, address, id_doc_type,
                    id_doc_number, preferences, vip, registered_on, is_sample)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, guest);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, Guest guest) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE guests SET full_name=?, phone=?, email=?, gender=?, nationality=?, address=?,
                    id_doc_type=?, id_doc_number=?, preferences=?, vip=?, registered_on=?, is_sample=?
                WHERE id=?
                """)) {
            fill(ps, guest);
            ps.setLong(13, guest.getId());
            ps.executeUpdate();
        }
    }

    public int countReservations(Connection connection, long guestId) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM stay_bookings WHERE guest_id=?")) {
            ps.setLong(1, guestId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void delete(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("DELETE FROM guests WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public int countAll(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM guests");
                var rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void fill(java.sql.PreparedStatement ps, Guest guest) throws SQLException {
        ps.setString(1, guest.getFullName());
        ps.setString(2, emptyToNull(guest.getPhone()));
        ps.setString(3, emptyToNull(guest.getEmail()));
        ps.setString(4, emptyToNull(guest.getGender()));
        ps.setString(5, emptyToNull(guest.getNationality()));
        ps.setString(6, emptyToNull(guest.getAddress()));
        ps.setString(7, emptyToNull(guest.getIdDocType()));
        ps.setString(8, emptyToNull(guest.getIdDocNumber()));
        ps.setString(9, emptyToNull(guest.getPreferences()));
        ps.setString(10, guest.getVip() == null ? "STANDARD" : guest.getVip());
        ps.setString(11, guest.getRegisteredOn());
        ps.setInt(12, guest.isSample() ? 1 : 0);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Guest map(ResultSet rs) throws SQLException {
        Guest guest = new Guest();
        guest.setId(rs.getLong("id"));
        guest.setFullName(rs.getString("full_name"));
        guest.setPhone(rs.getString("phone"));
        guest.setEmail(rs.getString("email"));
        guest.setGender(rs.getString("gender"));
        guest.setNationality(rs.getString("nationality"));
        guest.setAddress(rs.getString("address"));
        guest.setIdDocType(rs.getString("id_doc_type"));
        guest.setIdDocNumber(rs.getString("id_doc_number"));
        guest.setPreferences(rs.getString("preferences"));
        guest.setVip(rs.getString("vip"));
        guest.setRegisteredOn(rs.getString("registered_on"));
        guest.setSample(rs.getInt("is_sample") == 1);
        return guest;
    }
}
