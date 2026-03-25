package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.LineStringDTO;
import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.request.RouteDTORequest;
import ink.yowyob.geofence.dto.request.RouteSegmentDTORequest;
import ink.yowyob.geofence.dto.response.MultipleRoutesDTOResponse;
import ink.yowyob.geofence.dto.response.RouteDTOResponse;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.mapper.RouteDTOMapper;
import ink.yowyob.geofence.model.Route;
import ink.yowyob.geofence.model.RouteSegment;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.repository.RouteRepository;
import ink.yowyob.geofence.repository.RouteSegmentRepository;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.RouteService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class RouteServiceImpl implements RouteService {
    
    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteDTOMapper routeDTOMapper;
    
    @Override
    @Transactional
    public Mono<RouteDTOResponse> createRoute(RouteDTORequest routeRequest, User user) {
        return Mono.fromCallable(() -> {
            Route route = new Route();
            route.setUser(user);
            route.setName(routeRequest.name());
            route.setDescription(routeRequest.description());
            route.setStartPoint(convertToPoint(routeRequest.startPoint()));
            route.setStartAddress(routeRequest.startAddress());
            route.setEndPoint(convertToPoint(routeRequest.endPoint()));
            route.setEndAddress(routeRequest.endAddress());
            route.setEstimatedDistance(routeRequest.estimatedDistance());
            route.setEstimatedDuration(routeRequest.estimatedDuration());
            route.setDeviationTolerance(routeRequest.deviationTolerance() != null ? routeRequest.deviationTolerance() : 100.0);
            
            // Propriétés temporelles
            route.setIsTemporalEnabled(routeRequest.isTemporalEnabled() != null ? routeRequest.isTemporalEnabled() : false);
            if (routeRequest.startTime() != null) {
                route.setStartTime(LocalTime.parse(routeRequest.startTime()));
            }
            if (routeRequest.endTime() != null) {
                route.setEndTime(LocalTime.parse(routeRequest.endTime()));
            }
            if (routeRequest.activeDays() != null) {
                Set<ink.yowyob.geofence.Enum.DayOfWeek> activeDays = java.util.Arrays.stream(routeRequest.activeDays())
                    .map(day -> ink.yowyob.geofence.Enum.DayOfWeek.valueOf(day))
                    .collect(Collectors.toSet());
                route.setActiveDays(activeDays);
            }
            
            route.setIsActive(routeRequest.isActive() != null ? routeRequest.isActive() : true);
            
            Route savedRoute = routeRepository.save(route);
            
            // Créer les segments de route
            if (routeRequest.authorizedSegments() != null && !routeRequest.authorizedSegments().isEmpty()) {
                for (RouteSegmentDTORequest segmentRequest : routeRequest.authorizedSegments()) {
                    RouteSegment segment = new RouteSegment();
                    segment.setRoute(savedRoute);
                    segment.setName(segmentRequest.name());
                    segment.setDescription(segmentRequest.description());
                    segment.setPathLine(convertToLineString(segmentRequest.pathLine()));
                    segment.setSegmentOrder(segmentRequest.segmentOrder());
                    segment.setSegmentType(segmentRequest.segmentType() != null ? segmentRequest.segmentType() : RouteSegment.RouteSegmentType.MAIN_ROUTE);
                    segment.setPriority(segmentRequest.priority() != null ? segmentRequest.priority() : 1);
                    segment.setSpeedLimit(segmentRequest.speedLimit());
                    segment.setEstimatedTime(segmentRequest.estimatedTime());
                    segment.setIsActive(segmentRequest.isActive() != null ? segmentRequest.isActive() : true);
                    
                    routeSegmentRepository.save(segment);
                }
            }
            
            // Recharger la route avec ses segments, user et role
            savedRoute = routeRepository.findByIdWithUserAndRole(savedRoute.getId()).orElseThrow();
            
            return routeDTOMapper.apply(savedRoute);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<RouteDTOResponse> getRoute(UUID routeId, User user) {
        return Mono.fromCallable(() -> {
            Route route = routeRepository.findByIdWithUserAndRole(routeId)
                .orElseThrow(() -> new IllegalStateException("Route with id " + routeId + " not found"));

            assertSameOrganization(user, route.getUser());
            boolean isAuthorized = Objects.equals(route.getUser().getUuid(), user.getUuid()) ||
                    Objects.equals(user.getRole().getName(), UserRole.ADMIN) ||
                    Objects.equals(user.getRole().getName(), UserRole.MANAGER);
            if (!isAuthorized) {
                throw new BadCredentialsException("User " + user.getUsername() + " does not have permission to access this route");
            }
            return routeDTOMapper.apply(route);
        });
    }
    
    @Override
    @Transactional
    public Mono<RouteDTOResponse> updateRoute(UUID routeId, RouteDTORequest routeRequest, User user) {
        return Mono.fromCallable(() -> {
            Route route = routeRepository.findByIdWithUserAndRole(routeId)
                .orElseThrow(() -> new IllegalStateException("Route with id " + routeId + " not found"));

            assertSameOrganization(user, route.getUser());
                
            // Vérification des permissions
            if (!Objects.equals(route.getUser().getUuid(), user.getUuid()) && 
                !Objects.equals(user.getRole().getName(), UserRole.ADMIN)) {
                throw new BadCredentialsException("User " + user.getUsername() + " does not have permission to edit this route");
            }
            
            // Mise à jour des propriétés
            if (routeRequest.name() != null) {
                route.setName(routeRequest.name());
            }
            if (routeRequest.description() != null) {
                route.setDescription(routeRequest.description());
            }
            if (routeRequest.startPoint() != null) {
                route.setStartPoint(convertToPoint(routeRequest.startPoint()));
            }
            if (routeRequest.startAddress() != null) {
                route.setStartAddress(routeRequest.startAddress());
            }
            if (routeRequest.endPoint() != null) {
                route.setEndPoint(convertToPoint(routeRequest.endPoint()));
            }
            if (routeRequest.endAddress() != null) {
                route.setEndAddress(routeRequest.endAddress());
            }
            if (routeRequest.estimatedDistance() != null) {
                route.setEstimatedDistance(routeRequest.estimatedDistance());
            }
            if (routeRequest.estimatedDuration() != null) {
                route.setEstimatedDuration(routeRequest.estimatedDuration());
            }
            if (routeRequest.deviationTolerance() != null) {
                route.setDeviationTolerance(routeRequest.deviationTolerance());
            }
            
            // Mise à jour des propriétés temporelles
            if (routeRequest.isTemporalEnabled() != null) {
                route.setIsTemporalEnabled(routeRequest.isTemporalEnabled());
            }
            if (routeRequest.startTime() != null) {
                route.setStartTime(LocalTime.parse(routeRequest.startTime()));
            }
            if (routeRequest.endTime() != null) {
                route.setEndTime(LocalTime.parse(routeRequest.endTime()));
            }
            if (routeRequest.activeDays() != null) {
                Set<ink.yowyob.geofence.Enum.DayOfWeek> activeDays = java.util.Arrays.stream(routeRequest.activeDays())
                    .map(day -> ink.yowyob.geofence.Enum.DayOfWeek.valueOf(day))
                    .collect(Collectors.toSet());
                route.setActiveDays(activeDays);
            }
            
            if (routeRequest.isActive() != null) {
                route.setIsActive(routeRequest.isActive());
            }
            
            Route savedRoute = routeRepository.save(route);
            return routeDTOMapper.apply(savedRoute);
        });
    }
    
    @Override
    @Transactional
    public Mono<Void> deleteRoute(UUID routeId, User user) {
        return Mono.fromRunnable(() -> {
            Route route = routeRepository.findByIdWithUserAndRole(routeId)
                .orElseThrow(() -> new IllegalStateException("Route with id " + routeId + " not found"));

            assertSameOrganization(user, route.getUser());
                
            // Vérification des permissions
            if (!Objects.equals(route.getUser().getUuid(), user.getUuid()) && 
                !Objects.equals(user.getRole().getName(), UserRole.ADMIN)) {
                throw new BadCredentialsException("User " + user.getUsername() + " does not have permission to delete this route");
            }
            
            routeRepository.deleteById(routeId);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<MultipleRoutesDTOResponse> getUserRoutes(User user) {
        return Mono.fromCallable(() -> {
            List<RouteDTOResponse> routes = routeRepository.findByUser(user)
                .stream()
                .map(routeDTOMapper)
                .collect(Collectors.toList());
            return new MultipleRoutesDTOResponse(routes);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<MultipleRoutesDTOResponse> getAllRoutes(User user) {
        return Mono.fromCallable(() -> {
            List<RouteDTOResponse> routes = routeRepository.findByUser_Organization_IdWithUserAndRole(user.getOrganization().getId())
                .stream()
                .map(routeDTOMapper)
                .collect(Collectors.toList());
            return new MultipleRoutesDTOResponse(routes);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Mono<MultipleRoutesDTOResponse> searchRoutes(String keyword, User user) {
        return Mono.fromCallable(() -> {
            List<RouteDTOResponse> routes = routeRepository.searchUserRoutes(user, keyword)
                .stream()
                .map(routeDTOMapper)
                .collect(Collectors.toList());
            return new MultipleRoutesDTOResponse(routes);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Flux<RouteDTOResponse> getActiveRoutesByVehicle(UUID vehicleId, User user) {
        return Flux.fromIterable(
            routeRepository.findActiveRoutesByVehicleId(vehicleId)
                .stream()
                .filter(route -> {
                    assertSameOrganization(user, route.getUser());
                    return Objects.equals(route.getUser().getUuid(), user.getUuid()) ||
                            Objects.equals(user.getRole().getName(), UserRole.ADMIN) ||
                            Objects.equals(user.getRole().getName(), UserRole.MANAGER);
                })
                .map(routeDTOMapper)
                .collect(Collectors.toList())
        );
    }
    
    @Override
    @Transactional
    public Mono<RouteDTOResponse> assignVehicleToRoute(UUID routeId, UUID vehicleId, User user) {
        return Mono.fromCallable(() -> {
            Route route = routeRepository.findByIdWithUserAndRole(routeId)
                .orElseThrow(() -> new IllegalStateException("Route with id " + routeId + " not found"));
            Vehicle vehicle = vehicleRepository.findByIdWithRoutes(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + vehicleId + " not found"));

            assertSameOrganization(user, route.getUser());
            assertSameOrganization(user, vehicle.getUser());
                
            // Vérification des permissions
            if (!Objects.equals(route.getUser().getUuid(), user.getUuid()) && 
                !Objects.equals(user.getRole().getName(), UserRole.ADMIN)) {
                throw new BadCredentialsException("User " + user.getUsername() + " does not have permission to assign vehicles to this route");
            }
            
            route.getAssignedVehicles().add(vehicle);
            vehicle.getAssignedRoutes().add(route);
            
            Route savedRoute = routeRepository.save(route);
            vehicleRepository.save(vehicle);
            
            return routeDTOMapper.apply(savedRoute);
        });
    }
    
    @Override
    @Transactional
    public Mono<RouteDTOResponse> removeVehicleFromRoute(UUID routeId, UUID vehicleId, User user) {
        return Mono.fromCallable(() -> {
            Route route = routeRepository.findByIdWithUserAndRole(routeId)
                .orElseThrow(() -> new IllegalStateException("Route with id " + routeId + " not found"));
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + vehicleId + " not found"));

            assertSameOrganization(user, route.getUser());
            assertSameOrganization(user, vehicle.getUser());
                
            // Vérification des permissions
            if (!Objects.equals(route.getUser().getUuid(), user.getUuid()) && 
                !Objects.equals(user.getRole().getName(), UserRole.ADMIN)) {
                throw new BadCredentialsException("User " + user.getUsername() + " does not have permission to modify this route");
            }
            
            route.getAssignedVehicles().remove(vehicle);
            vehicle.getAssignedRoutes().remove(route);
            
            Route savedRoute = routeRepository.save(route);
            vehicleRepository.save(vehicle);
            
            return routeDTOMapper.apply(savedRoute);
        });
    }
    
    @Override
    public Mono<Boolean> isVehicleOnRoute(UUID vehicleId, Point currentPosition, User user) {
        return Mono.fromCallable(() -> {
            List<Route> activeRoutes = routeRepository.findActiveRoutesByVehicleId(vehicleId);
            
            return activeRoutes.stream()
                .peek(route -> assertSameOrganization(user, route.getUser()))
                .filter(route -> Objects.equals(route.getUser().getUuid(), user.getUuid()) ||
                        Objects.equals(user.getRole().getName(), UserRole.ADMIN) ||
                        Objects.equals(user.getRole().getName(), UserRole.MANAGER))
                .anyMatch(route -> route.isPointWithinTolerance(currentPosition));
        });
    }
    
    @Override
    public Mono<Double> calculateDeviationDistance(UUID routeId, Point currentPosition, User user) {
        return Mono.fromCallable(() -> {
            Route route = routeRepository.findByIdWithUserAndRole(routeId)
                .orElseThrow(() -> new IllegalStateException("Route with id " + routeId + " not found"));

            assertSameOrganization(user, route.getUser());
            boolean isAuthorized = Objects.equals(route.getUser().getUuid(), user.getUuid()) ||
                    Objects.equals(user.getRole().getName(), UserRole.ADMIN) ||
                    Objects.equals(user.getRole().getName(), UserRole.MANAGER);
            if (!isAuthorized) {
                throw new BadCredentialsException("User " + user.getUsername() + " does not have permission to access this route");
            }
                
            double minDistance = Double.MAX_VALUE;
            
            for (RouteSegment segment : route.getAuthorizedSegments()) {
                if (segment.getIsActive() && segment.getPathLine() != null) {
                    double distance = segment.getPathLine().distance(currentPosition);
                    minDistance = Math.min(minDistance, distance);
                }
            }
            
            // Convertir en mètres (approximation)
            return minDistance == Double.MAX_VALUE ? Double.MAX_VALUE : minDistance * 111000.0;
        });
    }
    
    @Override
    public Flux<UUID> detectRouteDeviations(UUID vehicleId, Point currentPosition, Double toleranceMeters, User user) {
        return Flux.fromIterable(
            routeRepository.findActiveRoutesByVehicleId(vehicleId)
                .stream()
                .peek(route -> assertSameOrganization(user, route.getUser()))
                .filter(route -> Objects.equals(route.getUser().getUuid(), user.getUuid()) ||
                        Objects.equals(user.getRole().getName(), UserRole.ADMIN) ||
                        Objects.equals(user.getRole().getName(), UserRole.MANAGER))
                .filter(route -> !route.isPointWithinTolerance(currentPosition))
                .map(Route::getId)
                .collect(Collectors.toList())
        );
    }
    
    @Override
    public Mono<Double> calculateRouteProgress(UUID routeId, Point currentPosition, User user) {
        return Mono.fromCallable(() -> {
            Route route = routeRepository.findByIdWithUserAndRole(routeId)
                .orElseThrow(() -> new IllegalStateException("Route with id " + routeId + " not found"));

            assertSameOrganization(user, route.getUser());
            boolean isAuthorized = Objects.equals(route.getUser().getUuid(), user.getUuid()) ||
                    Objects.equals(user.getRole().getName(), UserRole.ADMIN) ||
                    Objects.equals(user.getRole().getName(), UserRole.MANAGER);
            if (!isAuthorized) {
                throw new BadCredentialsException("User " + user.getUsername() + " does not have permission to access this route");
            }
                
            // Logique simplifiée : calculer la progression basée sur la distance du point de départ
            if (route.getStartPoint() == null || route.getEndPoint() == null) {
                return 0.0;
            }
            
            double totalDistance = route.getStartPoint().distance(route.getEndPoint());
            double currentDistance = route.getStartPoint().distance(currentPosition);
            
            if (totalDistance == 0) return 100.0;
            
            return Math.min(100.0, (currentDistance / totalDistance) * 100.0);
        });
    }
    
    @Override
    public Mono<List<RouteDTOResponse>> suggestAlternativeRoutes(Point startPoint, Point endPoint, User user) {
        // Implémentation simplifiée - dans un vrai système, on utiliserait un service de routing
        return Mono.fromCallable(() -> {
            return List.of(); // Pour l'instant, retourner une liste vide
        });
    }
    
    @Override
    public Mono<RouteDTOResponse> optimizeRouteSegments(UUID routeId, User user) {
        // Implémentation simplifiée - optimisation des segments de route
        return getRoute(routeId, user);
    }

    private void assertSameOrganization(User actor, User resourceOwner) {
        if (actor.getOrganization() == null || resourceOwner.getOrganization() == null ||
                !Objects.equals(actor.getOrganization().getId(), resourceOwner.getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant access denied");
        }
    }
    
    @Override
    public Point convertToPoint(PointDTO dto) {
        if (dto == null || dto.coordinates() == null || dto.coordinates().size() != 2) {
            return null;
        }
        
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        double longitude = dto.coordinates().get(0);
        double latitude = dto.coordinates().get(1);
        
        return factory.createPoint(new Coordinate(longitude, latitude));
    }
    
    @Override
    public LineString convertToLineString(LineStringDTO dto) {
        if (dto == null || dto.coordinates() == null || dto.coordinates().isEmpty()) {
            return null;
        }
        
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        
        List<Coordinate> coordinates = dto.coordinates().stream()
            .map(coord -> new Coordinate(coord.get(0), coord.get(1)))
            .collect(Collectors.toList());
            
        return factory.createLineString(coordinates.toArray(new Coordinate[0]));
    }
}