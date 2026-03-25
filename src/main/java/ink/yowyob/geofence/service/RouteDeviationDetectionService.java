package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.response.RouteDTOResponse;
import ink.yowyob.geofence.model.Route;
import ink.yowyob.geofence.model.Vehicle;
import org.locationtech.jts.geom.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RouteDeviationDetectionService {
    
    /**
     * DTO pour les alertes de déviation
     */
    record RouteDeviationAlert(
        UUID vehicleId,
        String vehicleName,
        UUID routeId,
        String routeName,
        Point currentPosition,
        Double deviationDistance,
        Double toleranceDistance,
        LocalDateTime detectedAt,
        String severity,
        String message
    ) {}
    
    /**
     * DTO pour le statut de suivi de route
     */
    record RouteTrackingStatus(
        UUID vehicleId,
        UUID routeId,
        boolean isOnRoute,
        Double progressPercentage,
        Double deviationDistance,
        String currentSegment,
        LocalDateTime lastUpdateAt
    ) {}
    
    /**
     * Surveiller en continu les déviations d'un véhicule
     */
    Flux<RouteDeviationAlert> monitorVehicleDeviations(UUID vehicleId);
    
    /**
     * Vérifier si un véhicule dévie de ses routes assignées
     */
    Mono<RouteDeviationAlert> checkVehicleDeviation(UUID vehicleId, Point currentPosition);
    
    /**
     * Obtenir le statut de suivi de toutes les routes d'un véhicule
     */
    Flux<RouteTrackingStatus> getVehicleRouteTrackingStatus(UUID vehicleId);
    
    /**
     * Calculer la sévérité d'une déviation
     */
    Mono<String> calculateDeviationSeverity(Double deviationDistance, Double toleranceDistance);
    
    /**
     * Détecter les véhicules qui dévient de leurs routes
     */
    Flux<RouteDeviationAlert> detectAllVehicleDeviations();
    
    /**
     * Obtenir les alertes de déviation récentes pour un utilisateur
     */
    Flux<RouteDeviationAlert> getRecentDeviationAlerts(UUID userId, int limitHours);
    
    /**
     * Marquer une alerte comme traitée
     */
    Mono<Void> markAlertAsHandled(UUID alertId);
    
    /**
     * Configurer les paramètres de détection pour un véhicule
     */
    Mono<Void> configureDeviationSettings(UUID vehicleId, Double customTolerance, Integer checkIntervalSeconds);
    
    /**
     * Prédire si un véhicule va probablement dévier de sa route
     */
    Mono<Boolean> predictPotentialDeviation(UUID vehicleId, Point currentPosition, Point nextExpectedPosition);
}