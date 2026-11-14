package coralcove.ui.experiences;

import coralcove.model.Lodge;
import coralcove.model.Experience;
import coralcove.model.ExperienceBooking;
import coralcove.service.ExperienceService;
import coralcove.service.LodgeService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CurrencyUtils;
import coralcove.util.Money;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ExperiencePanel extends JPanel implements Refreshable {
    private final ExperienceService service = new ExperienceService();
    private final DataTable menu = new DataTable("Id", "Category", "Item", "Price", "Active");
    private final DataTable orders = new DataTable("Id", "Type", "Lodge", "Item", "Qty", "Total", "Status", "Sample");
    private final JComboBox<String> status = new JComboBox<>(new String[] {"", "NEW", "PREPARING", "READY", "SERVED", "CANCELLED"});
    private List<Experience> items = List.of();
    private List<ExperienceBooking> orderRows = List.of();

    public ExperiencePanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        var addItem = Ui.secondary("Add menu item");
        addItem.addActionListener(event -> addItem());
        var order = Ui.primary("New order");
        order.addActionListener(event -> place());
        var advance = Ui.secondary("Update status");
        advance.addActionListener(event -> advance());
        north.add(status);
        var refresh = Ui.secondary("Refresh");
        refresh.addActionListener(event -> refresh());
        north.add(refresh);
        north.add(addItem);
        north.add(order);
        north.add(advance);
        add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.setOpaque(false);
        center.add(menu);
        center.add(orders);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.run(this, () -> new Object[] {service.menu(), service.orders((String) status.getSelectedItem())}, result -> {
            Object[] pair = (Object[]) result;
            items = (List<Experience>) pair[0];
            orderRows = (List<ExperienceBooking>) pair[1];
            List<String[]> menuRows = new ArrayList<>();
            for (Experience item : items) {
                menuRows.add(new String[] {
                        String.valueOf(item.getId()), item.getCategory(), item.getName(),
                        CurrencyUtils.format(item.getPrice()), item.isActive() ? "Yes" : "No"
                });
            }
            menu.setRows(menuRows);
            List<String[]> data = new ArrayList<>();
            for (ExperienceBooking row : orderRows) {
                data.add(new String[] {
                        String.valueOf(row.getId()), row.getOrderType(),
                        row.getUnitCode() == null ? "" : row.getUnitCode(),
                        row.getDescription(), String.valueOf(row.getQty()),
                        CurrencyUtils.format(row.getLineTotal()), row.getStatus(),
                        row.isSample() ? "Sample" : ""
                });
            }
            orders.setRows(data);
        });
    }

    private void addItem() {
        JTextField name = new JTextField(14);
        JTextField category = new JTextField("Food", 12);
        JTextField price = new JTextField("0.00", 8);
        JPanel form = new JPanel(new GridLayout(3, 2));
        form.add(new javax.swing.JLabel("Name"));
        form.add(name);
        form.add(new javax.swing.JLabel("Category"));
        form.add(category);
        form.add(new javax.swing.JLabel("Price"));
        form.add(price);
        if (JOptionPane.showConfirmDialog(this, form, "Menu item", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        Experience item = new Experience();
        item.setName(name.getText());
        item.setCategory(category.getText());
        item.setPrice(Money.of(price.getText()));
        Ui.run(this, () -> {
            service.saveItem(item, true);
            return null;
        }, result -> refresh());
    }

    private void place() {
        if (items.isEmpty()) {
            Ui.error(this, "Add a menu item first.");
            return;
        }
        JComboBox<Experience> item = new JComboBox<>(items.toArray(Experience[]::new));
        JTextField qty = new JTextField("1", 4);
        JComboBox<String> type = new JComboBox<>(new String[] {"RESTAURANT", "ROOM_SERVICE"});
        JComboBox<Lodge> room = new JComboBox<>();
        Ui.run(this, () -> new LodgeService().search(null, null, null, "IN_STAY"), result -> {
            for (Lodge value : (List<Lodge>) result) {
                room.addItem(value);
            }
            JPanel form = new JPanel(new GridLayout(4, 2));
            form.add(new javax.swing.JLabel("Item"));
            form.add(item);
            form.add(new javax.swing.JLabel("Qty"));
            form.add(qty);
            form.add(new javax.swing.JLabel("Type"));
            form.add(type);
            form.add(new javax.swing.JLabel("Occupied room"));
            form.add(room);
            if (JOptionPane.showConfirmDialog(this, form, "Order", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            Experience chosen = (Experience) item.getSelectedItem();
            Lodge chosenRoom = (Lodge) room.getSelectedItem();
            Ui.run(this, () -> {
                service.place(chosen.getId(), chosen.getName(), Integer.parseInt(qty.getText().trim()),
                        (String) type.getSelectedItem(), chosenRoom == null ? null : chosenRoom.getId());
                return null;
            }, ignored -> refresh());
        });
    }

    private void advance() {
        int row = orders.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select an order first.");
            return;
        }
        ExperienceBooking order = orderRows.get(row);
        JComboBox<String> next = new JComboBox<>(new String[] {"NEW", "PREPARING", "READY", "SERVED", "CANCELLED"});
        next.setSelectedItem(order.getStatus());
        if (JOptionPane.showConfirmDialog(this, next, "Order status", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        Ui.run(this, () -> {
            service.advance(order.getId(), (String) next.getSelectedItem());
            return null;
        }, result -> refresh());
    }
}
