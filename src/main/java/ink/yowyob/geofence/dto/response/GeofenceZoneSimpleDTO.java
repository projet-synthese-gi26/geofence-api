package ink.yowyob.geofence.dto.response;

import java.util.UUID;

public record GeofenceZoneSimpleDTO(
        UUID id,
        String title,
        String type
) {
}