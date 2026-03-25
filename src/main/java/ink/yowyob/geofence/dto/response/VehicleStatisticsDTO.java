package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.Enum.AlertTypeEnum;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VehicleStatisticsDTO(
        UUID vehicleId,
        String brand,
        String model,
        String licensePlate,
        int totalAlerts,
        int associatedGeofenceZones,
        List<AlertCountDTO> alertsByType,
        Map<String, Long> alertsPerDay,
        AlertTypeEnum mostCommonAlertType
) {
}