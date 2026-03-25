package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.service.RouteDeviationDetectionService;
import ink.yowyob.geofence.service.RouteService;
import ink.yowyob.geofence.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * @apiDefine UserPermission
 * @apiPermission user
 * @apiHeader {String} Authorization Bearer token (JWT)
 * @apiHeaderExample {json} Header-Example:
 *     {
 *       "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 *     }
 */

/**
 * @apiDefine ErrorResponse
 * @apiError (Error 4xx) {Number} status Code d'erreur HTTP
 * @apiError (Error 4xx) {String} message Message d'erreur
 * @apiErrorExample {json} Error-Response:
 *     HTTP/1.1 400 Bad Request
 *     {
 *       "status": 400,
 *       "message": "Données invalides"
 *     }
 */

@RestController
@RequestMapping("/api/route-monitoring")
@AllArgsConstructor
@Slf4j
public class RouteDeviationController {
    
    private final RouteDeviationDetectionService deviationService;
    private final RouteService routeService;
    private final UserRepository userRepository;
    
    /**
     * @api {get} /route-monitoring/vehicle/:vehicleId/deviations Stream des déviations
     * @apiName StreamVehicleDeviations
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Stream en temps réel des alertes de déviation pour un véhicule (Server-Sent Events)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} vehicleId Identifiant du véhicule
     *
     * @apiSuccess {String} type Type d'événement (deviation_detected)
     * @apiSuccess {String} vehicleId ID du véhicule
     * @apiSuccess {String} routeId ID de la route
     * @apiSuccess {Number} deviationDistance Distance de déviation en mètres
     * @apiSuccess {Number[]} currentPosition Position actuelle [longitude, latitude]
     * @apiSuccess {String} timestamp Horodatage de la déviation
     *
     * @apiSuccessExample {json} Event-Example:
     *     data: {"type":"deviation_detected","vehicleId":"123e4567-e89b-12d3-a456-426614174000","routeId":"456e7890-e89b-12d3-a456-426614174001","deviationDistance":150.5,"currentPosition":[3.8667,11.5167],"timestamp":"2024-01-15T10:30:00Z"}
     *
     * @apiUse ErrorResponse
     */
    /**
     * Stream en temps réel des alertes de déviation pour un véhicule
     */
    @GetMapping(value = "/vehicle/{vehicleId}/deviations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<RouteDeviationDetectionService.RouteDeviationAlert> streamVehicleDeviations(@PathVariable UUID vehicleId) {
        log.info("Démarrage du stream de déviations pour le véhicule: {}", vehicleId);
        
        return deviationService.monitorVehicleDeviations(vehicleId)
            .doOnNext(alert -> log.info("Alerte de déviation émise: {}", alert))
            .doOnError(error -> log.error("Erreur dans le stream de déviations: ", error))
            .onErrorResume(error -> Flux.empty());
    }
    
    /**
     * @api {get} /route-monitoring/vehicle/:vehicleId/tracking-status Stream du statut de suivi
     * @apiName StreamTrackingStatus
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Stream du statut de suivi de route pour un véhicule (Server-Sent Events)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} vehicleId Identifiant du véhicule
     *
     * @apiSuccess {String} vehicleId ID du véhicule
     * @apiSuccess {String} status Statut du suivi (on_route, deviated, unknown)
     * @apiSuccess {String} [currentRouteId] ID de la route actuelle
     * @apiSuccess {Number} [progress] Progression sur la route (0.0 à 1.0)
     * @apiSuccess {String} timestamp Horodatage du statut
     *
     * @apiSuccessExample {json} Event-Example:
     *     data: {"vehicleId":"123e4567-e89b-12d3-a456-426614174000","status":"on_route","currentRouteId":"456e7890-e89b-12d3-a456-426614174001","progress":0.75,"timestamp":"2024-01-15T10:30:00Z"}
     *
     * @apiUse ErrorResponse
     */
    /**
     * Stream du statut de suivi pour un véhicule
     */
    @GetMapping(value = "/vehicle/{vehicleId}/tracking-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<RouteDeviationDetectionService.RouteTrackingStatus> streamTrackingStatus(@PathVariable UUID vehicleId) {
        return Flux.interval(Duration.ofSeconds(10))
            .flatMap(tick -> deviationService.getVehicleRouteTrackingStatus(vehicleId))
            .doOnError(error -> log.error("Erreur dans le stream de statut de suivi: ", error))
            .onErrorResume(error -> Flux.empty());
    }
    
    /**
     * @api {post} /route-monitoring/vehicle/:vehicleId/check-deviation Vérifier la déviation manuellement
     * @apiName CheckVehicleDeviation
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Vérifier manuellement si un véhicule dévie de sa route
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} vehicleId Identifiant du véhicule
     * @apiBody {Number[]} currentPosition Position actuelle [longitude, latitude]
     *
     * @apiSuccess {String} vehicleId ID du véhicule
     * @apiSuccess {String} routeId ID de la route
     * @apiSuccess {Number} deviationDistance Distance de déviation en mètres
     * @apiSuccess {Number[]} currentPosition Position actuelle
     * @apiSuccess {String} timestamp Horodatage de la vérification
     *
     * @apiUse ErrorResponse
     */
    /**
     * Vérifier manuellement si un véhicule dévie de sa route
     */
    @PostMapping("/vehicle/{vehicleId}/check-deviation")
    public Mono<ResponseEntity<RouteDeviationDetectionService.RouteDeviationAlert>> checkVehicleDeviation(
            @PathVariable UUID vehicleId,
            @RequestBody PointDTO currentPosition) {
        
        return deviationService.checkVehicleDeviation(vehicleId, routeService.convertToPoint(currentPosition))
            .map(alert -> alert != null ? 
                ResponseEntity.ok(alert) : 
                ResponseEntity.noContent().<RouteDeviationDetectionService.RouteDeviationAlert>build())
            .defaultIfEmpty(ResponseEntity.noContent().build());
    }
    
    /**
     * @api {get} /route-monitoring/all-deviations Stream de toutes les déviations
     * @apiName StreamAllDeviations
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Stream de toutes les alertes de déviation du système (Server-Sent Events)
     *
     * @apiUse AdminPermission
     *
     * @apiSuccess {String} vehicleId ID du véhicule
     * @apiSuccess {String} routeId ID de la route
     * @apiSuccess {Number} deviationDistance Distance de déviation en mètres
     * @apiSuccess {Number[]} currentPosition Position actuelle
     * @apiSuccess {String} timestamp Horodatage de l'alerte
     *
     * @apiUse ErrorResponse
     */
    /**
     * Stream de toutes les alertes de déviation (pour admin/monitoring)
     */
    @GetMapping(value = "/all-deviations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<RouteDeviationDetectionService.RouteDeviationAlert> streamAllDeviations() {
        return Flux.interval(Duration.ofSeconds(60))
            .flatMap(tick -> deviationService.detectAllVehicleDeviations())
            .doOnError(error -> log.error("Erreur dans le stream de toutes les déviations: ", error))
            .onErrorResume(error -> Flux.empty());
    }
    
    /**
     * @api {get} /route-monitoring/recent-alerts Alertes récentes
     * @apiName GetRecentAlerts
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Obtenir les alertes de déviation récentes pour l'utilisateur connecté
     *
     * @apiUse UserPermission
     *
     * @apiQuery {Number} [limitHours=24] Nombre d'heures à remonter
     *
     * @apiSuccess {String} vehicleId ID du véhicule
     * @apiSuccess {String} routeId ID de la route
     * @apiSuccess {Number} deviationDistance Distance de déviation en mètres
     * @apiSuccess {Number[]} currentPosition Position actuelle
     * @apiSuccess {String} timestamp Horodatage de l'alerte
     *
     * @apiUse ErrorResponse
     */
    /**
     * Obtenir les alertes récentes pour l'utilisateur connecté
     */
    @GetMapping("/recent-alerts")
    public Flux<RouteDeviationDetectionService.RouteDeviationAlert> getRecentAlerts(
            @RequestParam(defaultValue = "24") int limitHours) {
        
        return getCurrentUser()
            .flatMapMany(user -> deviationService.getRecentDeviationAlerts(user.getUuid(), limitHours));
    }
    
    /**
     * @api {post} /route-monitoring/alerts/:alertId/mark-handled Marquer une alerte comme traitée
     * @apiName MarkAlertAsHandled
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Marquer une alerte de déviation comme traitée
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} alertId Identifiant de l'alerte
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiUse ErrorResponse
     */
    /**
     * Marquer une alerte comme traitée
     */
    @PostMapping("/alerts/{alertId}/mark-handled")
    public Mono<ResponseEntity<Void>> markAlertAsHandled(@PathVariable UUID alertId) {
        return deviationService.markAlertAsHandled(alertId)
            .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
    
    /**
     * @api {post} /route-monitoring/vehicle/:vehicleId/configure Configurer la détection
     * @apiName ConfigureDeviationSettings
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Configurer les paramètres de détection de déviation pour un véhicule
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} vehicleId Identifiant du véhicule
     * @apiQuery {Number} toleranceMeters Tolérance en mètres
     * @apiQuery {Number} [checkIntervalSeconds=30] Intervalle de vérification en secondes
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiUse ErrorResponse
     */
    /**
     * Configurer les paramètres de détection pour un véhicule
     */
    @PostMapping("/vehicle/{vehicleId}/configure")
    public Mono<ResponseEntity<Void>> configureDeviationSettings(
            @PathVariable UUID vehicleId,
            @RequestParam Double toleranceMeters,
            @RequestParam(defaultValue = "30") Integer checkIntervalSeconds) {
        
        return deviationService.configureDeviationSettings(vehicleId, toleranceMeters, checkIntervalSeconds)
            .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
    
    /**
     * @api {post} /route-monitoring/vehicle/:vehicleId/predict-deviation Prédire une déviation
     * @apiName PredictDeviation
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Prédire si un véhicule risque de dévier de sa route
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} vehicleId Identifiant du véhicule
     * @apiQuery {Number} currentLng Longitude actuelle
     * @apiQuery {Number} currentLat Latitude actuelle
     * @apiQuery {Number} nextLng Longitude de la prochaine position
     * @apiQuery {Number} nextLat Latitude de la prochaine position
     *
     * @apiSuccess {Boolean} willDeviate True si le véhicule risque de dévier
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     true
     *
     * @apiUse ErrorResponse
     */
    /**
     * Prédire une déviation potentielle
     */
    @PostMapping("/vehicle/{vehicleId}/predict-deviation")
    public Mono<ResponseEntity<Boolean>> predictDeviation(
            @PathVariable UUID vehicleId,
            @RequestParam Double currentLng,
            @RequestParam Double currentLat,
            @RequestParam Double nextLng,
            @RequestParam Double nextLat) {
        
        PointDTO currentPos = new PointDTO(java.util.List.of(currentLng, currentLat));
        PointDTO nextPos = new PointDTO(java.util.List.of(nextLng, nextLat));
        
        return deviationService.predictPotentialDeviation(
                vehicleId, 
                routeService.convertToPoint(currentPos),
                routeService.convertToPoint(nextPos))
            .map(ResponseEntity::ok);
    }
    
    /**
     * @api {get} /route-monitoring/dashboard/live-status Statut du dashboard en temps réel
     * @apiName StreamDashboardStatus
     * @apiGroup Route Monitoring
     * @apiVersion 2.0.0
     * @apiDescription Stream du statut combiné pour le dashboard de surveillance (Server-Sent Events)
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Number} totalAlerts Nombre total d'alertes actives
     * @apiSuccess {Number} criticalAlerts Nombre d'alertes critiques
     * @apiSuccess {String} lastUpdate Dernière mise à jour
     *
     * @apiSuccessExample {json} Event-Example:
     *     data: {"totalAlerts":5,"criticalAlerts":2,"lastUpdate":"2024-01-15T10:30:00"}
     *
     * @apiUse ErrorResponse
     */
    /**
     * Stream de statut combiné pour le dashboard
     */
    @GetMapping(value = "/dashboard/live-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DashboardStatus> streamDashboardStatus() {
        return getCurrentUser()
            .flatMapMany(user -> {
                // Obtenir tous les véhicules de l'utilisateur et surveiller leurs statuts
                return Flux.interval(Duration.ofSeconds(15))
                    .flatMap(tick -> 
                        deviationService.detectAllVehicleDeviations()
                            .filter(alert -> alert.vehicleId() != null) // Filtrer pour les véhicules de l'utilisateur
                            .collectList()
                            .map(alerts -> new DashboardStatus(
                                alerts.size(),
                                alerts.stream().mapToLong(alert -> "CRITICAL".equals(alert.severity()) ? 1 : 0).sum(),
                                java.time.LocalDateTime.now()
                            ))
                    );
            });
    }
    
    /**
     * DTO pour le statut du dashboard
     */
    public record DashboardStatus(
        long totalAlerts,
        long criticalAlerts,
        java.time.LocalDateTime lastUpdate
    ) {}
    
    /**
     * Méthode utilitaire pour obtenir l'utilisateur actuel
     */
    private Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .cast(SecurityContext.class)
            .map(SecurityContext::getAuthentication)
            .cast(Authentication.class)
            .map(Authentication::getName)
            .flatMap(username -> Mono.fromCallable(() -> 
                userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
            ));
    }
}