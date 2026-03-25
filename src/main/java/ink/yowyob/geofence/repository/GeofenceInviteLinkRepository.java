package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.GeofenceInviteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeofenceInviteLinkRepository extends JpaRepository<GeofenceInviteLink, UUID> {

    Optional<GeofenceInviteLink> findByInviteCodeAndIsActiveTrue(String inviteCode);

    List<GeofenceInviteLink> findByGeofenceIdAndGeofenceTypeAndIsActiveTrueOrderByCreatedAtDesc(
            UUID geofenceId, String geofenceType);

    List<GeofenceInviteLink> findByCreatedByUuidOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT il FROM GeofenceInviteLink il WHERE il.createdBy.uuid = :userId AND il.geofenceId = :geofenceId AND il.geofenceType = :type AND il.isActive = true")
    List<GeofenceInviteLink> findActiveInviteLinksForGeofence(
            @Param("userId") UUID userId,
            @Param("geofenceId") UUID geofenceId,
            @Param("type") String type);

    boolean existsByInviteCode(String inviteCode);
}