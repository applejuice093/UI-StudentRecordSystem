package coralcove.model;

import java.math.BigDecimal;

public class Employee {
    private long id;
    private long resortId;
    private String name;
    private String phone;
    private String email;
    private String designation;
    private String department;
    private BigDecimal salary = BigDecimal.ZERO;
    private String joiningDate;
    private String status = "ACTIVE";
    private boolean sample;

    public long getResortId() { return resortId; }
    public void setResortId(long resortId) { this.resortId = resortId; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public String getJoiningDate() { return joiningDate; }
    public void setJoiningDate(String joiningDate) { this.joiningDate = joiningDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }

    @Override
    public String toString() {
        return name + " · " + department;
    }
}
