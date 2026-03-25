package ink.yowyob.geofence.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PolygonGeofenceZoneDTORequest.class, name = "polygon"),
        @JsonSubTypes.Type(value = CircleGeofenceZoneDTORequest.class, name = "circle"),
})
public interface GeofenceZoneDTORequest {
    String title();
    String description();
    
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
