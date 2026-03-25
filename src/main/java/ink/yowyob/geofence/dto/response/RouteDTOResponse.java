package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.dto.PointDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RouteDTOResponse(
    UUID id,
    UserDTO user,
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
    List<RouteSegmentDTOResponse> authorizedSegments,
    
    // Véhicules assignés
    List<SimpleVehicleDTO> assignedVehicles,
    
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}