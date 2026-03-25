package ink.yowyob.geofence.dto.request.AuthRequest;

public record LoginUsernameDTO(
        String username,
        String password
) implements LoginDTO{
}
