package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.PolygonGeofenceZone;
import ink.yowyob.geofence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolygonGeofenceZoneRepository extends JpaRepository<PolygonGeofenceZone, UUID> {
    List<PolygonGeofenceZone> findByUser(User user);
    List<PolygonGeofenceZone> findByUser_Organization_Id(UUID organizationId);
}
