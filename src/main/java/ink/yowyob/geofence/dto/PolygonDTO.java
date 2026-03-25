package ink.yowyob.geofence.dto;

import java.util.List;

public record PolygonDTO(
        List<List<List<Double>>> coordinates
) {
}
