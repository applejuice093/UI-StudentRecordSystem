package coralcove.model;

public class Guest {
    private long id;
    private String fullName;
    private String phone;
    private String email;
    private String gender;
    private String nationality;
    private String address;
    private String idDocType;
    private String idDocNumber;
    private String preferences;
    private String vip = "STANDARD";
    private String registeredOn;
    private boolean sample;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getIdDocType() { return idDocType; }
    public void setIdDocType(String idDocType) { this.idDocType = idDocType; }
    public String getIdDocNumber() { return idDocNumber; }
    public void setIdDocNumber(String idDocNumber) { this.idDocNumber = idDocNumber; }
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
    public String getVip() { return vip; }
    public void setVip(String vip) { this.vip = vip; }
    public String getRegisteredOn() { return registeredOn; }
    public void setRegisteredOn(String registeredOn) { this.registeredOn = registeredOn; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }

    @Override
    public String toString() {
        return fullName + (phone == null || phone.isBlank() ? "" : " · " + phone);
    }
}
