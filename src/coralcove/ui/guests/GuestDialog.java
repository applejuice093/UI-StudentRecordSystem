package coralcove.ui.guests;

import coralcove.model.Guest;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class GuestDialog extends JDialog {
    private Guest result;

    private GuestDialog(java.awt.Frame owner, Guest existing) {
        super(owner, existing == null ? "Register guest" : "Edit guest", true);
        JTextField name = field(existing == null ? "" : existing.getFullName());
        JTextField phone = field(existing == null ? "" : existing.getPhone());
        JTextField email = field(existing == null ? "" : existing.getEmail());
        JComboBox<String> gender = new JComboBox<>(new String[] {"", "Female", "Male", "Other"});
        if (existing != null && existing.getGender() != null) {
            gender.setSelectedItem(existing.getGender());
        }
        JTextField nationality = field(existing == null ? "India" : existing.getNationality());
        JTextField address = field(existing == null ? "" : existing.getAddress());
        JComboBox<String> docType = new JComboBox<>(new String[] {"", "Aadhaar", "Passport", "Driving licence", "Other"});
        if (existing != null && existing.getIdDocType() != null) {
            docType.setSelectedItem(existing.getIdDocType());
        }
        JTextField docNo = field(existing == null ? "" : existing.getIdDocNumber());
        JTextField preferences = field(existing == null ? "" : existing.getPreferences());
        JComboBox<String> vip = new JComboBox<>(new String[] {"STANDARD", "VIP"});
        if (existing != null) {
            vip.setSelectedItem(existing.getVip());
        }
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CONTENT);
        addRow(form, 0, "Full name", name);
        addRow(form, 1, "Phone", phone);
        addRow(form, 2, "Email", email);
        addRow(form, 3, "Gender", gender);
        addRow(form, 4, "Nationality", nationality);
        addRow(form, 5, "Address", address);
        addRow(form, 6, "ID type", docType);
        addRow(form, 7, "ID number", docNo);
        addRow(form, 8, "Preferences", preferences);
        addRow(form, 9, "Classification", vip);
        var save = Ui.primary("Save");
        save.addActionListener(event -> {
            Guest guest = existing == null ? new Guest() : existing;
            guest.setFullName(name.getText());
            guest.setPhone(phone.getText());
            guest.setEmail(email.getText());
            guest.setGender((String) gender.getSelectedItem());
            guest.setNationality(nationality.getText());
            guest.setAddress(address.getText());
            guest.setIdDocType((String) docType.getSelectedItem());
            guest.setIdDocNumber(docNo.getText());
            guest.setPreferences(preferences.getText());
            guest.setVip((String) vip.getSelectedItem());
            result = guest;
            dispose();
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

    public static Guest show(java.awt.Frame owner, Guest existing) {
        GuestDialog dialog = new GuestDialog(owner, existing);
        dialog.setVisible(true);
        return dialog.result;
    }

    private static JTextField field(String value) {
        return new JTextField(value == null ? "" : value, 20);
    }

    private static void addRow(JPanel form, int row, String label, java.awt.Component field) {
        form.add(new JLabel(label), Ui.constraints(row, 0));
        form.add(field, Ui.constraints(row, 1));
    }
}
