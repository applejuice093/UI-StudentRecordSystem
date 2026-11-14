package coralcove.model;

public class LodgeCareTask {
    private long id;
    private long lodgeId;
    private String unitCode;
    private Long assigneeId;
    private String assigneeName;
    private String priority = "NORMAL";
    private String status = "PENDING";
    private String notes;
    private String inspectionStatus = "NONE";
    private String createdAt;
    private String completedAt;
    private boolean sample;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getLodgeId() { return lodgeId; }
    public void setLodgeId(long lodgeId) { this.lodgeId = lodgeId; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getInspectionStatus() { return inspectionStatus; }
    public void setInspectionStatus(String inspectionStatus) { this.inspectionStatus = inspectionStatus; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }
}
