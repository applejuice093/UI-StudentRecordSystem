package coralcove.ui.components;

import coralcove.model.Lodge;
import coralcove.util.CurrencyUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class LodgeCard extends JPanel {
    public LodgeCard(Lodge room) {
        setLayout(new BorderLayout(0, 4));
        setPreferredSize(new Dimension(160, 110));
        Color color = Theme.status(room.getStatus().name());
        setBackground(Color.WHITE);
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 6, 0, 0, color),
                new EmptyBorder(8, 10, 8, 10)));
        JLabel number = new JLabel(room.getUnitCode());
        number.setFont(Theme.SECTION);
        JLabel type = new JLabel(room.getRoomTypeName());
        type.setFont(Theme.SMALL);
        type.setForeground(Theme.MUTED);
        JLabel price = new JLabel(CurrencyUtils.format(room.getPricePerNight()));
        price.setFont(Theme.BODY);
        JLabel status = new JLabel(room.getStatus().label());
        status.setForeground(color);
        status.setFont(Theme.SMALL);
        if (room.isSample()) {
            status.setText(status.getText() + " · Sample");
        }
        JPanel text = new JPanel(new java.awt.GridLayout(4, 1));
        text.setOpaque(false);
        text.add(number);
        text.add(type);
        text.add(price);
        text.add(status);
        add(text, BorderLayout.CENTER);
    }
}
