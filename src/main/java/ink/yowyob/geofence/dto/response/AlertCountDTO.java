package ink.yowyob.geofence.dto.response;

import ink.yowyob.geofence.Enum.AlertTypeEnum;

public record AlertCountDTO(
        AlertTypeEnum type,
        long count
) {
}