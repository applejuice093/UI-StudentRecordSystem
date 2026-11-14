package coralcove.model;

public enum LodgeStatus {
    READY, IN_STAY, HELD, TURNDOWN, REPAIR, CLOSED;

    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
    }

    public static LodgeStatus parse(String raw) {
        return LodgeStatus.valueOf(raw.trim().toUpperCase());
    }
}
