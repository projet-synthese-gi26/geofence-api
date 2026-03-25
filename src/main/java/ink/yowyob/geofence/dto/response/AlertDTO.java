package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.Enum.AlertTypeEnum;
import ink.yowyob.geofence.dto.PointDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertDTO(
        UUID id,
        AlertTypeEnum type,
        LocalDateTime timestamp,
        String message,
        PointDTO location,
        SimpleVehicleDTO vehicle,
        GeofenceZoneSimpleDTO geofenceZone
) {
}