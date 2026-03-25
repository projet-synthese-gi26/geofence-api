package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import ink.yowyob.geofence.dto.response.VehicleApiKeyDTO;
import ink.yowyob.geofence.model.VehicleApiKey;
import ink.yowyob.geofence.service.FileStorageService;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class VehicleApiKeyDTOMapper implements Function<VehicleApiKey, VehicleApiKeyDTO> {

    private final FileStorageService fileStorageService;

    public VehicleApiKeyDTOMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public VehicleApiKeyDTO apply(VehicleApiKey apiKey) {
        // Convert vehicle to SimpleVehicleDTO
        String imageUrl = apiKey.getVehicle().getImageUrl() != null
                ? fileStorageService.getCompleteImageUrl(apiKey.getVehicle().getImageUrl())
                : null;

        SimpleVehicleDTO vehicleDTO = new SimpleVehicleDTO(
                apiKey.getVehicle().getId(),
                apiKey.getVehicle().getBrand(),
                apiKey.getVehicle().getModel(),
                apiKey.getVehicle().getLicensePlate(),
                imageUrl
        );

        return new VehicleApiKeyDTO(
                apiKey.getId(),
                apiKey.getApiKey(),
                vehicleDTO,
                apiKey.isActive(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getExpiresAt(),
                apiKey.isExpired(),
                apiKey.isValid()
        );
    }
}