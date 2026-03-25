package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.LineStringDTO;
import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.request.RouteDTORequest;
import ink.yowyob.geofence.dto.response.MultipleRoutesDTOResponse;
import ink.yowyob.geofence.dto.response.RouteDTOResponse;
import ink.yowyob.geofence.model.User;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface RouteService {
    
    // CRUD operations
    Mono<RouteDTOResponse> createRoute(RouteDTORequest routeRequest, User user);
    
    Mono<RouteDTOResponse> getRoute(UUID routeId, User user);
    
    Mono<RouteDTOResponse> updateRoute(UUID routeId, RouteDTORequest routeRequest, User user);
    
    Mono<Void> deleteRoute(UUID routeId, User user);
    
    // List operations
    Mono<MultipleRoutesDTOResponse> getUserRoutes(User user);
    
    Mono<MultipleRoutesDTOResponse> getAllRoutes(User user);
    
    Mono<MultipleRoutesDTOResponse> searchRoutes(String keyword, User user);
    
    Flux<RouteDTOResponse> getActiveRoutesByVehicle(UUID vehicleId, User user);
    
    // Vehicle assignment
    Mono<RouteDTOResponse> assignVehicleToRoute(UUID routeId, UUID vehicleId, User user);
    
    Mono<RouteDTOResponse> removeVehicleFromRoute(UUID routeId, UUID vehicleId, User user);
    
    // Route analysis and monitoring
    Mono<Boolean> isVehicleOnRoute(UUID vehicleId, Point currentPosition, User user);
    
    Mono<Double> calculateDeviationDistance(UUID routeId, Point currentPosition, User user);
    
    Flux<UUID> detectRouteDeviations(UUID vehicleId, Point currentPosition, Double toleranceMeters, User user);
    
    Mono<Double> calculateRouteProgress(UUID routeId, Point currentPosition, User user);
    
    // Utility methods
    Point convertToPoint(PointDTO dto);
    
    LineString convertToLineString(LineStringDTO dto);
    
    // Route optimization
    Mono<List<RouteDTOResponse>> suggestAlternativeRoutes(Point startPoint, Point endPoint, User user);
    
    Mono<RouteDTOResponse> optimizeRouteSegments(UUID routeId, User user);
}