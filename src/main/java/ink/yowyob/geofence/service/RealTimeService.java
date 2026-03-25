package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.response.AlertDTO;
import ink.yowyob.geofence.dto.response.LocationDTO;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.model.User;

public interface RealTimeService {
    
    /**
     * Diffuser une nouvelle position de véhicule en temps réel
     */
    void broadcastLocationUpdate(LocationDTO location, SimpleVehicleDTO vehicle, User user);
    
    /**
     * Diffuser une nouvelle alerte en temps réel
     */
    void broadcastAlert(AlertDTO alert, User user);
    
    /**
     * Diffuser une mise à jour de statut de véhicule
     */
    void broadcastVehicleStatusUpdate(SimpleVehicleDTO vehicle, String status, User user);
    
    /**
     * Envoyer des statistiques du dashboard en temps réel
     */
    void broadcastDashboardStats(Object stats, User user);
}