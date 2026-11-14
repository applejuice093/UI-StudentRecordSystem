package coralcove.ui.components;

import java.awt.Color;
import java.awt.Font;

public final class Theme {
    public static final Color NAVY = new Color(0x1B2430);
    public static final Color NAVY_DARK = new Color(0x141A22);
    public static final Color GOLD = new Color(0xC4A35A);
    public static final Color GOLD_DARK = new Color(0x8C7338);
    public static final Color CONTENT = new Color(0xF7F4EF);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x1F2933);
    public static final Color MUTED = new Color(0x5C6B73);
    public static final Color LINE = new Color(0xE4DDD2);
    public static final Color DANGER = new Color(0x9B2335);
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font SECTION = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private Theme() {
    }

    public static Color status(String name) {
        return switch (name) {
            case "READY" -> new Color(0x1F7A4D);
            case "IN_STAY" -> new Color(0x9B2335);
            case "HELD" -> new Color(0xC47B17);
            case "TURNDOWN" -> new Color(0x1D4E89);
            case "REPAIR" -> new Color(0x6B7280);
            case "CLOSED" -> new Color(0x374151);
            default -> NAVY;
        };
    }
}
