package ink.yowyob.geofence.dto.response;

import java.util.List;

public record MultipleVehicleDTOResponse(
        List<VehicleDTOResponse> vehicles
) {
}