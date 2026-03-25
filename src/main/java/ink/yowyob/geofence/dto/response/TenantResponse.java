package ink.yowyob.geofence.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String apiKey,
        String name,
        String domain,
        String contactEmail,
        String webhookUrl,
        boolean active,
        boolean internal,
        String subscriptionPlan,
        int maxUsers,
        int maxVehicles,
        int maxGeofences,
        int rateLimitPerHour,
        LocalDateTime createdAt,
        LocalDateTime lastApiCall
) {
}
