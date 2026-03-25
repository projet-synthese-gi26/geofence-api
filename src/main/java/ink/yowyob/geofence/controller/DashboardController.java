package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.response.DashboardStatsDTO;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    /**
     * @api {get} /dashboard/stats Récupérer les statistiques du dashboard
     * @apiName GetDashboardStats
     * @apiGroup Dashboard
     * @apiVersion 2.0.0
     * @apiDescription Récupère les statistiques du dashboard pour l'utilisateur connecté
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Number} totalVehicles Nombre total de véhicules
     * @apiSuccess {Number} totalGeofences Nombre total de géofences
     * @apiSuccess {Number} activeAlerts Nombre d'alertes actives
     * @apiSuccess {Number} totalRoutes Nombre total de routes
     * @apiSuccess {Object} recentActivity Activité récente
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "totalVehicles": 5,
     *       "totalGeofences": 12,
     *       "activeAlerts": 2,
     *       "totalRoutes": 3,
     *       "recentActivity": {
     *         "lastLocationUpdate": "2024-01-15T10:30:00Z",
     *         "lastAlert": "2024-01-15T09:45:00Z"
     *       }
     *     }
     *
     * @apiUse ErrorResponse
     */
    /**
     * Récupérer les statistiques du dashboard
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<DashboardStatsDTO>> getDashboardStats() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return dashboardService.getUserDashboardStats(user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}