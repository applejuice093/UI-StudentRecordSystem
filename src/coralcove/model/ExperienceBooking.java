package coralcove.model;

import java.math.BigDecimal;

public class ExperienceBooking {
    private long id;
    private Long experienceId;
    private Long reservationId;
    private Long guestId;
    private Long lodgeId;
    private String unitCode;
    private String orderType;
    private String description;
    private int qty;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;
    private String status;
    private Long invoiceItemId;
    private String createdAt;
    private boolean sample;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Long getServiceId() { return experienceId; }
    public void setServiceId(Long experienceId) { this.experienceId = experienceId; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getGuestId() { return guestId; }
    public void setGuestId(Long guestId) { this.guestId = guestId; }
    public Long getLodgeId() { return lodgeId; }
    public void setLodgeId(Long lodgeId) { this.lodgeId = lodgeId; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getInvoiceItemId() { return invoiceItemId; }
    public void setInvoiceItemId(Long invoiceItemId) { this.invoiceItemId = invoiceItemId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }
}
