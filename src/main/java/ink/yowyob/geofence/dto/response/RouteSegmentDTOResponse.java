package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.dto.LineStringDTO;
import ink.yowyob.geofence.model.RouteSegment;

import java.time.LocalDateTime;
import java.util.UUID;

public record RouteSegmentDTOResponse(
    UUID id,
    String name,
    String description,
    LineStringDTO pathLine,
    Integer segmentOrder,
    Double segmentLength,
    RouteSegment.RouteSegmentType segmentType,
    Integer priority,
    Double speedLimit,
    Integer estimatedTime,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}