package ink.yowyob.geofence.dto.response;

import java.util.List;

public record MultipleGeofenceZoneDTOResponse(
        List<PolygonGeofenceZoneDTOResponse> polygons,
        List<CircleGeofenceZoneDTOResponse> circles
) {
}
