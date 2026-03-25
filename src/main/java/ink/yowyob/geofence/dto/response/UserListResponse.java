package ink.yowyob.geofence.dto.response;

import java.util.List;

public record UserListResponse(
        List<UserDTO> users,
        int totalItems
) {
}