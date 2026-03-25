package ink.yowyob.geofence.dto.response;

import java.util.UUID;

public record SimpleVehicleDTO(
        UUID id,
        String brand,
        String model,
        String licensePlate,
        String imageUrl
) {
}