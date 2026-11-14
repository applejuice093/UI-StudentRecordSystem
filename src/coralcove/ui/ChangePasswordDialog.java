package coralcove.ui;

import coralcove.model.User;
import coralcove.service.AuthenticationService;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public final class ChangePasswordDialog extends JDialog {
    private boolean changed;

    private ChangePasswordDialog(java.awt.Frame owner, User user, boolean forced) {
        super(owner, "Change password", true);
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        form.setBackground(Theme.CONTENT);
        JPasswordField current = new JPasswordField(18);
        JPasswordField next = new JPasswordField(18);
        JPasswordField confirm = new JPasswordField(18);
        form.add(new JLabel("Current password"), Ui.constraints(0, 0));
        form.add(current, Ui.constraints(0, 1));
        form.add(new JLabel("New password"), Ui.constraints(1, 0));
        form.add(next, Ui.constraints(1, 1));
        form.add(new JLabel("Confirm password"), Ui.constraints(2, 0));
        form.add(confirm, Ui.constraints(2, 1));
        if (forced) {
            JLabel note = new JLabel("Change the default password before continuing.");
            note.setForeground(Theme.DANGER);
            form.add(note, Ui.constraints(3, 0));
        }
        JButton save = Ui.primary("Save password");
        save.addActionListener(event -> {
            String a = new String(next.getPassword());
            String b = new String(confirm.getPassword());
            if (!a.equals(b)) {
                Ui.error(this, "The new passwords do not match.");
                return;
            }
            Ui.run(this, () -> {
                new AuthenticationService().changePassword(user.getId(), new String(current.getPassword()), a);
                return true;
            }, result -> {
                changed = true;
                dispose();
            });
        });
        JPanel south = new JPanel();
        south.setBackground(Theme.CONTENT);
        south.add(save);
        if (!forced) {
            JButton cancel = Ui.secondary("Cancel");
            cancel.addActionListener(event -> dispose());
            south.add(cancel);
        }
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        setDefaultCloseOperation(forced ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(owner);
    }

    public static boolean show(java.awt.Frame owner, User user, boolean forced) {
        ChangePasswordDialog dialog = new ChangePasswordDialog(owner, user, forced);
        dialog.setVisible(true);
        return dialog.changed;
    }
}
