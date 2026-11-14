package coralcove.service;

import coralcove.dao.LodgeCareDAO;
import coralcove.dao.ReportDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.AppModule;
import coralcove.util.AppException;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public final class DashboardService {

    private final ReportDAO reports = new ReportDAO();

    public Snapshot load() {
        AuthorizationService.require(AppModule.DASHBOARD);
        try (var connection = DatabaseManager.getConnection()) {
            Snapshot snapshot = new Snapshot();
            snapshot.totalRooms = reports.countRooms(connection, null);
            snapshot.available = reports.countRooms(connection, "READY");
            snapshot.occupied = reports.countRooms(connection, "IN_STAY");
            snapshot.reserved = reports.countRooms(connection, "HELD");
            snapshot.maintenance = reports.countRooms(connection, "REPAIR");
            snapshot.guests = reports.countGuests(connection);
            snapshot.arrivals = reports.arrivals(connection, DateUtils.today());
            snapshot.departures = reports.departures(connection, DateUtils.today());
            snapshot.revenueToday = reports.paymentsOn(connection, DateUtils.today());
            snapshot.recentReservations = reports.recentReservations(connection);
            snapshot.recentPayments = reports.recentPayments(connection);
            snapshot.housekeeping = new LodgeCareDAO().search(connection, null, null).stream()
                    .filter(task -> !"COMPLETED".equals(task.getStatus()))
                    .limit(8)
                    .map(task -> new String[] {
                            task.getUnitCode(), task.getPriority(), task.getStatus(),
                            task.getAssigneeName() == null ? "" : task.getAssigneeName()
                    })
                    .toList();
            snapshot.maintenanceAlerts = new LodgeCareDAO().openMaintenance(connection);
            snapshot.monthlyRevenue = reports.monthlyRevenue(connection, 6);
            snapshot.bookings = reports.bookingsByMonth(connection, 6);
            snapshot.roomTypes = reports.roomsByType(connection);
            snapshot.occupancy = reports.occupancyByDay(connection, YearMonth.from(DateUtils.today()));
            return snapshot;
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public static final class Snapshot {
        public int totalRooms;
        public int available;
        public int occupied;
        public int reserved;
        public int maintenance;
        public int guests;
        public BigDecimal revenueToday = BigDecimal.ZERO;
        public List<String[]> arrivals = List.of();
        public List<String[]> departures = List.of();
        public List<String[]> recentReservations = List.of();
        public List<String[]> recentPayments = List.of();
        public List<String[]> housekeeping = List.of();
        public List<String[]> maintenanceAlerts = List.of();
        public Map<String, BigDecimal> monthlyRevenue = Map.of();
        public Map<String, Integer> bookings = Map.of();
        public Map<String, Integer> roomTypes = Map.of();
        public List<Integer> occupancy = List.of();

        public String revenueLabel() {
            return CurrencyUtils.format(revenueToday);
        }
    }
}
