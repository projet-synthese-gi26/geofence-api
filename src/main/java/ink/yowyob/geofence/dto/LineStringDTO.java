package ink.yowyob.geofence.dto;

import java.util.List;

public record LineStringDTO(
    List<List<Double>> coordinates  // Array of [longitude, latitude] points
) {}