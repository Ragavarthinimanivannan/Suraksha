public class EmergencyResponse {
    private boolean success;
    private String message;
    private String id;
    private EmergencyData data;

    // Getters and setters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getId() { return id; }
    public EmergencyData getData() { return data; }
}