package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.Alert;
import ink.yowyob.geofence.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository <Alert, UUID>{
    List<Alert> findByVehicle(Vehicle vehicle);
    List<Alert> findByVehicleInOrderByTimestampDesc(List<Vehicle> vehicles);
    List<Alert> findAllByOrderByTimestampDesc();
    
    // Nouvelles méthodes pour le dashboard
    long countByVehicleIn(List<Vehicle> vehicles);
    long countByVehicleInAndTimestampAfter(List<Vehicle> vehicles, LocalDateTime timestamp);
    
    // Méthodes pour récupérer les alertes récentes
    @Query("SELECT a FROM Alert a WHERE a.vehicle IN :vehicles AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<Alert> findRecentAlertsByVehicles(@Param("vehicles") List<Vehicle> vehicles, @Param("since") LocalDateTime since);
}
