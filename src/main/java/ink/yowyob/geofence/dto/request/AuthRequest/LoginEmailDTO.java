package ink.yowyob.geofence.dto.request.AuthRequest;

public record LoginEmailDTO(
        String email,
        String password
) implements LoginDTO{
}
