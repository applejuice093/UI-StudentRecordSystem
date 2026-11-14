package coralcove.ui.components;

import coralcove.model.AppModule;
import coralcove.service.AuthorizationService;
import coralcove.service.Session;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class Sidebar extends JPanel {
    private final Map<AppModule, JButton> buttons = new LinkedHashMap<>();

    public Sidebar(Consumer<AppModule> onSelect) {
        setLayout(new BorderLayout());
        setBackground(Theme.NAVY);
        setPreferredSize(new Dimension(245, 0));
        JLabel brand = new JLabel("CoralCove");
        brand.setFont(Theme.TITLE);
        brand.setForeground(Theme.GOLD);
        brand.setBorder(new EmptyBorder(20, 18, 8, 18));
        JLabel tag = new JLabel("Resort group");
        tag.setForeground(new java.awt.Color(0xD9D3C7));
        tag.setFont(Theme.SMALL);
        tag.setBorder(new EmptyBorder(0, 18, 16, 18));
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY);
        header.add(brand, BorderLayout.NORTH);
        header.add(tag, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 2));
        nav.setBackground(Theme.NAVY);
        nav.setBorder(new EmptyBorder(0, 10, 10, 10));
        for (AppModule module : AppModule.values()) {
            if (!AuthorizationService.allows(Session.get().getRole(), module)) {
                continue;
            }
            JButton button = new JButton(module.label());
            button.setHorizontalAlignment(JButton.LEFT);
            button.setFont(Theme.BODY);
            button.setForeground(java.awt.Color.WHITE);
            button.setBackground(Theme.NAVY);
            button.setBorder(new EmptyBorder(8, 10, 8, 10));
            button.addActionListener(event -> {
                highlight(button);
                onSelect.accept(module);
            });
            buttons.put(module, button);
            nav.add(button);
        }
        add(nav, BorderLayout.CENTER);
        if (!buttons.isEmpty()) {
            highlight(buttons.values().iterator().next());
        }
    }

    public void highlight(AppModule module) {
        JButton button = buttons.get(module);
        if (button != null) {
            highlight(button);
        }
    }

    private void highlight(JButton selected) {
        for (JButton button : buttons.values()) {
            button.setBackground(button == selected ? Theme.GOLD_DARK : Theme.NAVY);
        }
    }
}
