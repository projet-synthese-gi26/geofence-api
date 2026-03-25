package ink.yowyob.geofence.dto.response;

import java.util.List;

public record GeofenceForkListResponse(
        List<GeofenceForkDTO> forks,
        int totalItems
) {}