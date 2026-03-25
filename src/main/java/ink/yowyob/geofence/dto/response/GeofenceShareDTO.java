package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.Enum.ShareStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeofenceShareDTO(
        UUID id,
        GeofenceZoneSimpleDTO geofenceZone,
        UserDTO sharedBy,
        UserDTO sharedWith,
        LocalDateTime sharedAt,
        LocalDateTime expiresAt,
        boolean canEdit,
        ShareStatus status,
        LocalDateTime respondedAt
) {
}