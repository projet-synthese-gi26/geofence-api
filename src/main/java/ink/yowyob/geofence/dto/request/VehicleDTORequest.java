package ink.yowyob.geofence.dto.request;

import java.util.Set;
import java.util.UUID;

public record VehicleDTORequest(
        String brand,
        String model,
        String licensePlate,
        String description,
        Set<UUID> geofenceZoneIds
) {
}