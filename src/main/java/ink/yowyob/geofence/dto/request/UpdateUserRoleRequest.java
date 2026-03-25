package ink.yowyob.geofence.dto.request;

import ink.yowyob.geofence.Enum.UserRole;

public record UpdateUserRoleRequest(
        UserRole role
) {
}