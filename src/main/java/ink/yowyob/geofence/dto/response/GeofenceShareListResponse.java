package ink.yowyob.geofence.dto.response;

import java.util.List;

public record GeofenceShareListResponse(
        List<GeofenceShareDTO> shares,
        int totalItems
) {
}