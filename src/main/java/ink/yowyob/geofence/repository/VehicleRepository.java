package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.GeofenceZone;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository <Vehicle, UUID> {
    List<Vehicle> findByUser(User user);
    List<Vehicle> findByUser_Organization_Id(UUID organizationId);
    List<Vehicle> findByGeofenceZonesContaining(GeofenceZone geofenceZone);

    @Query("SELECT v FROM Vehicle v LEFT JOIN FETCH v.assignedRoutes WHERE v.id = :id")
    Optional<Vehicle> findByIdWithRoutes(@Param("id") UUID id);
}
