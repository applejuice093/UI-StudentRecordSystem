package coralcove.model;

import java.math.BigDecimal;

public class Payment {
    private long id;
    private long invoiceId;
    private BigDecimal amount = BigDecimal.ZERO;
    private PaymentMethod method = PaymentMethod.CASH;
    private String reference;
    private String paidAt;
    private Long receivedBy;
    private String note;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(long invoiceId) { this.invoiceId = invoiceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }
    public Long getReceivedBy() { return receivedBy; }
    public void setReceivedBy(Long receivedBy) { this.receivedBy = receivedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
