package coralcove.ui.reports;

import coralcove.service.ReportService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CSVExporter;
import coralcove.util.DateUtils;
import coralcove.util.PDFGenerator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;

public class ReportsPanel extends JPanel implements Refreshable {
    private static final String[] KINDS = {
            "Daily revenue", "Monthly revenue", "Yearly revenue", "Occupancy rate", "Stay trends",
            "Cancellation rate", "Guest history", "Outstanding payments", "Lodge performance",
            "Inventory status", "Employee attendance"
    };
    private final ReportService service = new ReportService();
    private final JComboBox<String> kind = new JComboBox<>(KINDS);
    private final JSpinner from = Ui.dateSpinner(DateUtils.today().withDayOfMonth(1));
    private final JSpinner to = Ui.dateSpinner(DateUtils.today());
    private final JTextArea note = new JTextArea(3, 40);
    private DataTable table = new DataTable("Result");
    private ReportService.Table last;

    public ReportsPanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setBackground(Theme.CONTENT);
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(kind);
        north.add(new JLabel("From"));
        north.add(from);
        north.add(new JLabel("To"));
        north.add(to);
        var run = Ui.primary("Run report");
        run.addActionListener(event -> refresh());
        var csv = Ui.secondary("Export CSV");
        csv.addActionListener(event -> export());
        var print = Ui.secondary("Print");
        print.addActionListener(event -> print());
        north.add(run);
        north.add(csv);
        north.add(print);
        add(north, BorderLayout.NORTH);
        add(note, BorderLayout.SOUTH);
        add(table, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.run(this, () -> service.run((String) kind.getSelectedItem(), Ui.date(from), Ui.date(to)), result -> {
            last = (ReportService.Table) result;
            note.setText(last.note());
            remove(table);
            table = new DataTable(last.columns().toArray(String[]::new));
            List<String[]> rows = new ArrayList<>();
            for (List<String> row : last.rows()) {
                rows.add(row.toArray(String[]::new));
            }
            table.setRows(rows);
            add(table, BorderLayout.CENTER);
            revalidate();
            repaint();
        });
    }

    private void export() {
        if (last == null) {
            Ui.error(this, "Run a report first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(last.title().replace(' ', '-') + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            CSVExporter.write(Path.of(chooser.getSelectedFile().getAbsolutePath()), last.columns(), last.rows());
            Ui.info(this, "Exported " + last.rows().size() + " row(s).");
        }
    }

    private void print() {
        if (last == null) {
            Ui.error(this, "Run a report first.");
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add(last.note());
        lines.add(String.join(" | ", last.columns()));
        for (List<String> row : last.rows()) {
            lines.add(String.join(" | ", row));
        }
        PDFGenerator.print(last.title(), lines);
    }
}
