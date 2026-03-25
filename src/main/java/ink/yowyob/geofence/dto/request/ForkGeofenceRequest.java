package ink.yowyob.geofence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ForkGeofenceRequest(
        @NotNull(message = "L'ID de la géofence est requis")
        UUID geofenceId,

        @NotBlank(message = "Le type de géofence est requis")
        String geofenceType,

        @NotBlank(message = "Le nouveau titre est requis")
        @Size(min = 1, max = 100, message = "Le titre doit contenir entre 1 et 100 caractères")
        String newTitle,

        @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
        String newDescription,

        @Size(max = 500, message = "La raison du fork ne peut pas dépasser 500 caractères")
        String forkReason
) {}
