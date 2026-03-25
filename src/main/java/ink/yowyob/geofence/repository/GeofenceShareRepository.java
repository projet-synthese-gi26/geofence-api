package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.Enum.ShareStatus;
import ink.yowyob.geofence.model.GeofenceShare;
import ink.yowyob.geofence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GeofenceShareRepository extends JpaRepository<GeofenceShare, UUID> {
    List<GeofenceShare> findBySharedWith(User user);
    List<GeofenceShare> findBySharedWithAndExpiresAtAfter(User user, LocalDateTime dateTime);
    List<GeofenceShare> findBySharedBy(User user);

    // Méthodes pour les invitations avec status
    List<GeofenceShare> findBySharedWithAndStatus(User user, ShareStatus status);
    List<GeofenceShare> findBySharedWithAndStatusAndExpiresAtAfter(User user, ShareStatus status, LocalDateTime dateTime);

    // Invitations en attente
    List<GeofenceShare> findBySharedWithAndStatusOrderBySharedAtDesc(User user, ShareStatus status);

    // MÉTHODES EXISTANTES POUR LES INVITATIONS
    @Query("SELECT CASE WHEN COUNT(gs) > 0 THEN true ELSE false END FROM GeofenceShare gs WHERE gs.geofenceZone.id = :geofenceId AND gs.sharedWith = :user")
    boolean existsByGeofenceZoneIdAndSharedWith(@Param("geofenceId") UUID geofenceId, @Param("user") User user);

    @Query("SELECT gs FROM GeofenceShare gs WHERE gs.geofenceZone.id = :geofenceId AND gs.sharedWith = :user")
    List<GeofenceShare> findByGeofenceZoneIdAndSharedWith(@Param("geofenceId") UUID geofenceId, @Param("user") User user);
}