package coralcove.model;

public class InventoryItem {
    private long id;
    private long resortId;
    private String sku;
    private String name;
    private String category;
    private Long supplierId;
    private String supplierName;
    private int quantity;
    private int minQuantity;
    private String unit;
    private boolean sample;

    public long getResortId() { return resortId; }
    public void setResortId(long resortId) { this.resortId = resortId; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }
    public boolean isLow() { return quantity < minQuantity; }
}
