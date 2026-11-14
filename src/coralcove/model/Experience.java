package coralcove.model;

import java.math.BigDecimal;

public class Experience {
    private long id;
    private long resortId;
    private String name;
    private String category;
    private BigDecimal price = BigDecimal.ZERO;
    private boolean active = true;

    public long getResortId() { return resortId; }
    public void setResortId(long resortId) { this.resortId = resortId; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name;
    }
}
