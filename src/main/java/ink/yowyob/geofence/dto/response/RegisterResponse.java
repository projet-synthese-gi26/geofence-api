package ink.yowyob.geofence.dto.response;

public record RegisterResponse(
        boolean success,
        String message,
        UserDTO user
) {
}
