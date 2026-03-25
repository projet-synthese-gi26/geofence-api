package ink.yowyob.geofence.dto.response;

import java.util.List;

public record AlertListResponse(
        List<AlertDTO> alerts,
        int totalItems
) {
}