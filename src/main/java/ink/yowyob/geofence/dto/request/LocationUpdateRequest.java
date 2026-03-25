package ink.yowyob.geofence.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(
        @NotNull(message = "La latitude est requise")
        @DecimalMin(value = "-90.0", message = "La latitude doit être >= -90")
        @DecimalMax(value = "90.0", message = "La latitude doit être <= 90")
        Double latitude,

        @NotNull(message = "La longitude est requise")
        @DecimalMin(value = "-180.0", message = "La longitude doit être >= -180")
        @DecimalMax(value = "180.0", message = "La longitude doit être <= 180")
        Double longitude,

        Double speed, // Vitesse en km/h (optionnelle)
        Double heading, // Direction en degrés (0-360, optionnelle)
        Double altitude, // Altitude en mètres (optionnelle)
        Double accuracy, // Précision en mètres (optionnelle)
        String source // Source de la position (optionnelle)
) {}