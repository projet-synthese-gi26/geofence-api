package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.Location;
import ink.yowyob.geofence.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    // Trouver les dernières positions d'un véhicule
    Page<Location> findByVehicleOrderByTimestampDesc(Vehicle vehicle, Pageable pageable);

    // Trouver la dernière position d'un véhicule
    Optional<Location> findTopByVehicleOrderByTimestampDesc(Vehicle vehicle);

    // Trouver les positions d'un véhicule dans une période
    @Query("SELECT l FROM Location l WHERE l.vehicle = :vehicle AND l.timestamp BETWEEN :startTime AND :endTime ORDER BY l.timestamp DESC")
    List<Location> findByVehicleAndTimestampBetween(
            @Param("vehicle") Vehicle vehicle,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Trouver toutes les dernières positions par véhicule
    @Query("SELECT l FROM Location l WHERE l.id IN (SELECT MAX(l2.id) FROM Location l2 GROUP BY l2.vehicle.id)")
    List<Location> findLatestLocationsByVehicle();

    // Compter les positions d'un véhicule
    long countByVehicle(Vehicle vehicle);

    // Supprimer les anciennes positions (pour nettoyage)
    void deleteByTimestampBefore(LocalDateTime timestamp);

    /**
     * Trouve la position précédente d'un véhicule (excluant une position spécifique)
     */
    @Query("SELECT l FROM Location l WHERE l.vehicle = :vehicle AND l.id != :excludeId ORDER BY l.timestamp DESC LIMIT 1")
    Optional<Location> findTopByVehicleAndIdNotOrderByTimestampDesc(
            @Param("vehicle") Vehicle vehicle,
            @Param("excludeId") UUID excludeId
    );

    /**
     * Trouve la position précédente d'un véhicule avant un timestamp donné
     */
    @Query("SELECT l FROM Location l WHERE l.vehicle = :vehicle AND l.timestamp < :beforeTimestamp ORDER BY l.timestamp DESC LIMIT 1")
    Optional<Location> findTopByVehicleAndTimestampBeforeOrderByTimestampDesc(
            @Param("vehicle") Vehicle vehicle,
            @Param("beforeTimestamp") LocalDateTime beforeTimestamp
    );

    /**
     * Trouve les positions d'un véhicule avec fetch join pour éviter le lazy loading
     */
    @Query("SELECT l FROM Location l JOIN FETCH l.vehicle WHERE l.vehicle = :vehicle ORDER BY l.timestamp DESC")
    Page<Location> findByVehicleOrderByTimestampDescWithVehicle(Vehicle vehicle, Pageable pageable);

    /**
     * Trouve la dernière position d'un véhicule avec fetch join pour éviter le lazy loading
     */
    @Query("SELECT l FROM Location l JOIN FETCH l.vehicle WHERE l.vehicle = :vehicle ORDER BY l.timestamp DESC")
    List<Location> findByVehicleOrderByTimestampDescWithVehicleList(Vehicle vehicle, Pageable pageable);
    
    /**
     * Trouve la première entrée d'un véhicule dans une zone (pour calcul temps de séjour)
     * Note: Requête simplifiée - la logique complète sera dans le service
     */
    @Query("SELECT l FROM Location l WHERE l.vehicle = :vehicle AND l.timestamp < :exitTime ORDER BY l.timestamp ASC")
    Optional<Location> findFirstEntryIntoGeofence(
            @Param("vehicle") Vehicle vehicle, 
            @Param("geofenceId") UUID geofenceId, 
            @Param("exitTime") LocalDateTime exitTime
    );
}
