package coralcove.ui.components;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatCard extends JPanel {
    private final JLabel value = new JLabel("-");

    public StatCard(String title) {
        setLayout(new BorderLayout(0, 6));
        Ui.cardBorder(this);
        JLabel heading = new JLabel(title);
        heading.setFont(Theme.SMALL);
        heading.setForeground(Theme.MUTED);
        value.setFont(Theme.TITLE);
        value.setForeground(Theme.NAVY);
        add(heading, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
    }

    public void setValue(String text) {
        value.setText(text);
    }
}
