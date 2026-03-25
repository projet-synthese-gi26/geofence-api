package ink.yowyob.geofence.dto.response;

import java.util.UUID;

public interface GeofenceZoneDTOResponse {
    UUID id();
    UserDTO user();
    String description();
    String title();
    String type();
    
    // Propriétés intelligentes
    Boolean isTemporalEnabled();
    String startTime();
    String endTime();
    String[] activeDays();
    Boolean isConditionalEnabled();
    Double maxSpeed();
    Integer maxDwellTime();
    Integer minDwellTime();
}
