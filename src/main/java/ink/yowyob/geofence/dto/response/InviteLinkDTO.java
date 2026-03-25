package ink.yowyob.geofence.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record InviteLinkDTO(
        UUID id,
        String inviteCode,
        UUID geofenceId,
        String geofenceType,
        UserDTO createdBy,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean canEdit,
        boolean isActive,
        int maxUses,
        int currentUses,
        boolean isExpired,
        boolean canBeUsed
) {}