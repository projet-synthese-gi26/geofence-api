package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.model.Route;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.repository.RouteRepository;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.RouteDeviationDetectionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class RouteDeviationDetectionServiceImpl implements RouteDeviationDetectionService {
    
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    
    @Override
    public Flux<RouteDeviationAlert> monitorVehicleDeviations(UUID vehicleId) {
        log.info("Démarrage du monitoring des déviations pour le véhicule: {}", vehicleId);
        
        return Flux.interval(Duration.ofSeconds(30)) // Vérifier toutes les 30 secondes
            .flatMap(tick -> {
                // Dans un vrai système, on obtiendrait la position actuelle du GPS
                // Ici, on simule avec une position fictive pour la démonstration
                return getCurrentVehiclePosition(vehicleId)
                    .flatMap(position -> checkVehicleDeviation(vehicleId, position))
                    .onErrorResume(error -> {
                        log.warn("Erreur lors de la vérification de déviation pour le véhicule {}: {}", 
                                vehicleId, error.getMessage());
                        return Mono.empty();
                    });
            })
            .filter(alert -> alert != null);
    }
    
    @Override
    public Mono<RouteDeviationAlert> checkVehicleDeviation(UUID vehicleId, Point currentPosition) {
        return Mono.fromCallable(() -> {
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle == null) {
                return null;
            }
            
            // Obtenir toutes les routes actives assignées au véhicule
            return routeRepository.findActiveRoutesByVehicleId(vehicleId)
                .stream()
                .filter(route -> route.isActiveAt(LocalDateTime.now()))
                .filter(route -> !route.isPointWithinTolerance(currentPosition))
                .findFirst()
                .map(route -> {
                    double deviationDistance = calculateMinDistanceToRoute(route, currentPosition);
                    String severity = calculateSeveritySync(deviationDistance, route.getDeviationTolerance());
                    
                    return new RouteDeviationAlert(
                        vehicleId,
                        vehicle.getBrand() + " " + vehicle.getModel() + " (" + vehicle.getLicensePlate() + ")",
                        route.getId(),
                        route.getName(),
                        currentPosition,
                        deviationDistance,
                        route.getDeviationTolerance(),
                        LocalDateTime.now(),
                        severity,
                        generateDeviationMessage(vehicle, route, deviationDistance, severity)
                    );
                })
                .orElse(null);
        });
    }
    
    @Override
    public Flux<RouteTrackingStatus> getVehicleRouteTrackingStatus(UUID vehicleId) {
        return Mono.fromCallable(() -> routeRepository.findActiveRoutesByVehicleId(vehicleId))
            .flatMapMany(Flux::fromIterable)
            .flatMap(route -> 
                getCurrentVehiclePosition(vehicleId)
                    .map(currentPosition -> {
                        boolean isOnRoute = route.isPointWithinTolerance(currentPosition);
                        double progressPercentage = calculateRouteProgressSync(route, currentPosition);
                        double deviationDistance = isOnRoute ? 0.0 : calculateMinDistanceToRoute(route, currentPosition);
                        String currentSegment = findCurrentSegment(route, currentPosition);
                        
                        return new RouteTrackingStatus(
                            vehicleId,
                            route.getId(),
                            isOnRoute,
                            progressPercentage,
                            deviationDistance,
                            currentSegment,
                            LocalDateTime.now()
                        );
                    })
            );
    }
    
    @Override
    public Mono<String> calculateDeviationSeverity(Double deviationDistance, Double toleranceDistance) {
        return Mono.fromCallable(() -> calculateSeveritySync(deviationDistance, toleranceDistance));
    }
    
    private String calculateSeveritySync(Double deviationDistance, Double toleranceDistance) {
        if (deviationDistance <= toleranceDistance) {
            return "NONE";
        } else if (deviationDistance <= toleranceDistance * 2) {
            return "LOW";
        } else if (deviationDistance <= toleranceDistance * 5) {
            return "MEDIUM";
        } else if (deviationDistance <= toleranceDistance * 10) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }
    
    @Override
    public Flux<RouteDeviationAlert> detectAllVehicleDeviations() {
        return Flux.fromIterable(vehicleRepository.findAll())
            .flatMap(vehicle -> 
                getCurrentVehiclePosition(vehicle.getId())
                    .flatMap(position -> checkVehicleDeviation(vehicle.getId(), position))
                    .filter(alert -> alert != null)
            );
    }
    
    @Override
    public Flux<RouteDeviationAlert> getRecentDeviationAlerts(UUID userId, int limitHours) {
        // Dans un vrai système, on aurait une table d'alertes persistées
        // Ici, on retourne un flux vide pour la démonstration
        return Flux.empty();
    }
    
    @Override
    public Mono<Void> markAlertAsHandled(UUID alertId) {
        // Dans un vrai système, on marquerait l'alerte comme traitée en base
        return Mono.fromRunnable(() -> 
            log.info("Alerte {} marquée comme traitée", alertId));
    }
    
    @Override
    public Mono<Void> configureDeviationSettings(UUID vehicleId, Double customTolerance, Integer checkIntervalSeconds) {
        // Dans un vrai système, on sauvegarderait ces paramètres pour le véhicule
        return Mono.fromRunnable(() -> 
            log.info("Configuration de déviation mise à jour pour le véhicule {}: tolérance={}, interval={}", 
                    vehicleId, customTolerance, checkIntervalSeconds));
    }
    
    @Override
    public Mono<Boolean> predictPotentialDeviation(UUID vehicleId, Point currentPosition, Point nextExpectedPosition) {
        return Mono.fromCallable(() -> {
            // Algorithme simple de prédiction basé sur la trajectoire
            return routeRepository.findActiveRoutesByVehicleId(vehicleId)
                .stream()
                .anyMatch(route -> {
                    boolean currentlyOnRoute = route.isPointWithinTolerance(currentPosition);
                    boolean nextPositionOnRoute = route.isPointWithinTolerance(nextExpectedPosition);
                    
                    // Si actuellement sur la route mais la prochaine position ne l'est pas
                    return currentlyOnRoute && !nextPositionOnRoute;
                });
        });
    }
    
    /**
     * Simule l'obtention de la position actuelle d'un véhicule
     * Dans un vrai système, ceci viendrait d'un service GPS/tracking
     */
    private Mono<Point> getCurrentVehiclePosition(UUID vehicleId) {
        return Mono.fromCallable(() -> {
            // Position fictive pour la démonstration
            // Dans un vrai système, on obtiendrait ceci d'un service de tracking
            org.locationtech.jts.geom.GeometryFactory factory = 
                new org.locationtech.jts.geom.GeometryFactory(new org.locationtech.jts.geom.PrecisionModel(), 4326);
            return factory.createPoint(new org.locationtech.jts.geom.Coordinate(-74.006, 40.7128)); // NYC
        });
    }
    
    /**
     * Calcule la distance minimale d'un point à une route
     */
    private double calculateMinDistanceToRoute(Route route, Point position) {
        return route.getAuthorizedSegments().stream()
            .filter(segment -> segment.getIsActive() && segment.getPathLine() != null)
            .mapToDouble(segment -> segment.getPathLine().distance(position))
            .min()
            .orElse(Double.MAX_VALUE) * 111000.0; // Convertir en mètres
    }
    
    /**
     * Calcule le pourcentage de progression sur une route
     */
    private double calculateRouteProgressSync(Route route, Point position) {
        if (route.getStartPoint() == null || route.getEndPoint() == null) {
            return 0.0;
        }
        
        double totalDistance = route.getStartPoint().distance(route.getEndPoint());
        double currentDistance = route.getStartPoint().distance(position);
        
        if (totalDistance == 0) return 100.0;
        
        return Math.min(100.0, (currentDistance / totalDistance) * 100.0);
    }
    
    /**
     * Trouve le segment actuel sur lequel se trouve le véhicule
     */
    private String findCurrentSegment(Route route, Point position) {
        return route.getAuthorizedSegments().stream()
            .filter(segment -> segment.getIsActive())
            .filter(segment -> segment.isPointWithinTolerance(position, route.getDeviationTolerance()))
            .findFirst()
            .map(segment -> segment.getName() != null ? segment.getName() : "Segment " + segment.getSegmentOrder())
            .orElse("Hors route");
    }
    
    /**
     * Génère un message d'alerte de déviation
     */
    private String generateDeviationMessage(Vehicle vehicle, Route route, double deviationDistance, String severity) {
        String vehicleName = vehicle.getBrand() + " " + vehicle.getModel() + " (" + vehicle.getLicensePlate() + ")";
        
        return switch (severity) {
            case "LOW" -> String.format("Le véhicule %s s'écarte légèrement de la route '%s' (%.1fm)", 
                    vehicleName, route.getName(), deviationDistance);
            case "MEDIUM" -> String.format("Le véhicule %s dévie de la route '%s' (%.1fm)", 
                    vehicleName, route.getName(), deviationDistance);
            case "HIGH" -> String.format("ATTENTION: Le véhicule %s s'écarte significativement de la route '%s' (%.1fm)", 
                    vehicleName, route.getName(), deviationDistance);
            case "CRITICAL" -> String.format("ALERTE CRITIQUE: Le véhicule %s est très loin de la route '%s' (%.1fm)", 
                    vehicleName, route.getName(), deviationDistance);
            default -> String.format("Le véhicule %s suit la route '%s' correctement", 
                    vehicleName, route.getName());
        };
    }
}