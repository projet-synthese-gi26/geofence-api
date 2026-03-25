package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.VehicleDTORequest;
import ink.yowyob.geofence.dto.response.MultipleVehicleDTOResponse;
import ink.yowyob.geofence.dto.response.VehicleDTOResponse;
import ink.yowyob.geofence.model.User;

import java.util.UUID;

public interface VehicleService {
    VehicleDTOResponse getVehicle(UUID id, User currentUser);
    MultipleVehicleDTOResponse getVehicles(User currentUser);
    VehicleDTOResponse createVehicle(VehicleDTORequest vehicleDTORequest, User currentUser);
    MultipleVehicleDTOResponse getMyVehicles(User currentUser);
    VehicleDTOResponse editVehicle(VehicleDTORequest vehicleDTORequest, UUID id, User currentUser);
    void deleteVehicle(UUID id, User currentUser);

    VehicleDTOResponse updateVehicleImage(UUID id, String imageUrl, User currentUser);

    // Pour la gestion des zones de geofence
    VehicleDTOResponse assignToGeofenceZone(UUID vehicleId, UUID zoneId, String type, User currentUser);
    VehicleDTOResponse removeFromGeofenceZone(UUID vehicleId, UUID zoneId, User currentUser);
}
