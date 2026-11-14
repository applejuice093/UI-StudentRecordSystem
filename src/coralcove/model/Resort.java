package coralcove.model;

public class Resort {
    private long id;
    private String name;
    private String location;
    private String coastline;
    private String timezone = "Asia/Kolkata";
    private String taxPercent = "12";
    private boolean active = true;
    private boolean sample;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCoastline() { return coastline; }
    public void setCoastline(String coastline) { this.coastline = coastline; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getTaxPercent() { return taxPercent; }
    public void setTaxPercent(String taxPercent) { this.taxPercent = taxPercent; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
