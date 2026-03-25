package ink.yowyob.geofence.dto.request;

import ink.yowyob.geofence.dto.PointDTO;

import java.util.List;

public record RouteDTORequest(
    String name,
    String description,
    PointDTO startPoint,
    String startAddress,
    PointDTO endPoint,
    String endAddress,
    Double estimatedDistance,
    Integer estimatedDuration,
    Double deviationTolerance,
    
    // Propriétés temporelles
    Boolean isTemporalEnabled,
    String startTime,
    String endTime,
    String[] activeDays,
    
    // Segments de route autorisés
    List<RouteSegmentDTORequest> authorizedSegments,
    
    Boolean isActive
) {}