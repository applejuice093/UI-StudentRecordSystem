package coralcove.model;

import java.math.BigDecimal;

public class Lodge {
    private long id;
    private long resortId;
    private String resortName;
    private String unitCode;
    private String zone = "BEACH";
    private String viewAspect = "SEA";
    private long lodgeCategoryId;
    private String roomTypeName;
    private String bedConfig;
    private int maxGuests;
    private BigDecimal pricePerNight = BigDecimal.ZERO;
    private String description;
    private String amenities;
    private LodgeStatus status = LodgeStatus.READY;
    private boolean sample;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getResortId() { return resortId; }
    public void setResortId(long resortId) { this.resortId = resortId; }
    public String getResortName() { return resortName; }
    public void setResortName(String resortName) { this.resortName = resortName; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getViewAspect() { return viewAspect; }
    public void setViewAspect(String viewAspect) { this.viewAspect = viewAspect; }
    public long getLodgeCategoryId() { return lodgeCategoryId; }
    public void setLodgeCategoryId(long lodgeCategoryId) { this.lodgeCategoryId = lodgeCategoryId; }
    public String getRoomTypeName() { return roomTypeName; }
    public void setRoomTypeName(String roomTypeName) { this.roomTypeName = roomTypeName; }
    public String getBedConfig() { return bedConfig; }
    public void setBedConfig(String bedConfig) { this.bedConfig = bedConfig; }
    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public LodgeStatus getStatus() { return status; }
    public void setStatus(LodgeStatus status) { this.status = status; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }

    @Override
    public String toString() {
        return unitCode + " · " + (roomTypeName == null ? "" : roomTypeName);
    }
}
