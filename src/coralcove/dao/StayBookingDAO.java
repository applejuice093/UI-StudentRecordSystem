package coralcove.dao;

import coralcove.model.BookingSource;
import coralcove.model.StayBooking;
import coralcove.model.StayStatus;
import coralcove.util.DateUtils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class StayBookingDAO {

    private static final String SELECT = """
            SELECT r.*, g.full_name AS guest_name, rm.unit_code
            FROM stay_bookings r
            JOIN guests g ON g.id = r.guest_id
            JOIN lodges rm ON rm.id = r.lodge_id
            """;

    public boolean overlaps(Connection connection, long lodgeId, LocalDate checkIn, LocalDate checkOut, Long excludeId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM stay_bookings
                WHERE lodge_id = ? AND status <> 'CANCELLED'
                  AND check_in < ? AND check_out > ?
                """;
        if (excludeId != null) {
            sql += " AND id <> ?";
        }
        try (var ps = connection.prepareStatement(sql)) {
            ps.setLong(1, lodgeId);
            ps.setString(2, DateUtils.iso(checkOut));
            ps.setString(3, DateUtils.iso(checkIn));
            if (excludeId != null) {
                ps.setLong(4, excludeId);
            }
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public boolean referenceExists(Connection connection, String reference, Long excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM stay_bookings WHERE booking_reference = ?";
        if (excludeId != null) {
            sql += " AND id <> ?";
        }
        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, reference);
            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public long insert(Connection connection, StayBooking reservation) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO stay_bookings(booking_reference, resort_id, guest_id, lodge_id, check_in, check_out, adults,
                    children, nights, room_price, discount, tax, total, source, meal_plan, transfer, status,
                    special_requests, created_at, is_sample)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, reservation);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, StayBooking reservation) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE stay_bookings SET booking_reference=?, resort_id=?, guest_id=?, lodge_id=?, check_in=?, check_out=?,
                    adults=?, children=?, nights=?, room_price=?, discount=?, tax=?, total=?, source=?, meal_plan=?,
                    transfer=?, status=?, special_requests=?, created_at=?, is_sample=? WHERE id=?
                """)) {
            fill(ps, reservation);
            ps.setLong(21, reservation.getId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection connection, long id, StayStatus status) throws SQLException {
        try (var ps = connection.prepareStatement("UPDATE stay_bookings SET status=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void linkGuest(Connection connection, long reservationId, long guestId, boolean primary) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO stay_party(reservation_id, guest_id, is_primary) VALUES (?,?,?)
                ON CONFLICT(reservation_id, guest_id) DO UPDATE SET is_primary = excluded.is_primary
                """)) {
            ps.setLong(1, reservationId);
            ps.setLong(2, guestId);
            ps.setInt(3, primary ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public StayBooking findById(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement(SELECT + " WHERE r.id=?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public StayBooking findByReference(Connection connection, String reference) throws SQLException {
        try (var ps = connection.prepareStatement(SELECT + " WHERE r.booking_reference=?")) {
            ps.setString(1, reference);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<StayBooking> search(Connection connection, String reference, String guest, LocalDate from, LocalDate to,
            String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (reference != null && !reference.isBlank()) {
            sql.append(" AND r.booking_reference LIKE ?");
            args.add("%" + reference.trim() + "%");
        }
        if (guest != null && !guest.isBlank()) {
            sql.append(" AND g.full_name LIKE ?");
            args.add("%" + guest.trim() + "%");
        }
        if (from != null) {
            sql.append(" AND r.check_out > ?");
            args.add(DateUtils.iso(from));
        }
        if (to != null) {
            sql.append(" AND r.check_in < ?");
            args.add(DateUtils.iso(to));
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND r.status = ?");
            args.add(status);
        }
        Long resortFilter = coralcove.service.Session.resortFilter();
        if (resortFilter != null) {
            sql.append(" AND r.resort_id = ?");
            args.add(String.valueOf(resortFilter));
        }
        sql.append(" ORDER BY r.check_in DESC, r.id DESC");
        try (var ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setString(i + 1, args.get(i));
            }
            try (var rs = ps.executeQuery()) {
                List<StayBooking> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public List<StayBooking> forGuest(Connection connection, long guestId) throws SQLException {
        try (var ps = connection.prepareStatement(SELECT + " WHERE r.guest_id=? ORDER BY r.check_in DESC")) {
            ps.setLong(1, guestId);
            try (var rs = ps.executeQuery()) {
                List<StayBooking> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public boolean hasActiveCheckedIn(Connection connection, long lodgeId, Long excludeReservation) throws SQLException {
        String sql = "SELECT COUNT(*) FROM stay_bookings WHERE lodge_id=? AND status='ARRIVED'";
        if (excludeReservation != null) {
            sql += " AND id<>?";
        }
        try (var ps = connection.prepareStatement(sql)) {
            ps.setLong(1, lodgeId);
            if (excludeReservation != null) {
                ps.setLong(2, excludeReservation);
            }
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public boolean hasUpcomingHold(Connection connection, long lodgeId, LocalDate today) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT COUNT(*) FROM stay_bookings
                WHERE lodge_id=? AND status IN ('PENDING','CONFIRMED')
                  AND check_in <= ? AND check_out > ?
                """)) {
            ps.setLong(1, lodgeId);
            ps.setString(2, DateUtils.iso(today));
            ps.setString(3, DateUtils.iso(today));
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private void fill(java.sql.PreparedStatement ps, StayBooking reservation) throws SQLException {
        ps.setString(1, reservation.getBookingReference());
        ps.setLong(2, reservation.getResortId());
        ps.setLong(3, reservation.getGuestId());
        ps.setLong(4, reservation.getLodgeId());
        ps.setString(5, DateUtils.iso(reservation.getCheckIn()));
        ps.setString(6, DateUtils.iso(reservation.getCheckOut()));
        ps.setInt(7, reservation.getAdults());
        ps.setInt(8, reservation.getChildren());
        ps.setInt(9, reservation.getNights());
        Jdbc.money(ps, 10, reservation.getRoomPrice());
        Jdbc.money(ps, 11, reservation.getDiscount());
        Jdbc.money(ps, 12, reservation.getTax());
        Jdbc.money(ps, 13, reservation.getTotal());
        ps.setString(14, reservation.getSource().name());
        ps.setString(15, reservation.getMealPlan() == null ? "EP" : reservation.getMealPlan());
        ps.setString(16, reservation.getTransfer() == null ? "NONE" : reservation.getTransfer());
        ps.setString(17, reservation.getStatus().name());
        ps.setString(18, reservation.getSpecialRequests());
        ps.setString(19, reservation.getCreatedAt());
        ps.setInt(20, reservation.isSample() ? 1 : 0);
    }

    private StayBooking map(ResultSet rs) throws SQLException {
        StayBooking reservation = new StayBooking();
        reservation.setId(rs.getLong("id"));
        reservation.setBookingReference(rs.getString("booking_reference"));
        reservation.setResortId(rs.getLong("resort_id"));
        reservation.setMealPlan(rs.getString("meal_plan"));
        reservation.setTransfer(rs.getString("transfer"));
        reservation.setGuestId(rs.getLong("guest_id"));
        reservation.setGuestName(rs.getString("guest_name"));
        reservation.setLodgeId(rs.getLong("lodge_id"));
        reservation.setUnitCode(rs.getString("unit_code"));
        reservation.setCheckIn(DateUtils.parse(rs.getString("check_in")));
        reservation.setCheckOut(DateUtils.parse(rs.getString("check_out")));
        reservation.setAdults(rs.getInt("adults"));
        reservation.setChildren(rs.getInt("children"));
        reservation.setNights(rs.getInt("nights"));
        reservation.setRoomPrice(Jdbc.money(rs, "room_price"));
        reservation.setDiscount(Jdbc.money(rs, "discount"));
        reservation.setTax(Jdbc.money(rs, "tax"));
        reservation.setTotal(Jdbc.money(rs, "total"));
        reservation.setSource(BookingSource.parse(rs.getString("source")));
        reservation.setStatus(StayStatus.parse(rs.getString("status")));
        reservation.setSpecialRequests(rs.getString("special_requests"));
        reservation.setCreatedAt(rs.getString("created_at"));
        reservation.setSample(rs.getInt("is_sample") == 1);
        return reservation;
    }
}
