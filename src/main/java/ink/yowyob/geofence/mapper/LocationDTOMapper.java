package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.response.LocationDTO;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.model.Location;
import ink.yowyob.geofence.service.FileStorageService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class LocationDTOMapper implements Function<Location, LocationDTO> {

    private final FileStorageService fileStorageService;

    public LocationDTOMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public LocationDTO apply(Location location) {
        // Convert vehicle to SimpleVehicleDTO
        String imageUrl = location.getVehicle().getImageUrl() != null
                ? fileStorageService.getCompleteImageUrl(location.getVehicle().getImageUrl())
                : null;

        SimpleVehicleDTO vehicleDTO = new SimpleVehicleDTO(
                location.getVehicle().getId(),
                location.getVehicle().getBrand(),
                location.getVehicle().getModel(),
                location.getVehicle().getLicensePlate(),
                imageUrl
        );

        // Convert position to PointDTO
        PointDTO positionDTO = new PointDTO(
                List.of(location.getPosition().getX(), location.getPosition().getY())
        );

        return new LocationDTO(
                location.getId(),
                positionDTO,
                vehicleDTO,
                location.getTimestamp(),
                location.getSpeed(),
                location.getHeading(),
                location.getAltitude(),
                location.getAccuracy(),
                location.getSource()
        );
    }
}