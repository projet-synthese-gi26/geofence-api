package ink.yowyob.geofence.dto.request;

import ink.yowyob.geofence.dto.LineStringDTO;
import ink.yowyob.geofence.model.RouteSegment;

public record RouteSegmentDTORequest(
    String name,
    String description,
    LineStringDTO pathLine,
    Integer segmentOrder,
    RouteSegment.RouteSegmentType segmentType,
    Integer priority,
    Double speedLimit,
    Integer estimatedTime,
    Boolean isActive
) {}