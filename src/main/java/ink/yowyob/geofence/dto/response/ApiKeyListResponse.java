package ink.yowyob.geofence.dto.response;

import java.util.List;

public record ApiKeyListResponse(
        List<VehicleApiKeyDTO> apiKeys,
        int totalItems
) {}