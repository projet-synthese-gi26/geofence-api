package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.response.AlertDTO;
import ink.yowyob.geofence.dto.response.GeofenceZoneSimpleDTO;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.model.Alert;
import ink.yowyob.geofence.model.CircleGeofenceZone;
import ink.yowyob.geofence.model.Location;
import ink.yowyob.geofence.model.PolygonGeofenceZone;
import ink.yowyob.geofence.service.FileStorageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class AlertDTOMapper implements Function<Alert, AlertDTO> {

    private final FileStorageService fileStorageService;

    public AlertDTOMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public AlertDTO apply(Alert alert) {
        // Convert vehicle to SimpleVehicleDTO
        SimpleVehicleDTO vehicleDTO = null;
        if (alert.getVehicle() != null) {
            String imageUrl = alert.getVehicle().getImageUrl() != null
                    ? fileStorageService.getCompleteImageUrl(alert.getVehicle().getImageUrl())
                    : null;

            vehicleDTO = new SimpleVehicleDTO(
                    alert.getVehicle().getId(),
                    alert.getVehicle().getBrand(),
                    alert.getVehicle().getModel(),
                    alert.getVehicle().getLicensePlate(),
                    imageUrl
            );
        }

        // Convert geofence zone to GeofenceZoneSimpleDTO
        GeofenceZoneSimpleDTO zoneDTO = null;
        if (alert.getGeofenceZone() != null) {
            String type = "unknown";
            if (alert.getGeofenceZone() instanceof CircleGeofenceZone) {
                type = "circle";
            } else if (alert.getGeofenceZone() instanceof PolygonGeofenceZone) {
                type = "polygon";
            }

            zoneDTO = new GeofenceZoneSimpleDTO(
                    alert.getGeofenceZone().getId(),
                    alert.getGeofenceZone().getTitle(),
                    type
            );
        }

        // Convert location to PointDTO
        PointDTO locationDTO = null;
        if (alert.getLocation() != null && alert.getLocation().getPosition() != null) {
            Location location = alert.getLocation();
            List<Double> coordinates = new ArrayList<>();
            coordinates.add(location.getPosition().getX());
            coordinates.add(location.getPosition().getY());
            locationDTO = new PointDTO(coordinates);
        }

        return new AlertDTO(
                alert.getId(),
                alert.getType().getType(),
                alert.getTimestamp(),
                alert.getMessage(),
                locationDTO,
                vehicleDTO,
                zoneDTO
        );
    }
}