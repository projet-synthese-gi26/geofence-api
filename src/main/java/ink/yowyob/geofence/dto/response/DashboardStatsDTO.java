package ink.yowyob.geofence.dto.response;

public record DashboardStatsDTO(
        int totalVehicles,
        int activeVehicles,
        int totalAlerts,
        int recentPositions,
        double avgSpeed,
        int onlineVehicles,
        int totalGeofences,
        int recentAlerts,
        VehicleActivityStatsDTO vehicleActivity
) {
    
    public record VehicleActivityStatsDTO(
            int vehiclesWithRecentPositions,
            int vehiclesOffline,
            int totalPositionsToday,
            double maxSpeedToday,
            int alertsToday
    ) {}
}