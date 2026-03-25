package ink.yowyob.geofence.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeofenceForkDTO(
        UUID id,
        GeofenceZoneSimpleDTO originalGeofence,
        GeofenceZoneSimpleDTO forkedGeofence,
        UserDTO forkedBy,
        LocalDateTime forkedAt,
        String forkReason
) {}