package coralcove.dao;

import coralcove.model.StayStatus;
import coralcove.service.Session;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read models for dashboard cards, charts, and the reports screen. */
public final class ReportDAO {

    public int countRooms(Connection connection, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lodges WHERE 1=1" + scope("resort_id")
                + (status == null ? "" : " AND status=?");
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

    public int countGuests(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM guests"); var rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public BigDecimal paymentsOn(Connection connection, LocalDate day) throws SQLException {
        return sumPayments(connection, DateUtils.iso(day), DateUtils.iso(day));
    }

    public List<String[]> arrivals(Connection connection, LocalDate day) throws SQLException {
        return stays(connection, "r.check_in=?", DateUtils.iso(day),
                "r.status IN ('PENDING','CONFIRMED','ARRIVED')");
    }

    public List<String[]> departures(Connection connection, LocalDate day) throws SQLException {
        return stays(connection, "r.check_out=?", DateUtils.iso(day),
                "r.status IN ('ARRIVED','CONFIRMED')");
    }

    public List<String[]> recentReservations(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT r.booking_reference, g.full_name, rm.unit_code, r.check_in, r.status, r.total
                FROM stay_bookings r
                JOIN guests g ON g.id=r.guest_id
                JOIN lodges rm ON rm.id=r.lodge_id
                WHERE 1=1
                """ + scope("r.resort_id") + """
                ORDER BY r.id DESC LIMIT 8
                """);
                var rs = ps.executeQuery()) {
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[] {
                        rs.getString(1), rs.getString(2), rs.getString(3), DateUtils.display(rs.getString(4)),
                        rs.getString(5), CurrencyUtils.format(Jdbc.money(rs, "total"))
                });
            }
            return rows;
        }
    }

    public List<String[]> recentPayments(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT p.paid_at, p.reference, p.method, p.amount, i.invoice_number
                FROM payments p JOIN invoices i ON i.id=p.invoice_id
                JOIN stay_bookings r ON r.id=i.reservation_id
                WHERE 1=1
                """ + scope("r.resort_id") + " ORDER BY p.id DESC LIMIT 8");
                var rs = ps.executeQuery()) {
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[] {
                        rs.getString(1), rs.getString(5), rs.getString(2), rs.getString(3),
                        CurrencyUtils.format(Jdbc.money(rs, "amount"))
                });
            }
            return rows;
        }
    }

    public Map<String, BigDecimal> monthlyRevenue(Connection connection, int months) throws SQLException {
        YearMonth start = YearMonth.from(DateUtils.today()).minusMonths(months - 1L);
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            series.put(start.plusMonths(i).toString(), Money.zero());
        }
        try (var ps = connection.prepareStatement("""
                SELECT p.paid_at, p.amount FROM payments p
                JOIN invoices i ON i.id=p.invoice_id
                JOIN stay_bookings r ON r.id=i.reservation_id
                WHERE 1=1
                """ + scope("r.resort_id"));
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                String stamp = rs.getString(1);
                if (stamp == null || stamp.length() < 7) {
                    continue;
                }
                String key = stamp.substring(0, 7);
                if (series.containsKey(key)) {
                    series.put(key, series.get(key).add(Jdbc.money(rs, "amount")));
                }
            }
        }
        return series;
    }

    public Map<String, Integer> bookingsByMonth(Connection connection, int months) throws SQLException {
        YearMonth start = YearMonth.from(DateUtils.today()).minusMonths(months - 1L);
        Map<String, Integer> series = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            series.put(start.plusMonths(i).toString(), 0);
        }
        try (var ps = connection.prepareStatement(
                "SELECT check_in FROM stay_bookings WHERE status <> 'CANCELLED'" + scope("resort_id"));
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getString(1).substring(0, 7);
                if (series.containsKey(key)) {
                    series.put(key, series.get(key) + 1);
                }
            }
        }
        return series;
    }

    public Map<String, Integer> roomsByType(Connection connection) throws SQLException {
        Map<String, Integer> series = new LinkedHashMap<>();
        try (var ps = connection.prepareStatement("""
                SELECT t.name, COUNT(r.id) FROM lodge_categories t
                LEFT JOIN lodges r ON r.lodge_category_id=t.id
                """ + (Session.resortFilter() == null
                    ? " AND r.resort_id IN (SELECT id FROM resorts WHERE active=1)"
                    : " AND r.resort_id = " + Session.resortFilter()) + """
                GROUP BY t.id ORDER BY t.id
                """);
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                series.put(rs.getString(1), rs.getInt(2));
            }
        }
        return series;
    }

    /**
     * Occupancy uses reservation nights, not today's room status.
     * Numerator: nights inside the inclusive range covered by CONFIRMED, ARRIVED, or DEPARTED.
     * Denominator: lodges whose status is not CLOSED, multiplied by the number of days in the range.
     */
    public String[] occupancy(Connection connection, LocalDate from, LocalDate to) throws SQLException {
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        int sellable = 0;
        try (var ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM lodges WHERE status <> 'CLOSED'" + scope("resort_id"));
                var rs = ps.executeQuery()) {
            rs.next();
            sellable = rs.getInt(1);
        }
        int sold = 0;
        try (var ps = connection.prepareStatement("""
                SELECT check_in, check_out, status FROM stay_bookings
                WHERE status IN ('CONFIRMED','ARRIVED','DEPARTED')
                """ + scope("resort_id"));
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate in = DateUtils.parse(rs.getString(1));
                LocalDate out = DateUtils.parse(rs.getString(2));
                LocalDate start = in.isBefore(from) ? from : in;
                LocalDate end = out.isAfter(to.plusDays(1)) ? to.plusDays(1) : out;
                if (start.isBefore(end)) {
                    sold += (int) java.time.temporal.ChronoUnit.DAYS.between(start, end);
                }
            }
        }
        int denominator = sellable * days;
        BigDecimal rate = denominator == 0
                ? Money.zero()
                : BigDecimal.valueOf(sold).multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        return new String[] {
                String.valueOf(sold), String.valueOf(denominator), rate.toPlainString() + "%"
        };
    }

    public List<List<String>> revenueReport(Connection connection, LocalDate from, LocalDate to, String grain)
            throws SQLException {
        Map<String, BigDecimal> buckets = new LinkedHashMap<>();
        try (var ps = connection.prepareStatement("""
                SELECT p.paid_at, p.amount FROM payments p
                JOIN invoices i ON i.id=p.invoice_id
                JOIN stay_bookings r ON r.id=i.reservation_id
                WHERE substr(p.paid_at,1,10) >= ? AND substr(p.paid_at,1,10) <= ?
                """ + scope("r.resort_id"))) {
            ps.setString(1, DateUtils.iso(from));
            ps.setString(2, DateUtils.iso(to));
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String stamp = rs.getString(1);
                    String key = switch (grain) {
                        case "year" -> stamp.substring(0, 4);
                        case "month" -> stamp.substring(0, 7);
                        default -> stamp.substring(0, 10);
                    };
                    buckets.merge(key, Jdbc.money(rs, "amount"), BigDecimal::add);
                }
            }
        }
        List<List<String>> rows = new ArrayList<>();
        for (var entry : buckets.entrySet()) {
            rows.add(List.of(entry.getKey(), CurrencyUtils.formatPlain(entry.getValue())));
        }
        return rows;
    }

    public List<List<String>> reservationTrend(Connection connection, LocalDate from, LocalDate to) throws SQLException {
        Map<String, Integer> created = new LinkedHashMap<>();
        Map<String, Integer> cancelled = new LinkedHashMap<>();
        try (var ps = connection.prepareStatement("""
                SELECT check_in, status FROM stay_bookings
                WHERE check_in >= ? AND check_in <= ?
                """ + scope("resort_id"))) {
            ps.setString(1, DateUtils.iso(from));
            ps.setString(2, DateUtils.iso(to));
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString(1).substring(0, 7);
                    created.merge(key, 1, Integer::sum);
                    if (StayStatus.CANCELLED.name().equals(rs.getString(2))) {
                        cancelled.merge(key, 1, Integer::sum);
                    }
                }
            }
        }
        List<List<String>> rows = new ArrayList<>();
        for (var entry : created.entrySet()) {
            int cancel = cancelled.getOrDefault(entry.getKey(), 0);
            BigDecimal rate = entry.getValue() == 0
                    ? Money.zero()
                    : BigDecimal.valueOf(cancel * 100.0 / entry.getValue()).setScale(2, RoundingMode.HALF_UP);
            rows.add(List.of(entry.getKey(), String.valueOf(entry.getValue()), String.valueOf(cancel),
                    rate.toPlainString() + "%"));
        }
        return rows;
    }

    public List<List<String>> outstanding(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT i.invoice_number, g.full_name, i.total, i.amount_paid, i.balance, i.status
                FROM invoices i JOIN guests g ON g.id=i.guest_id
                JOIN stay_bookings r ON r.id=i.reservation_id
                WHERE i.balance <> '0.00'
                """ + scope("r.resort_id") + " ORDER BY i.id DESC");
                var rs = ps.executeQuery()) {
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(List.of(rs.getString(1), rs.getString(2),
                        CurrencyUtils.formatPlain(Jdbc.money(rs, "total")),
                        CurrencyUtils.formatPlain(Jdbc.money(rs, "amount_paid")),
                        CurrencyUtils.formatPlain(Jdbc.money(rs, "balance")),
                        rs.getString(6)));
            }
            return rows;
        }
    }

    public List<List<String>> guestHistory(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT g.full_name, g.phone, g.vip, COUNT(r.id),
                       COALESCE(SUM(CASE WHEN r.status IN ('ARRIVED','DEPARTED') THEN 1 ELSE 0 END), 0)
                FROM guests g LEFT JOIN stay_bookings r ON r.guest_id=g.id
                GROUP BY g.id ORDER BY g.full_name
                """);
                var rs = ps.executeQuery()) {
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(List.of(rs.getString(1), rs.getString(2) == null ? "" : rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getString(5)));
            }
            return rows;
        }
    }

    public List<List<String>> roomPerformance(Connection connection, LocalDate from, LocalDate to) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT rm.unit_code, t.name, r.nights, r.total, r.status
                FROM stay_bookings r
                JOIN lodges rm ON rm.id=r.lodge_id
                JOIN lodge_categories t ON t.id=rm.lodge_category_id
                WHERE r.status <> 'CANCELLED' AND r.check_in <= ? AND r.check_out > ?
                """ + scope("r.resort_id") + " ORDER BY rm.unit_code")) {
            ps.setString(1, DateUtils.iso(to));
            ps.setString(2, DateUtils.iso(from));
            try (var rs = ps.executeQuery()) {
                Map<String, int[]> nights = new LinkedHashMap<>();
                Map<String, BigDecimal> money = new LinkedHashMap<>();
                Map<String, String> type = new LinkedHashMap<>();
                while (rs.next()) {
                    String number = rs.getString(1);
                    type.put(number, rs.getString(2));
                    nights.merge(number, new int[] {rs.getInt(3)}, (a, b) -> {
                        a[0] += b[0];
                        return a;
                    });
                    money.merge(number, Jdbc.money(rs, "total"), BigDecimal::add);
                }
                List<List<String>> rows = new ArrayList<>();
                for (var entry : nights.entrySet()) {
                    rows.add(List.of(entry.getKey(), type.get(entry.getKey()),
                            String.valueOf(entry.getValue()[0]), CurrencyUtils.formatPlain(money.get(entry.getKey()))));
                }
                return rows;
            }
        }
    }

    public List<List<String>> inventory(Connection connection) throws SQLException {
        try (var ps = connection.prepareStatement(
                "SELECT sku, name, category, quantity, min_quantity, unit FROM inventory_items WHERE 1=1"
                        + scope("resort_id") + " ORDER BY name");
                var rs = ps.executeQuery()) {
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                int qty = rs.getInt(4);
                int min = rs.getInt(5);
                rows.add(List.of(rs.getString(1), rs.getString(2), rs.getString(3),
                        String.valueOf(qty), String.valueOf(min), qty < min ? "LOW" : "OK",
                        rs.getString(6) == null ? "" : rs.getString(6)));
            }
            return rows;
        }
    }

    public List<List<String>> attendance(Connection connection, LocalDate from, LocalDate to) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT e.name, e.department, a.work_date, a.status
                FROM employee_attendance a JOIN employees e ON e.id=a.employee_id
                WHERE a.work_date >= ? AND a.work_date <= ?
                """ + scope("e.resort_id") + " ORDER BY a.work_date, e.name")) {
            ps.setString(1, DateUtils.iso(from));
            ps.setString(2, DateUtils.iso(to));
            try (var rs = ps.executeQuery()) {
                List<List<String>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(List.of(rs.getString(1), rs.getString(2), DateUtils.display(rs.getString(3)), rs.getString(4)));
                }
                return rows;
            }
        }
    }

    public List<Integer> occupancyByDay(Connection connection, YearMonth month) throws SQLException {
        int length = month.lengthOfMonth();
        int[] sold = new int[length];
        int sellable;
        try (var ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM lodges WHERE status <> 'CLOSED'" + scope("resort_id"));
                var rs = ps.executeQuery()) {
            rs.next();
            sellable = rs.getInt(1);
        }
        try (var ps = connection.prepareStatement("""
                SELECT check_in, check_out FROM stay_bookings
                WHERE status IN ('CONFIRMED','ARRIVED','DEPARTED')
                """ + scope("resort_id"));
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate in = DateUtils.parse(rs.getString(1));
                LocalDate out = DateUtils.parse(rs.getString(2));
                for (int day = 1; day <= length; day++) {
                    LocalDate date = month.atDay(day);
                    if (!date.isBefore(in) && date.isBefore(out)) {
                        sold[day - 1]++;
                    }
                }
            }
        }
        List<Integer> percents = new ArrayList<>();
        for (int value : sold) {
            percents.add(sellable == 0 ? 0 : (int) Math.round(value * 100.0 / sellable));
        }
        return percents;
    }

    private BigDecimal sumPayments(Connection connection, String from, String to) throws SQLException {
        BigDecimal sum = Money.zero();
        try (var ps = connection.prepareStatement("""
                SELECT p.amount FROM payments p
                JOIN invoices i ON i.id=p.invoice_id
                JOIN stay_bookings r ON r.id=i.reservation_id
                WHERE substr(p.paid_at,1,10) >= ? AND substr(p.paid_at,1,10) <= ?
                """ + scope("r.resort_id"))) {
            ps.setString(1, from);
            ps.setString(2, to);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    sum = sum.add(Jdbc.money(rs, "amount"));
                }
            }
        }
        return sum;
    }

    private List<String[]> stays(Connection connection, String dateClause, String day, String statusClause)
            throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT r.booking_reference, g.full_name, rm.unit_code, r.status
                FROM stay_bookings r
                JOIN guests g ON g.id=r.guest_id
                JOIN lodges rm ON rm.id=r.lodge_id
                WHERE %s AND %s %s
                ORDER BY g.full_name
                """.formatted(dateClause, statusClause, scope("r.resort_id")))) {
            ps.setString(1, day);
            try (var rs = ps.executeQuery()) {
                List<String[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
                }
                return rows;
            }
        }
    }

    /** Limits a query to the switcher. All resorts keeps only active properties. */
    private static String scope(String column) {
        Long filter = Session.resortFilter();
        if (filter != null) {
            return " AND " + column + " = " + filter.longValue();
        }
        return " AND " + column + " IN (SELECT id FROM resorts WHERE active = 1)";
    }
}
