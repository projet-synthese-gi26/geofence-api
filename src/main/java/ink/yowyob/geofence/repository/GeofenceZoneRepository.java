package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.GeofenceZone;
import ink.yowyob.geofence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GeofenceZoneRepository extends JpaRepository<GeofenceZone, UUID> {
    
    /**
     * Compter le nombre total de géofences pour un utilisateur
     */
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT id FROM circle_geofence_zone WHERE user_uuid = :#{#user.uuid}
                UNION ALL
                SELECT id FROM polygon_geofence_zone WHERE user_uuid = :#{#user.uuid}
            ) AS combined_zones
            """, nativeQuery = true)
    int countByUser(@Param("user") User user);
}