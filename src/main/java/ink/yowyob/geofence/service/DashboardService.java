package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.response.DashboardStatsDTO;
import ink.yowyob.geofence.model.User;

public interface DashboardService {
    
    /**
     * Récupérer les statistiques du dashboard pour un utilisateur
     */
    DashboardStatsDTO getUserDashboardStats(User user);
}