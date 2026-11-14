package coralcove.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StayBooking {
    private long id;
    private String bookingReference;
    private long resortId;
    private String mealPlan = "EP";
    private String transfer = "NONE";
    private long guestId;
    private String guestName;
    private long lodgeId;
    private String unitCode;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int adults;
    private int children;
    private int nights;
    private BigDecimal roomPrice = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BookingSource source = BookingSource.WALK_IN;
    private StayStatus status = StayStatus.CONFIRMED;
    private String specialRequests;
    private String createdAt;
    private boolean sample;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    public long getResortId() { return resortId; }
    public void setResortId(long resortId) { this.resortId = resortId; }
    public String getMealPlan() { return mealPlan; }
    public void setMealPlan(String mealPlan) { this.mealPlan = mealPlan; }
    public String getTransfer() { return transfer; }
    public void setTransfer(String transfer) { this.transfer = transfer; }
    public long getGuestId() { return guestId; }
    public void setGuestId(long guestId) { this.guestId = guestId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public long getLodgeId() { return lodgeId; }
    public void setLodgeId(long lodgeId) { this.lodgeId = lodgeId; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public int getAdults() { return adults; }
    public void setAdults(int adults) { this.adults = adults; }
    public int getChildren() { return children; }
    public void setChildren(int children) { this.children = children; }
    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }
    public BigDecimal getRoomPrice() { return roomPrice; }
    public void setRoomPrice(BigDecimal roomPrice) { this.roomPrice = roomPrice; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BookingSource getSource() { return source; }
    public void setSource(BookingSource source) { this.source = source; }
    public StayStatus getStatus() { return status; }
    public void setStatus(StayStatus status) { this.status = status; }
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }
}
