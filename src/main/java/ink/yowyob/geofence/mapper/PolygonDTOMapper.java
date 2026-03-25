package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.PolygonDTO;
import ink.yowyob.geofence.dto.response.PolygonGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.model.PolygonGeofenceZone;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.FileStorageService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PolygonDTOMapper implements Function<PolygonGeofenceZone, PolygonGeofenceZoneDTOResponse> {
    private final UserDTOMapper userDTOMapper;
    private final VehicleRepository vehicleRepository;
    private final FileStorageService fileStorageService;

    public PolygonDTOMapper(UserDTOMapper userDTOMapper, VehicleRepository vehicleRepository, FileStorageService fileStorageService) {
        this.userDTOMapper = userDTOMapper;
        this.vehicleRepository = vehicleRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public PolygonGeofenceZoneDTOResponse apply(PolygonGeofenceZone polygonGeofenceZone) {
        // Récupérer les véhicules associés à cette zone
        List<SimpleVehicleDTO> vehicles = vehicleRepository.findByGeofenceZonesContaining(polygonGeofenceZone)
                .stream()
                .map(this::mapToSimpleVehicleDTO)
                .collect(Collectors.toList());

        return new PolygonGeofenceZoneDTOResponse(
                polygonGeofenceZone.getId(),
                userDTOMapper.apply(polygonGeofenceZone.getUser()),
                polygonGeofenceZone.getDescription(),
                polygonGeofenceZone.getTitle(),
                "polygon",
                convertToPolygonDTO(polygonGeofenceZone.getPolygon()),
                vehicles,
                // Propriétés intelligentes
                polygonGeofenceZone.getIsTemporalEnabled(),
                polygonGeofenceZone.getStartTime() != null ? polygonGeofenceZone.getStartTime().toString() : null,
                polygonGeofenceZone.getEndTime() != null ? polygonGeofenceZone.getEndTime().toString() : null,
                polygonGeofenceZone.getActiveDays() != null ? 
                    polygonGeofenceZone.getActiveDays().stream().map(Enum::name).toArray(String[]::new) : null,
                polygonGeofenceZone.getIsConditionalEnabled(),
                polygonGeofenceZone.getMaxSpeed(),
                polygonGeofenceZone.getMaxDwellTime(),
                polygonGeofenceZone.getMinDwellTime()
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

    private PolygonDTO convertToPolygonDTO(Polygon jtsPolygon) {
        List<List<List<Double>>> coordinates = new ArrayList<>();

        // Convert exterior ring
        List<List<Double>> exteriorRing = new ArrayList<>();
        for (Coordinate coord : jtsPolygon.getExteriorRing().getCoordinates()) {
            exteriorRing.add(Arrays.asList(coord.x, coord.y));
        }

        List<List<Double>> ringCoordinates = new ArrayList<>(exteriorRing);
        coordinates.add(ringCoordinates);

        // Add interior rings if present
        for (int i = 0; i < jtsPolygon.getNumInteriorRing(); i++) {
            List<List<Double>> interiorRing = new ArrayList<>();
            for (Coordinate coord : jtsPolygon.getInteriorRingN(i).getCoordinates()) {
                interiorRing.add(Arrays.asList(coord.x, coord.y));
            }
            coordinates.add(interiorRing);
        }

        return new PolygonDTO(coordinates);
    }
}