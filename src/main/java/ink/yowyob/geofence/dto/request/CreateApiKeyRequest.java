package ink.yowyob.geofence.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateApiKeyRequest(
        @NotNull(message = "L'ID du véhicule est requis")
        UUID vehicleId,

        LocalDateTime expiresAt // Date d'expiration optionnelle
) {}