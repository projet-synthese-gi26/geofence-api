package ink.yowyob.geofence.dto.response;

import java.util.List;

public record LocationUpdateResponse(
        boolean success,
        String message,
        LocationDTO location,
        List<AlertDTO> alertsGenerated // Alertes générées suite à cette position
) {}