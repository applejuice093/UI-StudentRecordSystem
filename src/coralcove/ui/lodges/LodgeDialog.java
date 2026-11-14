package coralcove.ui.lodges;

import coralcove.model.Lodge;
import coralcove.model.LodgeStatus;
import coralcove.model.LodgeCategory;
import coralcove.model.Resort;
import coralcove.service.Session;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.Money;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class LodgeDialog extends JDialog {
    private static final String[] ZONES = {"BEACH", "LAGOON", "GARDEN", "CLIFF"};
    private static final String[] VIEWS = {"SEA", "GARDEN", "POOL"};
    private Lodge result;

    private LodgeDialog(java.awt.Frame owner, Lodge existing, List<LodgeCategory> types, List<Resort> resorts) {
        super(owner, existing == null ? "Add lodge" : "Edit lodge", true);
        JComboBox<Resort> resort = new JComboBox<>(resorts.toArray(Resort[]::new));
        JTextField number = new JTextField(existing == null ? "" : existing.getUnitCode(), 14);
        JComboBox<String> zone = new JComboBox<>(ZONES);
        JComboBox<String> view = new JComboBox<>(VIEWS);
        JComboBox<LodgeCategory> type = new JComboBox<>(types.toArray(LodgeCategory[]::new));
        JTextField beds = new JTextField(existing == null ? "1 King" : existing.getBedConfig(), 14);
        JTextField guests = new JTextField(existing == null ? "2" : String.valueOf(existing.getMaxGuests()), 14);
        JTextField price = new JTextField(existing == null ? "" : existing.getPricePerNight().toPlainString(), 14);
        JTextArea description = new JTextArea(existing == null ? "" : nullToEmpty(existing.getDescription()), 3, 18);
        JTextField amenities = new JTextField(existing == null ? "" : nullToEmpty(existing.getAmenities()), 14);
        JComboBox<LodgeStatus> status = new JComboBox<>(LodgeStatus.values());
        if (existing != null) {
            status.setSelectedItem(existing.getStatus());
            zone.setSelectedItem(existing.getZone());
            view.setSelectedItem(existing.getViewAspect());
            for (int i = 0; i < type.getItemCount(); i++) {
                if (type.getItemAt(i).getId() == existing.getLodgeCategoryId()) {
                    type.setSelectedIndex(i);
                }
            }
            for (int i = 0; i < resort.getItemCount(); i++) {
                if (resort.getItemAt(i).getId() == existing.getResortId()) {
                    resort.setSelectedIndex(i);
                }
            }
        } else if (Session.resortFilter() != null) {
            for (int i = 0; i < resort.getItemCount(); i++) {
                if (resort.getItemAt(i).getId() == Session.resortFilter()) {
                    resort.setSelectedIndex(i);
                }
            }
        }
        boolean pickResort = Session.resortFilter() == null;
        resort.setEnabled(pickResort);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CONTENT);
        int row = 0;
        if (pickResort) {
            form.add(label("Resort"), Ui.constraints(row, 0));
            form.add(resort, Ui.constraints(row++, 1));
        }
        form.add(label("Unit code"), Ui.constraints(row, 0));
        form.add(number, Ui.constraints(row++, 1));
        form.add(label("Zone"), Ui.constraints(row, 0));
        form.add(zone, Ui.constraints(row++, 1));
        form.add(label("View"), Ui.constraints(row, 0));
        form.add(view, Ui.constraints(row++, 1));
        form.add(label("Category"), Ui.constraints(row, 0));
        form.add(type, Ui.constraints(row++, 1));
        form.add(label("Beds"), Ui.constraints(row, 0));
        form.add(beds, Ui.constraints(row++, 1));
        form.add(label("Max guests"), Ui.constraints(row, 0));
        form.add(guests, Ui.constraints(row++, 1));
        form.add(label("Price / night (INR)"), Ui.constraints(row, 0));
        form.add(price, Ui.constraints(row++, 1));
        form.add(label("Amenities"), Ui.constraints(row, 0));
        form.add(amenities, Ui.constraints(row++, 1));
        form.add(label("Description"), Ui.constraints(row, 0));
        form.add(description, Ui.constraints(row++, 1));
        form.add(label("Status"), Ui.constraints(row, 0));
        form.add(status, Ui.constraints(row, 1));
        var save = Ui.primary("Save");
        save.addActionListener(event -> {
            try {
                if (resort.getSelectedItem() == null) {
                    throw new IllegalArgumentException("Choose a resort before saving.");
                }
                Lodge room = existing == null ? new Lodge() : existing;
                room.setResortId(((Resort) resort.getSelectedItem()).getId());
                room.setUnitCode(number.getText());
                room.setZone(String.valueOf(zone.getSelectedItem()));
                room.setViewAspect(String.valueOf(view.getSelectedItem()));
                room.setLodgeCategoryId(((LodgeCategory) type.getSelectedItem()).getId());
                room.setBedConfig(beds.getText());
                room.setMaxGuests(Integer.parseInt(guests.getText().trim()));
                room.setPricePerNight(Money.of(price.getText()));
                room.setAmenities(amenities.getText());
                room.setDescription(description.getText());
                room.setStatus((LodgeStatus) status.getSelectedItem());
                result = room;
                dispose();
            } catch (RuntimeException ex) {
                Ui.error(this, ex.getMessage() == null ? "Check the lodge fields." : ex.getMessage());
            }
        });
        JPanel south = new JPanel();
        south.add(save);
        var cancel = Ui.secondary("Cancel");
        cancel.addActionListener(event -> dispose());
        south.add(cancel);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    public static Lodge show(java.awt.Frame owner, Lodge existing, List<LodgeCategory> types, List<Resort> resorts) {
        LodgeDialog dialog = new LodgeDialog(owner, existing, types, resorts);
        dialog.setVisible(true);
        return dialog.result;
    }

    private static javax.swing.JLabel label(String text) {
        return new javax.swing.JLabel(text);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
