public class EmergencyData {
    private String userName;
    private String userPhone;
    private LocationData location;
    private String emergencyType;

    public EmergencyData(String userName, String userPhone, LocationData location, String emergencyType) {
        this.userName = userName;
        this.userPhone = userPhone;
        this.location = location;
        this.emergencyType = emergencyType;
    }

    // Getters and setters
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public LocationData getLocation() { return location; }
    public void setLocation(LocationData location) { this.location = location; }

    public String getEmergencyType() { return emergencyType; }
    public void setEmergencyType(String emergencyType) { this.emergencyType = emergencyType; }
}
