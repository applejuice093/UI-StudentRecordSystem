package coralcove.model;

public enum BookingSource {
    WALK_IN("Walk-in"),
    WEBSITE("Website"),
    PHONE("Phone"),
    TRAVEL_AGENT("Travel agent");

    private final String label;

    BookingSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static BookingSource parse(String raw) {
        return BookingSource.valueOf(raw.trim().toUpperCase());
    }
}
