package ink.yowyob.geofence.dto.response;

public record GeofenceWithForkInfoDTO(
        String id,
        String title,
        String description,
        String type,
        UserDTO owner,
        boolean isOriginal,
        boolean isFork,
        GeofenceForkInfoDTO forkInfo,
        long forkCount
) {}