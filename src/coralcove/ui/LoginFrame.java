package coralcove.ui;

import coralcove.config.AppConfig;
import coralcove.service.AuthenticationService;
import coralcove.service.Session;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginFrame extends JFrame {
    private final JTextField username = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);
    private final JCheckBox remember = new JCheckBox("Remember username");
    private final JLabel message = new JLabel(" ");

    public LoginFrame() {
        super(AppConfig.WINDOW_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 520));
        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(Theme.NAVY);
        brand.setPreferredSize(new Dimension(320, 0));
        JLabel name = new JLabel("CoralCove");
        name.setFont(Theme.TITLE);
        name.setForeground(Theme.GOLD);
        name.setBorder(javax.swing.BorderFactory.createEmptyBorder(40, 28, 8, 28));
        JLabel line = new JLabel("<html>Resort group desk for villas,<br>arrivals, and experiences.</html>");
        line.setForeground(java.awt.Color.WHITE);
        line.setFont(Theme.BODY);
        line.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 28, 28, 28));
        brand.add(name, BorderLayout.NORTH);
        brand.add(line, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CONTENT);
        username.setFont(Theme.BODY);
        password.setFont(Theme.BODY);
        username.setText(PreferencesStore.rememberedUsername());
        remember.setSelected(!username.getText().isBlank());
        remember.setOpaque(false);
        message.setForeground(Theme.DANGER);
        form.add(Ui.heading("Sign in"), Ui.constraints(0, 0));
        form.add(new JLabel("Username"), Ui.constraints(1, 0));
        form.add(username, Ui.constraints(1, 1));
        form.add(new JLabel("Password"), Ui.constraints(2, 0));
        form.add(password, Ui.constraints(2, 1));
        form.add(remember, Ui.constraints(3, 1));
        JButton enter = Ui.primary("Sign in");
        enter.addActionListener(event -> submit());
        password.addActionListener(event -> submit());
        form.add(enter, Ui.constraints(4, 1));
        form.add(message, Ui.constraints(5, 1));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(brand, BorderLayout.WEST);
        getContentPane().add(form, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    private void submit() {
        message.setText(" ");
        String name = username.getText();
        String secret = new String(password.getPassword());
        Ui.run(this, () -> new AuthenticationService().login(name, secret), user -> {
            Session.set((coralcove.model.User) user);
            if (remember.isSelected()) {
                PreferencesStore.remember(name);
            } else {
                PreferencesStore.remember(null);
            }
            if (((coralcove.model.User) user).isMustChangePassword()) {
                boolean changed = ChangePasswordDialog.show(this, (coralcove.model.User) user, true);
                if (!changed) {
                    Session.clear();
                    message.setText("Password change is required.");
                    return;
                }
            }
            dispose();
            new MainFrame().setVisible(true);
        });
    }
}
