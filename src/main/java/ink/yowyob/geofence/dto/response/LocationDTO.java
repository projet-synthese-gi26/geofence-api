package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.dto.PointDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record LocationDTO(
        UUID id,
        PointDTO position,
        SimpleVehicleDTO vehicle,
        LocalDateTime timestamp,
        Double speed,
        Double heading,
        Double altitude,
        Double accuracy,
        String source
) {}