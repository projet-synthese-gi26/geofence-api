package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.dto.PointDTO;

import java.util.List;
import java.util.UUID;

public record CircleGeofenceZoneDTOResponse(
        UUID id,
        UserDTO user,
        String type,
        String description,
        String title,
        PointDTO center,
        Double radius,
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
