import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/emergency")
    Call<EmergencyResponse> saveEmergency(@Body EmergencyData emergencyData);
}