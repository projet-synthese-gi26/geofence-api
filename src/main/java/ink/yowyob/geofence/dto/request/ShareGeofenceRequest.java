package ink.yowyob.geofence.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShareGeofenceRequest(
        UUID targetUserId,
        LocalDateTime expiresAt,
        boolean canEdit
) {}