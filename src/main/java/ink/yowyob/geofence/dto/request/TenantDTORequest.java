package ink.yowyob.geofence.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO de requête pour créer ou modifier un tenant/organisation
 */
public record TenantDTORequest(
        @NotBlank(message = "Le nom de l'organisation est requis")
        String name,

        String domain, // Domaine optionnel (ex: easy-rental.com)

        @Email(message = "Email de contact invalide")
        String contactEmail,

        String webhookUrl, // URL de webhook optionnelle

        @NotNull(message = "Le plan d'abonnement est requis")
        String subscriptionPlan, // FREE, BASIC, PREMIUM, ENTERPRISE, UNLIMITED

        @Positive(message = "Le nombre maximum d'utilisateurs doit être positif")
        Integer maxUsers,

        @Positive(message = "Le nombre maximum de véhicules doit être positif")
        Integer maxVehicles,

        @Positive(message = "Le nombre maximum de géofences doit être positif")
        Integer maxGeofences,

        @Positive(message = "Le rate limit doit être positif")
        Integer rateLimitPerHour
) {
}
