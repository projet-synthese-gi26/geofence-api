package ink.yowyob.geofence.dto.request;

public record UpdateTenantRequest(
        String name,
        String domain,
        String contactEmail,
        String webhookUrl,
        Boolean active,
        String subscriptionPlan,
        Integer maxUsers,
        Integer maxVehicles,
        Integer maxGeofences,
        Integer rateLimitPerHour
) {
}
