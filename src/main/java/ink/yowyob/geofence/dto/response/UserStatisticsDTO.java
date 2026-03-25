package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.Enum.AlertTypeEnum;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UserStatisticsDTO(
        int totalVehicles,
        int totalGeofenceZones,
        int totalAlerts,
        List<AlertCountDTO> alertsByType,
        Map<String, Long> alertsPerDay,
        UUID mostActiveVehicleId,
        AlertTypeEnum mostCommonAlertType
) {
}