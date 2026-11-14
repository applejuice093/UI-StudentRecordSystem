package coralcove.dao;

import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.model.LodgeCategory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class LodgeDAO {

    private static final String SELECT = """
            SELECT r.*, t.name AS type_name, s.name AS resort_name FROM lodges r
            JOIN lodge_categories t ON t.id = r.lodge_category_id
            JOIN resorts s ON s.id = r.resort_id
            """;

    public List<LodgeCategory> types(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM lodge_categories ORDER BY id");
                var rs = ps.executeQuery()) {
            List<LodgeCategory> types = new ArrayList<>();
            while (rs.next()) {
                LodgeCategory type = new LodgeCategory();
                type.setId(rs.getLong("id"));
                type.setName(rs.getString("name"));
                type.setDescription(rs.getString("description"));
                types.add(type);
            }
            return types;
        }
    }

    public Lodge findById(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement(SELECT + " WHERE r.id = ?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Lodge findByNumber(Connection connection, long resortId, String number) throws SQLException {
        try (var ps = connection.prepareStatement(SELECT + " WHERE r.resort_id = ? AND r.unit_code = ?")) {
            ps.setLong(1, resortId);
            ps.setString(2, number);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Lodge> search(Connection connection, String text, String zone, Long typeId, String status)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            sql.append(" AND (r.unit_code LIKE ? OR t.name LIKE ? OR r.amenities LIKE ?)");
            String like = "%" + text.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (zone != null && !zone.isBlank()) {
            sql.append(" AND r.zone = ?");
            args.add(zone);
        }
        Long resortFilter = coralcove.service.Session.resortFilter();
        if (resortFilter != null) {
            sql.append(" AND r.resort_id = ?");
            args.add(resortFilter);
        }
        if (typeId != null) {
            sql.append(" AND r.lodge_category_id = ?");
            args.add(typeId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND r.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY r.zone, r.unit_code");
        try (var ps = connection.prepareStatement(sql.toString())) {
            bind(ps, args);
            try (var rs = ps.executeQuery()) {
                List<Lodge> lodges = new ArrayList<>();
                while (rs.next()) {
                    lodges.add(map(rs));
                }
                return lodges;
            }
        }
    }

    public long insert(Connection connection, Lodge room) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO lodges(resort_id, unit_code, zone, view_aspect, lodge_category_id, bed_config, max_guests,
                    price_per_night, description, amenities, status, is_sample)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, room);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, Lodge room) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE lodges SET resort_id=?, unit_code=?, zone=?, view_aspect=?, lodge_category_id=?, bed_config=?,
                    max_guests=?, price_per_night=?, description=?, amenities=?, status=?, is_sample=? WHERE id=?
                """)) {
            fill(ps, room);
            ps.setLong(13, room.getId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection connection, long lodgeId, LodgeStatus status) throws SQLException {
        try (var ps = connection.prepareStatement("UPDATE lodges SET status=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setLong(2, lodgeId);
            ps.executeUpdate();
        }
    }

    public int countReservations(Connection connection, long lodgeId) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM stay_bookings WHERE lodge_id=?")) {
            ps.setLong(1, lodgeId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void delete(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("DELETE FROM lodges WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public int countByStatus(Connection connection, String status) throws SQLException {
        String sql = status == null ? "SELECT COUNT(*) FROM lodges" : "SELECT COUNT(*) FROM lodges WHERE status=?";
        try (var ps = connection.prepareStatement(sql)) {
            if (status != null) {
                ps.setString(1, status);
            }
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void fill(java.sql.PreparedStatement ps, Lodge room) throws SQLException {
        ps.setLong(1, room.getResortId());
        ps.setString(2, room.getUnitCode());
        ps.setString(3, room.getZone());
        ps.setString(4, room.getViewAspect());
        ps.setLong(5, room.getLodgeCategoryId());
        ps.setString(6, room.getBedConfig());
        ps.setInt(7, room.getMaxGuests());
        Jdbc.money(ps, 8, room.getPricePerNight());
        ps.setString(9, room.getDescription());
        ps.setString(10, room.getAmenities());
        ps.setString(11, room.getStatus().name());
        ps.setInt(12, room.isSample() ? 1 : 0);
    }

    private Lodge map(ResultSet rs) throws SQLException {
        Lodge room = new Lodge();
        room.setId(rs.getLong("id"));
        room.setResortId(rs.getLong("resort_id"));
        room.setResortName(rs.getString("resort_name"));
        room.setUnitCode(rs.getString("unit_code"));
        room.setZone(rs.getString("zone"));
        room.setViewAspect(rs.getString("view_aspect"));
        room.setLodgeCategoryId(rs.getLong("lodge_category_id"));
        room.setRoomTypeName(rs.getString("type_name"));
        room.setBedConfig(rs.getString("bed_config"));
        room.setMaxGuests(rs.getInt("max_guests"));
        room.setPricePerNight(Jdbc.money(rs, "price_per_night"));
        room.setDescription(rs.getString("description"));
        room.setAmenities(rs.getString("amenities"));
        room.setStatus(LodgeStatus.parse(rs.getString("status")));
        room.setSample(rs.getInt("is_sample") == 1);
        return room;
    }

    private void bind(java.sql.PreparedStatement ps, List<Object> args) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            if (arg instanceof Integer n) {
                ps.setInt(i + 1, n);
            } else if (arg instanceof Long n) {
                ps.setLong(i + 1, n);
            } else {
                ps.setString(i + 1, String.valueOf(arg));
            }
        }
    }
}
