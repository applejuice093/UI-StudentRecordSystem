package coralcove.ui.inventory;

import coralcove.model.InventoryItem;
import coralcove.service.InventoryService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class InventoryPanel extends JPanel implements Refreshable {
    private final InventoryService service = new InventoryService();
    private final JTextField search = new JTextField(12);
    private final JComboBox<String> category = new JComboBox<>();
    private final JLabel alert = new JLabel(" ");
    private final DataTable table = new DataTable("Id", "SKU", "Item", "Category", "Qty", "Minimum", "Alert", "Sample");
    private final DataTable history = new DataTable("When", "Reason", "Change", "Note");
    private List<InventoryItem> rows = List.of();

    public InventoryPanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        category.addItem("");
        for (String value : InventoryService.CATEGORIES) {
            category.addItem(value);
        }
        alert.setForeground(Theme.DANGER);
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(search);
        north.add(category);
        var find = Ui.secondary("Search");
        find.addActionListener(event -> refresh());
        var add = Ui.primary("Add item");
        add.addActionListener(event -> edit(null));
        var edit = Ui.secondary("Edit");
        edit.addActionListener(event -> {
            InventoryItem item = selected();
            if (item != null) {
                edit(item);
            }
        });
        var supplier = Ui.secondary("Add supplier");
        supplier.addActionListener(event -> addSupplier());
        var adjust = Ui.secondary("Adjust stock");
        adjust.addActionListener(event -> adjust());
        north.add(find);
        north.add(add);
        north.add(edit);
        north.add(supplier);
        north.add(adjust);
        north.add(alert);
        add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.setOpaque(false);
        center.add(table);
        center.add(history);
        add(center, BorderLayout.CENTER);
        table.table().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadHistory();
            }
        });
    }

    @Override
    public void refresh() {
        String chosen = (String) category.getSelectedItem();
        Ui.run(this, () -> service.search(search.getText(), chosen == null || chosen.isBlank() ? null : chosen), result -> {
            rows = (List<InventoryItem>) result;
            List<String[]> data = new ArrayList<>();
            int low = 0;
            for (InventoryItem item : rows) {
                if (item.isLow()) {
                    low++;
                }
                data.add(new String[] {
                        String.valueOf(item.getId()), item.getSku(), item.getName(), item.getCategory(),
                        String.valueOf(item.getQuantity()), String.valueOf(item.getMinQuantity()),
                        item.isLow() ? "LOW" : "OK", item.isSample() ? "Sample" : ""
                });
            }
            table.setRows(data);
            alert.setText(low == 0 ? "No low-stock items in this view." : low + " item(s) are below minimum.");
        });
    }

    private InventoryItem selected() {
        int row = table.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select an item first.");
            return null;
        }
        return rows.get(row);
    }

    private void loadHistory() {
        int row = table.selectedRow();
        if (row < 0) {
            return;
        }
        long id = rows.get(row).getId();
        Ui.run(this, () -> service.history(id), result -> history.setRows((List<String[]>) result));
    }

    private void edit(InventoryItem existing) {
        JTextField sku = new JTextField(existing == null ? "" : existing.getSku());
        JTextField name = new JTextField(existing == null ? "" : existing.getName());
        JComboBox<String> cat = new JComboBox<>(InventoryService.CATEGORIES.toArray(String[]::new));
        if (existing != null) {
            cat.setSelectedItem(existing.getCategory());
        }
        JTextField qty = new JTextField(existing == null ? "0" : String.valueOf(existing.getQuantity()));
        JTextField min = new JTextField(existing == null ? "0" : String.valueOf(existing.getMinQuantity()));
        JTextField unit = new JTextField(existing == null ? "pcs" : nullToEmpty(existing.getUnit()));
        JPanel form = new JPanel(new GridLayout(0, 2));
        form.add(new JLabel("SKU"));
        form.add(sku);
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Category"));
        form.add(cat);
        form.add(new JLabel("Quantity"));
        form.add(qty);
        form.add(new JLabel("Minimum"));
        form.add(min);
        form.add(new JLabel("Unit"));
        form.add(unit);
        if (JOptionPane.showConfirmDialog(this, form, "Inventory item", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        InventoryItem item = existing == null ? new InventoryItem() : existing;
        item.setSku(sku.getText());
        item.setName(name.getText());
        item.setCategory((String) cat.getSelectedItem());
        item.setQuantity(Integer.parseInt(qty.getText().trim()));
        item.setMinQuantity(Integer.parseInt(min.getText().trim()));
        item.setUnit(unit.getText());
        Ui.run(this, () -> service.save(item, existing == null), result -> refresh());
    }

    private void addSupplier() {
        JTextField name = new JTextField(16);
        JTextField phone = new JTextField(12);
        JTextField email = new JTextField(16);
        JPanel form = new JPanel(new GridLayout(3, 2));
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Phone"));
        form.add(phone);
        form.add(new JLabel("Email"));
        form.add(email);
        if (JOptionPane.showConfirmDialog(this, form, "Supplier", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            Ui.run(this, () -> service.addSupplier(name.getText(), phone.getText(), email.getText()), result ->
                    Ui.info(this, "Supplier saved. Assign it by editing an item later if needed."));
        }
    }

    private void adjust() {
        InventoryItem item = selected();
        if (item == null) {
            return;
        }
        JTextField delta = new JTextField(6);
        JComboBox<String> reason = new JComboBox<>(new String[] {"PURCHASE", "ADJUSTMENT", "CONSUMPTION"});
        JTextField note = new JTextField(12);
        JPanel form = new JPanel(new GridLayout(3, 2));
        form.add(new JLabel("Change (+/-)"));
        form.add(delta);
        form.add(new JLabel("Reason"));
        form.add(reason);
        form.add(new JLabel("Note"));
        form.add(note);
        if (JOptionPane.showConfirmDialog(this, form, "Stock " + item.getName(), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        int change = Integer.parseInt(delta.getText().trim());
        if ("CONSUMPTION".equals(reason.getSelectedItem()) && change > 0) {
            change = -change;
        }
        int applied = change;
        Ui.run(this, () -> {
            service.adjust(item.getId(), applied, (String) reason.getSelectedItem(), note.getText());
            return null;
        }, result -> refresh());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
