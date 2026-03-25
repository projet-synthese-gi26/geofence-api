package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.response.CircleGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.model.CircleGeofenceZone;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.FileStorageService;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CircleDTOMapper implements Function<CircleGeofenceZone, CircleGeofenceZoneDTOResponse> {
    private final UserDTOMapper userDTOMapper;
    private final VehicleRepository vehicleRepository;
    private final FileStorageService fileStorageService;

    public CircleDTOMapper(UserDTOMapper userDTOMapper, VehicleRepository vehicleRepository, FileStorageService fileStorageService) {
        this.userDTOMapper = userDTOMapper;
        this.vehicleRepository = vehicleRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public CircleGeofenceZoneDTOResponse apply(CircleGeofenceZone circleGeofenceZone) {
        // Récupérer les véhicules associés à cette zone
        List<SimpleVehicleDTO> vehicles = vehicleRepository.findByGeofenceZonesContaining(circleGeofenceZone)
                .stream()
                .map(this::mapToSimpleVehicleDTO)
                .collect(Collectors.toList());

        return new CircleGeofenceZoneDTOResponse(
                circleGeofenceZone.getId(),
                userDTOMapper.apply(circleGeofenceZone.getUser()),
                "circle",
                circleGeofenceZone.getDescription(),
                circleGeofenceZone.getTitle(),
                convertPointToDTO(circleGeofenceZone.getCenter()),
                circleGeofenceZone.getRadius(),
                vehicles,
                // Propriétés intelligentes
                circleGeofenceZone.getIsTemporalEnabled(),
                circleGeofenceZone.getStartTime() != null ? circleGeofenceZone.getStartTime().toString() : null,
                circleGeofenceZone.getEndTime() != null ? circleGeofenceZone.getEndTime().toString() : null,
                circleGeofenceZone.getActiveDays() != null ? 
                    circleGeofenceZone.getActiveDays().stream().map(Enum::name).toArray(String[]::new) : null,
                circleGeofenceZone.getIsConditionalEnabled(),
                circleGeofenceZone.getMaxSpeed(),
                circleGeofenceZone.getMaxDwellTime(),
                circleGeofenceZone.getMinDwellTime()
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

    public PointDTO convertPointToDTO(Point point) {
        return new PointDTO(List.of(point.getX(), point.getY()));
    }
}