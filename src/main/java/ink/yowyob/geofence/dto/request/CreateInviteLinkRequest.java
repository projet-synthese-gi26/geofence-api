package ink.yowyob.geofence.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateInviteLinkRequest(
        @NotNull(message = "L'ID de la géofence est requis")
        UUID geofenceId,

        @NotBlank(message = "Le type de géofence est requis")
        String geofenceType,

        LocalDateTime expiresAt, // null = infini

        boolean canEdit,

        @Min(value = -1, message = "Le nombre maximum d'utilisations doit être -1 (illimité) ou positif")
        int maxUses
) {}