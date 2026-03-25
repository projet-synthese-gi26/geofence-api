package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.response.RouteDTOResponse;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.model.Route;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.FileStorageService;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RouteDTOMapper implements Function<Route, RouteDTOResponse> {
    private final UserDTOMapper userDTOMapper;
    private final RouteSegmentDTOMapper routeSegmentDTOMapper;
    private final VehicleRepository vehicleRepository;
    private final FileStorageService fileStorageService;

    public RouteDTOMapper(UserDTOMapper userDTOMapper, 
                         RouteSegmentDTOMapper routeSegmentDTOMapper,
                         VehicleRepository vehicleRepository, 
                         FileStorageService fileStorageService) {
        this.userDTOMapper = userDTOMapper;
        this.routeSegmentDTOMapper = routeSegmentDTOMapper;
        this.vehicleRepository = vehicleRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public RouteDTOResponse apply(Route route) {
        return new RouteDTOResponse(
            route.getId(),
            userDTOMapper.apply(route.getUser()),
            route.getName(),
            route.getDescription(),
            convertPointToDTO(route.getStartPoint()),
            route.getStartAddress(),
            convertPointToDTO(route.getEndPoint()),
            route.getEndAddress(),
            route.getEstimatedDistance(),
            route.getEstimatedDuration(),
            route.getDeviationTolerance(),
            
            // Propriétés temporelles
            route.getIsTemporalEnabled(),
            route.getStartTime() != null ? route.getStartTime().toString() : null,
            route.getEndTime() != null ? route.getEndTime().toString() : null,
            safeGetActiveDays(route),
            
            // Segments de route
            safeGetAuthorizedSegments(route),
            
            // Véhicules assignés
            safeGetAssignedVehicles(route),
            
            route.getIsActive(),
            route.getCreatedAt(),
            route.getUpdatedAt()
        );
    }

    private SimpleVehicleDTO mapToSimpleVehicleDTO(Vehicle vehicle) {
        String imageUrl = vehicle.getImageUrl() != null
                ? fileStorageService.getCompleteImageUrl(vehicle.getImageUrl())
                : null;

        return new SimpleVehicleDTO(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                imageUrl
        );
    }

    private PointDTO convertPointToDTO(Point point) {
        if (point == null) return null;
        return new PointDTO(List.of(point.getX(), point.getY()));
    }
    
    private String[] safeGetActiveDays(Route route) {
        try {
            if (route.getActiveDays() != null && !route.getActiveDays().isEmpty()) {
                return route.getActiveDays().stream().map(Enum::name).toArray(String[]::new);
            }
        } catch (Exception e) {
            // LazyInitializationException or other Hibernate exceptions
        }
        return new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
    }
    
    private List safeGetAuthorizedSegments(Route route) {
        try {
            if (route.getAuthorizedSegments() != null) {
                return route.getAuthorizedSegments().stream()
                    .map(routeSegmentDTOMapper)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // LazyInitializationException or other Hibernate exceptions
        }
        return List.of();
    }
    
    private List<SimpleVehicleDTO> safeGetAssignedVehicles(Route route) {
        try {
            if (route.getAssignedVehicles() != null) {
                return route.getAssignedVehicles().stream()
                    .map(this::mapToSimpleVehicleDTO)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // LazyInitializationException or other Hibernate exceptions
        }
        return List.of();
    }
}