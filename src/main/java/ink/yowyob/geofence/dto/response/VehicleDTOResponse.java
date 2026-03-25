package ink.yowyob.geofence.dto.response;

import java.util.Set;
import java.util.UUID;

public record VehicleDTOResponse(
        UUID id,
        String brand,
        String model,
        String licensePlate,
        String description,
        String imageUrl,
        UserDTO user,
        Set<GeofenceZoneSimpleDTO> geofenceZones
) {
}