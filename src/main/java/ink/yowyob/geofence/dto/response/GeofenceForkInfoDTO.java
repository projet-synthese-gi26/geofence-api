package ink.yowyob.geofence.dto.response;

import java.time.LocalDateTime;

public record GeofenceForkInfoDTO(
        String originalId,
        String originalTitle,
        UserDTO originalOwner,
        LocalDateTime forkedAt,
        String forkReason
) {}