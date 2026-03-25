package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.response.GeofenceForkDTO;
import ink.yowyob.geofence.dto.response.GeofenceZoneSimpleDTO;
import ink.yowyob.geofence.model.CircleGeofenceZone;
import ink.yowyob.geofence.model.GeofenceFork;
import ink.yowyob.geofence.model.GeofenceZone;
import ink.yowyob.geofence.model.PolygonGeofenceZone;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class GeofenceForkDTOMapper implements Function<GeofenceFork, GeofenceForkDTO> {

    private final UserDTOMapper userDTOMapper;

    public GeofenceForkDTOMapper(UserDTOMapper userDTOMapper) {
        this.userDTOMapper = userDTOMapper;
    }

    @Override
    public GeofenceForkDTO apply(GeofenceFork fork) {
        GeofenceZoneSimpleDTO originalDTO = mapToSimpleDTO(fork.getOriginalGeofence());
        GeofenceZoneSimpleDTO forkedDTO = mapToSimpleDTO(fork.getForkedGeofence());

        return new GeofenceForkDTO(
                fork.getId(),
                originalDTO,
                forkedDTO,
                userDTOMapper.apply(fork.getForkedBy()),
                fork.getForkedAt(),
                fork.getForkReason()
        );
    }

    private GeofenceZoneSimpleDTO mapToSimpleDTO(GeofenceZone geofence) {
        String type = "unknown";
        if (geofence instanceof CircleGeofenceZone) {
            type = "circle";
        } else if (geofence instanceof PolygonGeofenceZone) {
            type = "polygon";
        }

        return new GeofenceZoneSimpleDTO(
                geofence.getId(),
                geofence.getTitle(),
                type
        );
    }
}