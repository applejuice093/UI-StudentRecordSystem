package coralcove.model;

public enum StayStatus {
    PENDING, CONFIRMED, ARRIVED, DEPARTED, CANCELLED, NO_SHOW;

    public static StayStatus parse(String raw) {
        return StayStatus.valueOf(raw.trim().toUpperCase());
    }
}
