package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.dto.PolygonDTO;

import java.util.List;
import java.util.UUID;

public record PolygonGeofenceZoneDTOResponse(
        UUID id,
        UserDTO user,
        String description,
        String title,
        String type,
        PolygonDTO polygon,
        List<SimpleVehicleDTO> vehicles,
        // Propriétés intelligentes
        Boolean isTemporalEnabled,
        String startTime,
        String endTime,
        String[] activeDays,
        Boolean isConditionalEnabled,
        Double maxSpeed,
        Integer maxDwellTime,
        Integer minDwellTime
) implements GeofenceZoneDTOResponse{
}
