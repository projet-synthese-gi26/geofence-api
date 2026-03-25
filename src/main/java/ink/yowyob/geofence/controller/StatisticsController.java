package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.response.SystemStatisticsDTO;
import ink.yowyob.geofence.dto.response.UserStatisticsDTO;
import ink.yowyob.geofence.dto.response.VehicleStatisticsDTO;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.Implementation.StatisticsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
 * @apiDefine AdminPermission
 * @apiPermission admin
 * @apiHeader {String} Authorization Bearer token (JWT) - Rôle ADMIN requis
 * @apiHeaderExample {json} Header-Example:
 *     {
 *       "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 *     }
 */

/**
 * @apiDefine ManagerPermission
 * @apiPermission manager
 * @apiHeader {String} Authorization Bearer token (JWT) - Rôle ADMIN ou MANAGER requis
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
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsServiceImpl statisticsService;
    private final UserRepository userRepository;

    private Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() ->
                        userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * @api {get} /statistics/system Statistiques système
     * @apiName GetSystemStatistics
     * @apiGroup Statistics
     * @apiVersion 2.0.0
     * @apiDescription Récupère les statistiques globales du système (accès admin/manager)
     *
     * @apiUse ManagerPermission
     *
     * @apiSuccess {Number} totalUsers Nombre total d'utilisateurs
     * @apiSuccess {Number} totalVehicles Nombre total de véhicules
     * @apiSuccess {Number} totalGeofenceZones Nombre total de géofences
     * @apiSuccess {Number} totalAlerts Nombre total d'alertes
     * @apiSuccess {Object[]} alertsByType Répartition des alertes par type
     * @apiSuccess {String} alertsByType.type Type d'alerte
     * @apiSuccess {Number} alertsByType.count Nombre d'alertes
     * @apiSuccess {Object} alertsPerDay Alertes par jour (30 derniers jours)
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "totalUsers": 150,
     *       "totalVehicles": 89,
     *       "totalGeofenceZones": 45,
     *       "totalAlerts": 234,
     *       "alertsByType": [
     *         {"type": "ZONE_EXIT", "count": 98},
     *         {"type": "ZONE_ENTER", "count": 76},
     *         {"type": "SPEED_LIMIT", "count": 45},
     *         {"type": "BATTERY_LOW", "count": 12},
     *         {"type": "SYSTEM_ERROR", "count": 3}
     *       ],
     *       "alertsPerDay": {
     *         "2024-01-07": 12,
     *         "2024-01-08": 8
     *       }
     *     }
     *
     * @apiError (Error 403) Forbidden Accès refusé - Droits insuffisants
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /statistics/system Statistiques système
     * @apiName GetSystemStatisticsV2
     * @apiGroup Statistics
     * @apiVersion 2.0.0
     * @apiDescription Récupère les statistiques globales du système (accès admin/manager) (version 2.0.0 avec améliorations)
     *
     * @apiUse ManagerPermission
     *
     * @apiSuccess {Number} totalUsers Nombre total d'utilisateurs
     * @apiSuccess {Number} totalVehicles Nombre total de véhicules
     * @apiSuccess {Number} totalGeofenceZones Nombre total de géofences
     * @apiSuccess {Number} totalAlerts Nombre total d'alertes
     * @apiSuccess {Object[]} alertsByType Répartition des alertes par type
     * @apiSuccess {String} alertsByType.type Type d'alerte
     * @apiSuccess {Number} alertsByType.count Nombre d'alertes
     * @apiSuccess {Object} alertsPerDay Alertes par jour (30 derniers jours)
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "totalUsers": 150,
     *       "totalVehicles": 89,
     *       "totalGeofenceZones": 45,
     *       "totalAlerts": 234,
     *       "alertsByType": [
     *         {"type": "ZONE_EXIT", "count": 98},
     *         {"type": "ZONE_ENTER", "count": 76},
     *         {"type": "SPEED_LIMIT", "count": 45},
     *         {"type": "BATTERY_LOW", "count": 12},
     *         {"type": "SYSTEM_ERROR", "count": 3}
     *       ],
     *       "alertsPerDay": {
     *         "2024-01-07": 12,
     *         "2024-01-08": 8
     *       }
     *     }
     *
     * @apiError (Error 403) Forbidden Accès refusé - Droits insuffisants
     * @apiUse ErrorResponse
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Mono<ResponseEntity<SystemStatisticsDTO>> getSystemStatistics() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> statisticsService.getSystemStatistics(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /statistics/user Mes statistiques
     * @apiName GetUserStatistics
     * @apiGroup Statistics
     * @apiVersion 2.0.0
     * @apiDescription Récupère les statistiques de l'utilisateur connecté
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Number} totalVehicles Nombre de véhicules
     * @apiSuccess {Number} totalGeofenceZones Nombre de géofences
     * @apiSuccess {Number} totalAlerts Nombre total d'alertes
     * @apiSuccess {Object[]} alertsByType Répartition des alertes par type
     * @apiSuccess {Object} alertsPerDay Alertes par jour (30 derniers jours)
     * @apiSuccess {String} [mostActiveVehicleId] Véhicule le plus actif
     * @apiSuccess {String} [mostCommonAlertType] Type d'alerte le plus fréquent
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "totalVehicles": 3,
     *       "totalGeofenceZones": 5,
     *       "totalAlerts": 28,
     *       "alertsByType": [
     *         {"type": "ZONE_EXIT", "count": 15},
     *         {"type": "ZONE_ENTER", "count": 13}
     *       ],
     *       "alertsPerDay": {
     *         "2024-01-07": 4,
     *         "2024-01-08": 2
     *       },
     *       "mostActiveVehicleId": "123e4567-e89b-12d3-a456-426614174000",
     *       "mostCommonAlertType": "ZONE_EXIT"
     *     }
     *
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /statistics/user Mes statistiques
     * @apiName GetUserStatisticsV2
     * @apiGroup Statistics
     * @apiVersion 2.0.0
     * @apiDescription Récupère les statistiques de l'utilisateur connecté (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Number} totalVehicles Nombre de véhicules
     * @apiSuccess {Number} totalGeofenceZones Nombre de géofences
     * @apiSuccess {Number} totalAlerts Nombre total d'alertes
     * @apiSuccess {Object[]} alertsByType Répartition des alertes par type
     * @apiSuccess {Object} alertsPerDay Alertes par jour (30 derniers jours)
     * @apiSuccess {String} [mostActiveVehicleId] Véhicule le plus actif
     * @apiSuccess {String} [mostCommonAlertType] Type d'alerte le plus fréquent
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "totalVehicles": 3,
     *       "totalGeofenceZones": 5,
     *       "totalAlerts": 28,
     *       "alertsByType": [
     *         {"type": "ZONE_EXIT", "count": 15},
     *         {"type": "ZONE_ENTER", "count": 13}
     *       ],
     *       "alertsPerDay": {
     *         "2024-01-07": 4,
     *         "2024-01-08": 2
     *       },
     *       "mostActiveVehicleId": "123e4567-e89b-12d3-a456-426614174000",
     *       "mostCommonAlertType": "ZONE_EXIT"
     *     }
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/user")
    public Mono<ResponseEntity<UserStatisticsDTO>> getUserStatistics() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> statisticsService.getUserStatistics(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /statistics/vehicle/:vehicleId Statistiques d'un véhicule
     * @apiName GetVehicleStatistics
     * @apiGroup Statistics
     * @apiVersion 2.0.0
     * @apiDescription Récupère les statistiques d'un véhicule spécifique
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} vehicleId Identifiant du véhicule
     *
     * @apiSuccess {String} vehicleId Identifiant du véhicule
     * @apiSuccess {String} brand Marque du véhicule
     * @apiSuccess {String} model Modèle du véhicule
     * @apiSuccess {String} licensePlate Plaque d'immatriculation
     * @apiSuccess {Number} totalAlerts Nombre total d'alertes
     * @apiSuccess {Number} associatedGeofenceZones Nombre de géofences associées
     * @apiSuccess {Object[]} alertsByType Répartition des alertes par type
     * @apiSuccess {Object} alertsPerDay Alertes par jour (30 derniers jours)
     * @apiSuccess {String} [mostCommonAlertType] Type d'alerte le plus fréquent
     *
     * @apiError (Error 404) NotFound Véhicule non trouvé
     * @apiError (Error 403) Forbidden Accès refusé
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /statistics/vehicle/:vehicleId Statistiques d'un véhicule
     * @apiName GetVehicleStatisticsV2
     * @apiGroup Statistics
     * @apiVersion 2.0.0
     * @apiDescription Récupère les statistiques d'un véhicule spécifique (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} vehicleId Identifiant du véhicule
     *
     * @apiSuccess {String} vehicleId Identifiant du véhicule
     * @apiSuccess {String} brand Marque du véhicule
     * @apiSuccess {String} model Modèle du véhicule
     * @apiSuccess {String} licensePlate Plaque d'immatriculation
     * @apiSuccess {Number} totalAlerts Nombre total d'alertes
     * @apiSuccess {Number} associatedGeofenceZones Nombre de géofences associées
     * @apiSuccess {Object[]} alertsByType Répartition des alertes par type
     * @apiSuccess {Object} alertsPerDay Alertes par jour (30 derniers jours)
     * @apiSuccess {String} [mostCommonAlertType] Type d'alerte le plus fréquent
     *
     * @apiError (Error 404) NotFound Véhicule non trouvé
     * @apiError (Error 403) Forbidden Accès refusé
     * @apiUse ErrorResponse
     */
    @GetMapping("/vehicle/{vehicleId}")
    public Mono<ResponseEntity<VehicleStatisticsDTO>> getVehicleStatistics(@PathVariable UUID vehicleId) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> statisticsService.getVehicleStatistics(vehicleId, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }
}