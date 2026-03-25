package ink.yowyob.geofence.dto.response;

public record AuthResponse(
        UserDTO userDTO,
        String token
) {
}
