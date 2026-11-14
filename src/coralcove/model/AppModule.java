package coralcove.model;

public enum AppModule {
    DASHBOARD("Dashboard"),
    STAYS("Stays"),
    LODGES("Lodges"),
    GUESTS("Guests"),
    ARRIVALS("Arrivals"),
    BILLING("Billing"),
    LODGE_CARE("Lodge care"),
    EXPERIENCES("Experiences"),
    EMPLOYEES("Employees"),
    INVENTORY("Inventory"),
    REPORTS("Reports"),
    SETTINGS("Settings");

    private final String label;

    AppModule(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
