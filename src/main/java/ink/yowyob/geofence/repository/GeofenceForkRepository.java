package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.GeofenceFork;
import ink.yowyob.geofence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeofenceForkRepository extends JpaRepository<GeofenceFork, UUID> {

    // Trouver les forks d'une géofence originale
    List<GeofenceFork> findByOriginalGeofenceIdOrderByForkedAtDesc(UUID originalGeofenceId);

    // Trouver l'original d'une géofence forkée
    Optional<GeofenceFork> findByForkedGeofenceId(UUID forkedGeofenceId);

    // Trouver tous les forks créés par un utilisateur
    List<GeofenceFork> findByForkedByOrderByForkedAtDesc(User user);

    // Compter les forks d'une géofence
    long countByOriginalGeofenceId(UUID originalGeofenceId);

    // Vérifier si une géofence a été forkée
    boolean existsByOriginalGeofenceId(UUID originalGeofenceId);

    // Vérifier si une géofence est un fork
    boolean existsByForkedGeofenceId(UUID forkedGeofenceId);

    // Requête complexe pour obtenir la chaîne de forks
    @Query("SELECT gf FROM GeofenceFork gf WHERE gf.originalGeofence.id = :geofenceId OR gf.forkedGeofence.id = :geofenceId")
    List<GeofenceFork> findForkChain(@Param("geofenceId") UUID geofenceId);
}