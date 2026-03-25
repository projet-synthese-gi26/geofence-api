package ink.yowyob.geofence.dto.request;

import ink.yowyob.geofence.dto.PointDTO;

public record CircleGeofenceZoneDTORequest(
        String title,
        String description,
        PointDTO center,
        Double radius,
        // Propriétés intelligentes
        Boolean isTemporalEnabled,
        String startTime,
        String endTime,
        String[] activeDays,
        Boolean isConditionalEnabled,
        Double maxSpeed,
        Integer maxDwellTime,
        Integer minDwellTime
) implements GeofenceZoneDTORequest {
}
