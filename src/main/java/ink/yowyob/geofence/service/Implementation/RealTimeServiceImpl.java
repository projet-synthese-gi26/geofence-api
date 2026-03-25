package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.dto.response.AlertDTO;
import ink.yowyob.geofence.dto.response.LocationDTO;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.service.RealTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeServiceImpl implements RealTimeService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    public void broadcastLocationUpdate(LocationDTO location, SimpleVehicleDTO vehicle, User user) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "LOCATION_UPDATE");
            message.put("timestamp", LocalDateTime.now());
            message.put("location", location);
            message.put("vehicle", vehicle);
            
            // Envoyer à l'utilisateur propriétaire
            String destination = "/user/" + user.getUuid() + "/queue/location-updates";
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Location update broadcasted for vehicle {} to user {}", 
                    vehicle.licensePlate(), user.getEmail());
        } catch (Exception e) {
            log.error("Error broadcasting location update: {}", e.getMessage());
        }
    }
    
    @Override
    public void broadcastAlert(AlertDTO alert, User user) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ALERT");
            message.put("timestamp", LocalDateTime.now());
            message.put("alert", alert);
            
            // Envoyer à l'utilisateur propriétaire
            String destination = "/user/" + user.getUuid() + "/queue/alerts";
            messagingTemplate.convertAndSend(destination, message);
            
            // Envoyer aussi aux topics généraux pour le dashboard
            messagingTemplate.convertAndSend("/topic/alerts", message);
            
            log.info("Alert broadcasted: {} for user {}", alert.message(), user.getEmail());
        } catch (Exception e) {
            log.error("Error broadcasting alert: {}", e.getMessage());
        }
    }
    
    @Override
    public void broadcastVehicleStatusUpdate(SimpleVehicleDTO vehicle, String status, User user) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "VEHICLE_STATUS");
            message.put("timestamp", LocalDateTime.now());
            message.put("vehicle", vehicle);
            message.put("status", status);
            
            String destination = "/user/" + user.getUuid() + "/queue/vehicle-status";
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Vehicle status update broadcasted: {} - {} for user {}", 
                    vehicle.licensePlate(), status, user.getEmail());
        } catch (Exception e) {
            log.error("Error broadcasting vehicle status update: {}", e.getMessage());
        }
    }
    
    @Override
    public void broadcastDashboardStats(Object stats, User user) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "DASHBOARD_STATS");
            message.put("timestamp", LocalDateTime.now());
            message.put("stats", stats);
            
            String destination = "/user/" + user.getUuid() + "/queue/dashboard-stats";
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Dashboard stats broadcasted to user {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error broadcasting dashboard stats: {}", e.getMessage());
        }
    }
}