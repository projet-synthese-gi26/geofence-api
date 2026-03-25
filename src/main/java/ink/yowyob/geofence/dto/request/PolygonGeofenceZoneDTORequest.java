package ink.yowyob.geofence.dto.request;

import ink.yowyob.geofence.dto.PolygonDTO;


public record PolygonGeofenceZoneDTORequest(
        String title,
        String description,
        PolygonDTO polygon,
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
