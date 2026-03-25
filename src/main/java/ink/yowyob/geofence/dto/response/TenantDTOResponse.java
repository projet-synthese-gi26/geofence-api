package ink.yowyob.geofence.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un tenant/organisation
 */
public record TenantDTOResponse(
        UUID id,
        String name,
        String domain,
        String apiKey, // Clé API du tenant
        String contactEmail,
        String webhookUrl,
        boolean isActive,
        boolean isInternal, // true si c'est le tenant du frontend original
        String subscriptionPlan,
        int maxUsers,
        int maxVehicles,
        int maxGeofences,
        int rateLimitPerHour,
        LocalDateTime createdAt,
        LocalDateTime lastApiCall
) {
}
