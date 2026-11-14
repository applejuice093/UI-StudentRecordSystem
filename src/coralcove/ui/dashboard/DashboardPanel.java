package coralcove.ui.dashboard;

import coralcove.service.DashboardService;
import coralcove.service.Session;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.StatCard;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class DashboardPanel extends JPanel implements Refreshable {
    private final StatCard total = new StatCard("Total lodges");
    private final StatCard available = new StatCard("Available");
    private final StatCard occupied = new StatCard("Occupied");
    private final StatCard arrivals = new StatCard("Arrivals today");
    private final StatCard departures = new StatCard("Departures today");
    private final StatCard revenue = new StatCard("Revenue today");
    private final StatCard reserved = new StatCard("Reserved");
    private final StatCard guests = new StatCard("Registered guests");
    private final DataTable arrivalTable = new DataTable("Reference", "Guest", "Lodge", "Status");
    private final DataTable departureTable = new DataTable("Reference", "Guest", "Lodge", "Status");
    private final DataTable recent = new DataTable("Reference", "Guest", "Lodge", "Check-in", "Status", "Total");
    private final DataTable payments = new DataTable("When", "Invoice", "Reference", "Method", "Amount");
    private final DataTable tasks = new DataTable("Lodge", "Priority", "Status", "Assignee");
    private final DataTable alerts = new DataTable("Id", "Lodge", "Issue", "Status", "Opened");
    private final JPanel charts = new JPanel(new GridLayout(2, 2, 12, 12));

    public DashboardPanel() {
        super(new BorderLayout(0, 12));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel cards = new JPanel(new GridLayout(2, 4, 12, 12));
        cards.setOpaque(false);
        for (StatCard card : new StatCard[] {total, available, occupied, reserved, arrivals, departures, revenue, guests}) {
            cards.add(card);
        }
        charts.setOpaque(false);
        JPanel lists = new JPanel(new GridLayout(2, 3, 12, 12));
        lists.setOpaque(false);
        lists.add(block("Today's arrivals", arrivalTable));
        lists.add(block("Today's departures", departureTable));
        lists.add(block("Recent stay_bookings", recent));
        lists.add(block("Recent transactions", payments));
        lists.add(block("Pending housekeeping", tasks));
        lists.add(block("Maintenance alerts", alerts));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);
        body.add(charts, BorderLayout.CENTER);
        body.add(lists, BorderLayout.SOUTH);
        add(new JScrollPane(body), BorderLayout.CENTER);
        Session.onDataChanged(() -> SwingUtilities.invokeLater(this::refresh));
    }

    private JPanel block(String title, DataTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        Ui.cardBorder(panel);
        JLabel label = new JLabel(title);
        label.setFont(Theme.SECTION);
        panel.add(label, BorderLayout.NORTH);
        table.setPreferredSize(new java.awt.Dimension(280, 160));
        panel.add(table, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refresh() {
        Ui.run(this, () -> new DashboardService().load(), result -> {
            DashboardService.Snapshot snapshot = (DashboardService.Snapshot) result;
            total.setValue(String.valueOf(snapshot.totalRooms));
            available.setValue(String.valueOf(snapshot.available));
            occupied.setValue(String.valueOf(snapshot.occupied));
            reserved.setValue(String.valueOf(snapshot.reserved));
            arrivals.setValue(String.valueOf(snapshot.arrivals.size()));
            departures.setValue(String.valueOf(snapshot.departures.size()));
            revenue.setValue(snapshot.revenueLabel());
            guests.setValue(String.valueOf(snapshot.guests));
            arrivalTable.setRows(snapshot.arrivals);
            departureTable.setRows(snapshot.departures);
            recent.setRows(snapshot.recentReservations);
            payments.setRows(snapshot.recentPayments);
            tasks.setRows(snapshot.housekeeping);
            alerts.setRows(snapshot.maintenanceAlerts);
            charts.removeAll();
            charts.add(chart("Monthly revenue", category(snapshot.monthlyRevenue)));
            charts.add(chart("Bookings", counts(snapshot.bookings)));
            charts.add(occupancy(snapshot));
            charts.add(pie(snapshot.roomTypes));
            charts.revalidate();
            charts.repaint();
        });
    }

    private DefaultCategoryDataset category(Map<String, java.math.BigDecimal> series) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        series.forEach((key, value) -> dataset.addValue(value, "Revenue", key));
        return dataset;
    }

    private DefaultCategoryDataset counts(Map<String, Integer> series) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        series.forEach((key, value) -> dataset.addValue(value, "Bookings", key));
        return dataset;
    }

    private ChartPanel chart(String title, DefaultCategoryDataset dataset) {
        var chart = ChartFactory.createBarChart(title, "", "", dataset, PlotOrientation.VERTICAL, false, true, false);
        return new ChartPanel(chart);
    }

    private ChartPanel occupancy(DashboardService.Snapshot snapshot) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < snapshot.occupancy.size(); i++) {
            dataset.addValue(snapshot.occupancy.get(i), "Occupancy %", String.valueOf(i + 1));
        }
        var chart = ChartFactory.createLineChart("Lodge occupancy this month", "Day", "%", dataset,
                PlotOrientation.VERTICAL, false, true, false);
        return new ChartPanel(chart);
    }

    private ChartPanel pie(Map<String, Integer> types) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        types.forEach(dataset::setValue);
        return new ChartPanel(ChartFactory.createPieChart("Lodge type distribution", dataset, true, true, false));
    }
}
