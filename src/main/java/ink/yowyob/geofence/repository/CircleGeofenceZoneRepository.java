package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.CircleGeofenceZone;
import ink.yowyob.geofence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CircleGeofenceZoneRepository extends JpaRepository <CircleGeofenceZone, UUID> {
    List<CircleGeofenceZone> findByUser(User user);
    List<CircleGeofenceZone> findByUser_Organization_Id(UUID organizationId);
}
