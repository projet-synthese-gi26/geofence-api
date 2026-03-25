package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.model.VehicleApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleApiKeyRepository extends JpaRepository<VehicleApiKey, UUID> {

    @Query("SELECT vak FROM VehicleApiKey vak " +
           "LEFT JOIN FETCH vak.vehicle v " +
           "LEFT JOIN FETCH v.geofenceZones " +
           "WHERE vak.apiKey = :apiKey AND vak.isActive = true")
    Optional<VehicleApiKey> findByApiKeyAndIsActiveTrue(@Param("apiKey") String apiKey);

    Optional<VehicleApiKey> findByVehicle(Vehicle vehicle);

    Optional<VehicleApiKey> findByVehicleId(UUID vehicleId);

    boolean existsByApiKey(String apiKey);

    @Modifying
    @Query("UPDATE VehicleApiKey v SET v.isActive = false WHERE v.vehicle.id = :vehicleId")
    void deactivateByVehicleId(@Param("vehicleId") UUID vehicleId);

    @Modifying
    @Query("UPDATE VehicleApiKey v SET v.lastUsedAt = CURRENT_TIMESTAMP WHERE v.apiKey = :apiKey")
    void updateLastUsed(@Param("apiKey") String apiKey);
}