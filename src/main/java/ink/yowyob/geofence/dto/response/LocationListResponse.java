package ink.yowyob.geofence.dto.response;

import java.util.List;

public record LocationListResponse(
        List<LocationDTO> locations,
        int totalItems,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}