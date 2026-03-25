package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.Enum.UserRole;

import java.time.LocalDate;
import java.util.UUID;

public record UserDTO(
        UUID uuid,
        String username,
        String firstname,
        String lastname,
        String phoneNumber,
        String email,
        LocalDate DOB,
        UserRole Role
) {
}
