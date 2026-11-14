package coralcove.ui.components;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class DataTable extends JPanel {
    private final DefaultTableModel model;
    private final JTable table;

    public DataTable(String... columns) {
        super(new BorderLayout());
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFont(Theme.BODY);
        table.setRowHeight(28);
        table.getTableHeader().setFont(Theme.SMALL);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        setBackground(Theme.CONTENT);
    }

    public void setRows(List<String[]> rows) {
        model.setRowCount(0);
        for (String[] row : rows) {
            model.addRow(row);
        }
    }

    public int selectedRow() {
        int view = table.getSelectedRow();
        return view < 0 ? -1 : table.convertRowIndexToModel(view);
    }

    public String value(int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    public JTable table() {
        return table;
    }
}
