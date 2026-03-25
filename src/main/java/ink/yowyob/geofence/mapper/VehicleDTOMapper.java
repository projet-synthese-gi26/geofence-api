package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.response.GeofenceZoneSimpleDTO;
import ink.yowyob.geofence.dto.response.UserDTO;
import ink.yowyob.geofence.dto.response.VehicleDTOResponse;
import ink.yowyob.geofence.model.CircleGeofenceZone;
import ink.yowyob.geofence.model.PolygonGeofenceZone;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.service.FileStorageService;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VehicleDTOMapper implements Function<Vehicle, VehicleDTOResponse> {

    private final FileStorageService fileStorageService;

    public VehicleDTOMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public VehicleDTOResponse apply(Vehicle vehicle) {
        String imageUrl = vehicle.getImageUrl() != null
                ? fileStorageService.getCompleteImageUrl(vehicle.getImageUrl())
                : null;
        return new VehicleDTOResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                vehicle.getDescription(),
                imageUrl,
                new UserDTO(
                        vehicle.getUser().getUuid(),
                        vehicle.getUser().getUsername(),
                        vehicle.getUser().getFirstname(),
                        vehicle.getUser().getLastname(),
                        vehicle.getUser().getPhoneNumber(),
                        vehicle.getUser().getEmail(),
                        vehicle.getUser().getDOB(),
                        vehicle.getUser().getRole().getName()
                ),
                vehicle.getGeofenceZones().stream()
                        .map(zone -> new GeofenceZoneSimpleDTO(
                                zone.getId(),
                                zone.getTitle(),
                                zone instanceof CircleGeofenceZone ? "circle" :
                                        (zone instanceof PolygonGeofenceZone ? "polygon" : "unknown")
                        ))
                        .collect(Collectors.toSet())
        );
    }
}