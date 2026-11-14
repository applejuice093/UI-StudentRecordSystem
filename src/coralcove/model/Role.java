package coralcove.model;

public enum Role {
    ADMIN,
    MANAGER,
    FRONT_DESK,
    ATTENDANT;

    public String label() {
        String lower = name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static Role parse(String raw) {
        return Role.valueOf(raw.trim().toUpperCase());
    }
}
