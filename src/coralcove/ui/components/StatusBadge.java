package coralcove.ui.components;

import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class StatusBadge extends JPanel {
    private final JLabel label = new JLabel();

    public StatusBadge(String status) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(2, 8, 2, 8));
        label.setFont(Theme.SMALL);
        label.setForeground(Color.WHITE);
        add(label);
        setStatus(status);
    }

    public void setStatus(String status) {
        label.setText(status == null ? "" : status.replace('_', ' '));
        label.setBackground(Theme.status(status));
    }
}
