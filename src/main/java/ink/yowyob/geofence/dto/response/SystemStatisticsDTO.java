package ink.yowyob.geofence.dto.response;

import java.util.List;
import java.util.Map;

public record SystemStatisticsDTO(
        long totalUsers,
        long totalVehicles,
        long totalGeofenceZones,
        long totalAlerts,
        List<AlertCountDTO> alertsByType,
        Map<String, Long> alertsPerDay
) {
}