package ink.yowyob.geofence.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleApiKeyDTO(
        UUID id,
        String apiKey,
        SimpleVehicleDTO vehicle,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        LocalDateTime expiresAt,
        boolean isExpired,
        boolean isValid
) {}