package coralcove.service;

import coralcove.dao.ReportDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.AppModule;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class ReportService {

    public record Table(String title, String note, List<String> columns, List<List<String>> rows) {
    }

    private final ReportDAO reports = new ReportDAO();

    public Table run(String kind, LocalDate from, LocalDate to) {
        AuthorizationService.require(AppModule.REPORTS);
        if (from == null || to == null || to.isBefore(from)) {
            throw new AppException("Choose a date range that ends on or after the start date.");
        }
        try (var connection = DatabaseManager.getConnection()) {
            return switch (kind) {
                case "Daily revenue" -> new Table(kind, "Sum of stored payments by day.",
                        List.of("Day", "Amount"), reports.revenueReport(connection, from, to, "day"));
                case "Monthly revenue" -> new Table(kind, "Sum of stored payments by month.",
                        List.of("Month", "Amount"), reports.revenueReport(connection, from, to, "month"));
                case "Yearly revenue" -> new Table(kind, "Sum of stored payments by year.",
                        List.of("Year", "Amount"), reports.revenueReport(connection, from, to, "year"));
                case "Occupancy rate" -> occupancy(connection, from, to);
                case "Stay trends" -> new Table(kind, "Stays whose arrival date falls in the range.",
                        List.of("Month", "Bookings", "Cancellations", "Cancellation rate"),
                        reports.reservationTrend(connection, from, to));
                case "Cancellation rate" -> new Table(kind, "Cancellations divided by bookings with check-in in the range.",
                        List.of("Month", "Bookings", "Cancellations", "Cancellation rate"),
                        reports.reservationTrend(connection, from, to));
                case "Guest history" -> new Table(kind, "All guests. The date range is ignored.",
                        List.of("Guest", "Phone", "Class", "Reservations", "Stays"),
                        reports.guestHistory(connection));
                case "Outstanding payments" -> new Table(kind, "Invoices whose balance is not zero.",
                        List.of("Invoice", "Guest", "Total", "Paid", "Balance", "Status"),
                        reports.outstanding(connection));
                case "Lodge performance" -> new Table(kind, "Non-cancelled stay_bookings overlapping the range.",
                        List.of("Lodge", "Type", "Nights", "StayBooking total"),
                        reports.roomPerformance(connection, from, to));
                case "Inventory status" -> new Table(kind, "LOW means quantity is below the minimum. Sample rows are included and badged in the inventory screen.",
                        List.of("SKU", "Item", "Category", "Qty", "Minimum", "Alert", "Unit"),
                        reports.inventory(connection));
                case "Employee attendance" -> new Table(kind, "Attendance rows inside the range.",
                        List.of("Employee", "Department", "Date", "Status"),
                        reports.attendance(connection, from, to));
                default -> throw new AppException("Unknown report.");
            };
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    private Table occupancy(java.sql.Connection connection, LocalDate from, LocalDate to) throws SQLException {
        String[] values = reports.occupancy(connection, from, to);
        String note = "Sold room-nights (" + values[0] + ") divided by lodges that are not out of service, times days in "
                + DateUtils.display(from) + " to " + DateUtils.display(to) + " (" + values[1]
                + "). Counted statuses: CONFIRMED, ARRIVED, DEPARTED. Pending, cancelled, and no-show stays are excluded.";
        return new Table("Occupancy rate", note, List.of("Sold room-nights", "Available room-nights", "Occupancy"),
                List.of(List.of(values[0], values[1], values[2])));
    }
}
