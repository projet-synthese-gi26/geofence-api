package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.PolygonDTO;
import ink.yowyob.geofence.dto.request.CircleGeofenceZoneDTORequest;
import ink.yowyob.geofence.dto.request.GeofenceZoneDTORequest;
import ink.yowyob.geofence.dto.request.PolygonGeofenceZoneDTORequest;
import ink.yowyob.geofence.dto.response.CircleGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.GeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.MultipleGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.PolygonGeofenceZoneDTOResponse;
import ink.yowyob.geofence.model.User;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.UUID;

public interface GeofenceService {
    GeofenceZoneDTOResponse getGeofenceZone(UUID zoneId, String type, User user);
    MultipleGeofenceZoneDTOResponse getGeofenceZones(User user);
    GeofenceZoneDTOResponse createGeofenceZone(GeofenceZoneDTORequest geofenceZoneDTORequest, User user);
    MultipleGeofenceZoneDTOResponse getMyGeofenceZones(User user);
    GeofenceZoneDTOResponse editGeofenceZone(GeofenceZoneDTORequest geofenceZoneDTORequest, UUID zoneId, String type, User user);
    void deleteGeofenceZone(UUID zoneId, String type, User user);

    PolygonGeofenceZoneDTOResponse getPolygonGeofenceZone(UUID Id, User user);
    List<PolygonGeofenceZoneDTOResponse> getPolygonsGeofenceZone(User user);
    PolygonGeofenceZoneDTOResponse createPolygonGeofenceZone(PolygonGeofenceZoneDTORequest geofenceZoneDTORequest, User user);
    List<PolygonGeofenceZoneDTOResponse> getMyPolygonsGeofenceZone(User user);
    PolygonGeofenceZoneDTOResponse editPolygonGeofenceZone(PolygonGeofenceZoneDTORequest geofenceZoneDTORequest,UUID zoneId, User user);
    void deletePolygonGeofenceZone(UUID zoneId, User user);

    CircleGeofenceZoneDTOResponse getCircleGeofenceZone(UUID Id, User user);
    List<CircleGeofenceZoneDTOResponse> getCirclesGeofenceZone(User user);
    CircleGeofenceZoneDTOResponse createCircleGeofenceZone(CircleGeofenceZoneDTORequest geofenceZoneDTORequest, User user);
    List<CircleGeofenceZoneDTOResponse> getMyCirclesGeofenceZone(User user);
    CircleGeofenceZoneDTOResponse editCircleGeofenceZone(CircleGeofenceZoneDTORequest geofenceZoneDTORequest, UUID zoneId, User user);
    void deleteCircleGeofenceZone(UUID zoneId, User user);




    Polygon convertToPolygon(PolygonDTO dto);
    Point convertToPoint(PointDTO dto);

}
