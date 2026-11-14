package coralcove.dao;

import coralcove.model.Experience;
import coralcove.model.ExperienceBooking;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ExperienceDAO {

    public List<Experience> menu(Connection connection, boolean activeOnly) throws SQLException {
        Long resortFilter = coralcove.service.Session.resortFilter();
        String sql = "SELECT * FROM experiences WHERE 1=1"
                + (activeOnly ? " AND active=1" : "")
                + (resortFilter == null ? "" : " AND resort_id=" + resortFilter)
                + " ORDER BY category, name";
        try (var ps = connection.prepareStatement(sql); var rs = ps.executeQuery()) {
            List<Experience> rows = new ArrayList<>();
            while (rs.next()) {
                Experience item = new Experience();
                item.setId(rs.getLong("id"));
                item.setResortId(rs.getLong("resort_id"));
                item.setName(rs.getString("name"));
                item.setCategory(rs.getString("category"));
                item.setPrice(Jdbc.money(rs, "price"));
                item.setActive(rs.getInt("active") == 1);
                rows.add(item);
            }
            return rows;
        }
    }

    public long insertItem(Connection connection, Experience item) throws SQLException {
        try (var ps = connection.prepareStatement(
                "INSERT INTO experiences(resort_id, name, category, price, active) VALUES (?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, item.getResortId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getCategory());
            Jdbc.money(ps, 4, item.getPrice());
            ps.setInt(5, item.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updateItem(Connection connection, Experience item) throws SQLException {
        try (var ps = connection.prepareStatement(
                "UPDATE experiences SET resort_id=?, name=?, category=?, price=?, active=? WHERE id=?")) {
            ps.setLong(1, item.getResortId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getCategory());
            Jdbc.money(ps, 4, item.getPrice());
            ps.setInt(5, item.isActive() ? 1 : 0);
            ps.setLong(6, item.getId());
            ps.executeUpdate();
        }
    }

    public long insertOrder(Connection connection, ExperienceBooking order) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO experience_bookings(experience_id, reservation_id, guest_id, lodge_id, order_type, description,
                    qty, unit_price, line_total, status, invoice_item_id, created_at, is_sample)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            Jdbc.nullableLong(ps, 1, order.getServiceId());
            Jdbc.nullableLong(ps, 2, order.getReservationId());
            Jdbc.nullableLong(ps, 3, order.getGuestId());
            Jdbc.nullableLong(ps, 4, order.getLodgeId());
            ps.setString(5, order.getOrderType());
            ps.setString(6, order.getDescription());
            ps.setInt(7, order.getQty());
            Jdbc.money(ps, 8, order.getUnitPrice());
            Jdbc.money(ps, 9, order.getLineTotal());
            ps.setString(10, order.getStatus());
            Jdbc.nullableLong(ps, 11, order.getInvoiceItemId());
            ps.setString(12, order.getCreatedAt());
            ps.setInt(13, order.isSample() ? 1 : 0);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<ExperienceBooking> orders(Connection connection, String status) throws SQLException {
        String sql = """
                SELECT o.*, r.unit_code FROM experience_bookings o
                LEFT JOIN lodges r ON r.id = o.lodge_id
                LEFT JOIN experiences x ON x.id = o.experience_id
                WHERE 1=1
                """;
        Long resortFilter = coralcove.service.Session.resortFilter();
        if (resortFilter != null) {
            sql += " AND (r.resort_id=" + resortFilter + " OR x.resort_id=" + resortFilter + ")";
        } else {
            sql += " AND (x.resort_id IS NULL OR x.resort_id IN (SELECT id FROM resorts WHERE active=1))";
        }
        if (status != null && !status.isBlank()) {
            sql += " AND o.status=?";
        }
        sql += " ORDER BY o.id DESC";
        try (var ps = connection.prepareStatement(sql)) {
            if (status != null && !status.isBlank()) {
                ps.setString(1, status);
            }
            try (var rs = ps.executeQuery()) {
                List<ExperienceBooking> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public ExperienceBooking find(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT o.*, r.unit_code FROM experience_bookings o
                LEFT JOIN lodges r ON r.id=o.lodge_id WHERE o.id=?
                """)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void updateStatus(Connection connection, long id, String status, Long invoiceItemId) throws SQLException {
        try (var ps = connection.prepareStatement(
                "UPDATE experience_bookings SET status=?, invoice_item_id=? WHERE id=?")) {
            ps.setString(1, status);
            Jdbc.nullableLong(ps, 2, invoiceItemId);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public List<ExperienceBooking> unbilledServed(Connection connection, long reservationId) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT o.*, r.unit_code FROM experience_bookings o
                LEFT JOIN lodges r ON r.id=o.lodge_id
                WHERE o.reservation_id=? AND o.status='SERVED' AND o.invoice_item_id IS NULL
                """)) {
            ps.setLong(1, reservationId);
            try (var rs = ps.executeQuery()) {
                List<ExperienceBooking> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    private ExperienceBooking map(ResultSet rs) throws SQLException {
        ExperienceBooking order = new ExperienceBooking();
        order.setId(rs.getLong("id"));
        order.setServiceId(Jdbc.longOrNull(rs, "experience_id"));
        order.setReservationId(Jdbc.longOrNull(rs, "reservation_id"));
        order.setGuestId(Jdbc.longOrNull(rs, "guest_id"));
        order.setLodgeId(Jdbc.longOrNull(rs, "lodge_id"));
        order.setUnitCode(rs.getString("unit_code"));
        order.setOrderType(rs.getString("order_type"));
        order.setDescription(rs.getString("description"));
        order.setQty(rs.getInt("qty"));
        order.setUnitPrice(Jdbc.money(rs, "unit_price"));
        order.setLineTotal(Jdbc.money(rs, "line_total"));
        order.setStatus(rs.getString("status"));
        order.setInvoiceItemId(Jdbc.longOrNull(rs, "invoice_item_id"));
        order.setCreatedAt(rs.getString("created_at"));
        order.setSample(rs.getInt("is_sample") == 1);
        return order;
    }
}
