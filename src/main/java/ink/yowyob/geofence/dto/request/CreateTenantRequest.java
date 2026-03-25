package ink.yowyob.geofence.dto.request;

public record CreateTenantRequest(
        String apiKey,
        String name,
        String domain,
        String contactEmail,
        String webhookUrl,
        String subscriptionPlan,
        Integer maxUsers,
        Integer maxVehicles,
        Integer maxGeofences,
        Integer rateLimitPerHour
) {
}
