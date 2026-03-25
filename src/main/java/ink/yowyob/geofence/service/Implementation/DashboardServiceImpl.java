package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.dto.response.DashboardStatsDTO;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.model.Location;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.repository.LocationRepository;
import ink.yowyob.geofence.repository.AlertRepository;
import ink.yowyob.geofence.repository.GeofenceZoneRepository;
import ink.yowyob.geofence.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    
    private final VehicleRepository vehicleRepository;
    private final LocationRepository locationRepository;
    private final AlertRepository alertRepository;
    private final GeofenceZoneRepository geofenceZoneRepository;
    
    @Override
    public DashboardStatsDTO getUserDashboardStats(User user) {
        try {
            // Récupérer tous les véhicules de l'utilisateur
            List<Vehicle> vehicles = vehicleRepository.findByUser(user);
            int totalVehicles = vehicles.size();
            
            // Calculer les statistiques
            int activeVehicles = 0;
            int onlineVehicles = 0;
            int recentPositions = 0;
            double totalSpeed = 0;
            int speedCount = 0;
            double maxSpeedToday = 0;
            int totalPositionsToday = 0;
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            LocalDateTime thirtyMinutesAgo = now.minusMinutes(30);
            
            // Parcourir chaque véhicule pour calculer les stats
            for (Vehicle vehicle : vehicles) {
                // Dernière position
                List<Location> latestLocations = locationRepository
                        .findByVehicleOrderByTimestampDescWithVehicleList(vehicle, PageRequest.of(0, 1));
                
                if (!latestLocations.isEmpty()) {
                    Location latestLocation = latestLocations.get(0);
                    activeVehicles++;
                    
                    // Vérifier si en ligne (position récente)
                    if (latestLocation.getTimestamp().isAfter(thirtyMinutesAgo)) {
                        onlineVehicles++;
                        recentPositions++;
                    }
                    
                    // Calculer vitesse moyenne
                    if (latestLocation.getSpeed() != null && latestLocation.getSpeed() > 0) {
                        totalSpeed += latestLocation.getSpeed();
                        speedCount++;
                    }
                }
                
                // Positions aujourd'hui
                List<Location> todayLocations = locationRepository
                        .findByVehicleAndTimestampBetween(vehicle, startOfDay, now);
                totalPositionsToday += todayLocations.size();
                
                // Vitesse max aujourd'hui
                OptionalDouble maxSpeed = todayLocations.stream()
                        .filter(l -> l.getSpeed() != null)
                        .mapToDouble(Location::getSpeed)
                        .max();
                if (maxSpeed.isPresent() && maxSpeed.getAsDouble() > maxSpeedToday) {
                    maxSpeedToday = maxSpeed.getAsDouble();
                }
            }
            
            double avgSpeed = speedCount > 0 ? totalSpeed / speedCount : 0.0;
            
            // Compter les alertes
            int totalAlerts = (int) alertRepository.countByVehicleIn(vehicles);
            int alertsToday = (int) alertRepository.countByVehicleInAndTimestampAfter(vehicles, startOfDay);
            
            // Compter les géofences
            int totalGeofences = geofenceZoneRepository.countByUser(user);
            
            // Statistiques d'activité des véhicules
            int vehiclesOffline = totalVehicles - onlineVehicles;
            
            DashboardStatsDTO.VehicleActivityStatsDTO vehicleActivity = 
                    new DashboardStatsDTO.VehicleActivityStatsDTO(
                            recentPositions,
                            vehiclesOffline,
                            totalPositionsToday,
                            maxSpeedToday,
                            alertsToday
                    );
            
            return new DashboardStatsDTO(
                    totalVehicles,
                    activeVehicles,
                    totalAlerts,
                    recentPositions,
                    avgSpeed,
                    onlineVehicles,
                    totalGeofences,
                    alertsToday,
                    vehicleActivity
            );
            
        } catch (Exception e) {
            log.error("Erreur lors du calcul des statistiques dashboard pour l'utilisateur {}: {}", 
                    user.getEmail(), e.getMessage());
            
            // Retourner des valeurs par défaut en cas d'erreur
            return new DashboardStatsDTO(0, 0, 0, 0, 0.0, 0, 0, 0, 
                    new DashboardStatsDTO.VehicleActivityStatsDTO(0, 0, 0, 0.0, 0));
        }
    }
}