package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.response.GeofenceShareDTO;
import ink.yowyob.geofence.dto.response.GeofenceZoneSimpleDTO;
import ink.yowyob.geofence.dto.response.UserDTO;
import ink.yowyob.geofence.model.CircleGeofenceZone;
import ink.yowyob.geofence.model.GeofenceShare;
import ink.yowyob.geofence.model.PolygonGeofenceZone;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class GeofenceShareDTOMapper implements Function<GeofenceShare, GeofenceShareDTO> {

    private final UserDTOMapper userDTOMapper;

    public GeofenceShareDTOMapper(UserDTOMapper userDTOMapper) {
        this.userDTOMapper = userDTOMapper;
    }

    @Override
    public GeofenceShareDTO apply(GeofenceShare geofenceShare) {
        // Convert geofence zone to simple DTO
        String type = "unknown";
        if (geofenceShare.getGeofenceZone() instanceof CircleGeofenceZone) {
            type = "circle";
        } else if (geofenceShare.getGeofenceZone() instanceof PolygonGeofenceZone) {
            type = "polygon";
        }

        GeofenceZoneSimpleDTO geofenceZoneDTO = new GeofenceZoneSimpleDTO(
                geofenceShare.getGeofenceZone().getId(),
                geofenceShare.getGeofenceZone().getTitle(),
                type
        );

        // Convert users to DTOs
        UserDTO sharedByDTO = userDTOMapper.apply(geofenceShare.getSharedBy());
        UserDTO sharedWithDTO = userDTOMapper.apply(geofenceShare.getSharedWith());

        return new GeofenceShareDTO(
                geofenceShare.getId(),
                geofenceZoneDTO,
                sharedByDTO,
                sharedWithDTO,
                geofenceShare.getSharedAt(),
                geofenceShare.getExpiresAt(),
                geofenceShare.isCanEdit(),
                geofenceShare.getStatus(),
                geofenceShare.getRespondedAt()
        );
    }
}