package ink.yowyob.geofence.dto.request.AuthRequest;

public record LoginPhoneNumberDTO(
        String phoneNumber,
        String password
) implements LoginDTO{
}
