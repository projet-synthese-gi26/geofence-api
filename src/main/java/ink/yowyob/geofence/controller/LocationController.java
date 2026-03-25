package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.CreateApiKeyRequest;
import ink.yowyob.geofence.dto.request.LocationUpdateRequest;
import ink.yowyob.geofence.dto.response.*;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Tag(
    name = "Localisation et Clés API",
    description = "API de gestion du suivi GPS - Mise à jour des positions, historique des déplacements et gestion des clés API pour les appareils mobiles"
)
@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final UserRepository userRepository;

    private Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() ->
                        userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/api/public/location/update")
    @Operation(
        summary = "Mise à jour position (Endpoint Public)",
        description = """
            Met à jour la position d'un véhicule depuis un appareil mobile (smartphone, GPS tracker, etc.).

            **Authentification :** Cet endpoint utilise une clé API au lieu du JWT standard.
            La clé API doit être envoyée dans l'en-tête `X-API-Key`.

            **Format de la clé :** `vk_[64 caractères alphanumériques]`

            **Génération automatique d'alertes :**
            Le système analyse automatiquement la position et génère des alertes si :
            - Le véhicule entre ou sort d'une zone de géofence assignée
            - La vitesse dépasse la limite configurée pour une zone
            - Le véhicule dévie d'une route assignée
            - Les contraintes temporelles ne sont pas respectées

            **Données de position :**
            - **latitude** et **longitude** sont requis (format décimal)
            - **speed**, **heading**, **altitude**, **accuracy** sont optionnels mais recommandés
            - **source** permet d'identifier l'origine de la position (GPS, NETWORK, etc.)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Position mise à jour avec succès, alertes générées si nécessaire",
            content = @Content(schema = @Schema(implementation = LocationUpdateResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Clé API invalide, inactive ou expirée"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Données de position invalides"
        )
    })
    public Mono<ResponseEntity<LocationUpdateResponse>> updateLocationFromDevice(
            @Parameter(description = "Clé API du véhicule (format: vk_...)", required = true)
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody LocationUpdateRequest request
    ) {
        return Mono.fromCallable(() -> locationService.updateLocationFromDevice(apiKey, request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/api/location/vehicle/{vehicleId}/history")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Historique des positions d'un véhicule",
        description = """
            Récupère l'historique complet des positions d'un véhicule avec pagination.

            **Informations retournées pour chaque position :**
            - Coordonnées géographiques (latitude, longitude)
            - Horodatage de la position
            - Vitesse, direction, altitude (si disponibles)
            - Précision et source de la position

            **Pagination :**
            Les positions sont triées par date décroissante (les plus récentes en premier).
            Utilisez les paramètres `page` et `size` pour naviguer dans l'historique.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Historique récupéré avec succès",
            content = @Content(schema = @Schema(implementation = LocationListResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule"
        )
    })
    public Mono<ResponseEntity<LocationListResponse>> getLocationHistory(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId,
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> locationService.getLocationHistory(vehicleId, page, size, user)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/api/location/vehicle/{vehicleId}/latest")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Dernière position connue d'un véhicule",
        description = "Récupère la position la plus récente d'un véhicule. " +
                      "Utile pour afficher la position actuelle sur une carte ou un tableau de bord."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dernière position trouvée",
            content = @Content(schema = @Schema(implementation = LocationDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé ou aucune position enregistrée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule"
        )
    })
    public Mono<ResponseEntity<LocationDTO>> getLatestLocation(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> locationService.getLatestLocation(vehicleId, user)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/api/location/{locationId}")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Supprimer une position",
        description = "Supprime une position spécifique de l'historique. " +
                      "Cette opération est irréversible."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Position supprimée avec succès"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Position non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule"
        )
    })
    public Mono<ResponseEntity<Void>> deleteLocation(
            @Parameter(description = "ID de la position", required = true)
            @PathVariable UUID locationId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    locationService.deleteLocation(locationId, user);
                    return ResponseEntity.noContent().<Void>build();
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/api/location/api-key")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Générer une clé API pour un véhicule",
        description = """
            Génère une nouvelle clé API permettant à un appareil mobile de mettre à jour la position d'un véhicule.

            **Utilisation :**
            1. Générez la clé via cet endpoint
            2. Configurez l'appareil mobile avec cette clé
            3. L'appareil utilise la clé dans l'en-tête `X-API-Key` pour appeler `/api/public/location/update`

            **Format de la clé :** `vk_[64 caractères alphanumériques]`

            **Sécurité :**
            - La clé est liée à un véhicule spécifique
            - Elle peut être révoquée à tout moment
            - Une date d'expiration optionnelle peut être définie
            - Chaque véhicule ne peut avoir qu'une seule clé active à la fois
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Clé API générée avec succès",
            content = @Content(schema = @Schema(implementation = VehicleApiKeyDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "ID du véhicule et date d'expiration optionnelle",
        content = @Content(schema = @Schema(implementation = CreateApiKeyRequest.class))
    )
    public Mono<ResponseEntity<VehicleApiKeyDTO>> generateApiKey(
            @Valid @RequestBody CreateApiKeyRequest request
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> locationService.generateApiKey(request, user)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(201).body(result));
    }

    @GetMapping("/api/location/vehicle/{vehicleId}/api-key")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Récupérer la clé API d'un véhicule",
        description = "Récupère la clé API active associée à un véhicule. " +
                      "Retourne les informations de la clé incluant son état (active/inactive) et sa date d'expiration."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Clé API trouvée",
            content = @Content(schema = @Schema(implementation = VehicleApiKeyDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule ou clé API non trouvé(e)"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule"
        )
    })
    public Mono<ResponseEntity<VehicleApiKeyDTO>> getApiKeyForVehicle(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> locationService.getApiKeyForVehicle(vehicleId, user)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/api/location/vehicle/{vehicleId}/api-key")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Révoquer la clé API d'un véhicule",
        description = """
            Révoque (désactive) la clé API d'un véhicule.

            **Effet :**
            - La clé est immédiatement désactivée
            - Les tentatives de mise à jour de position avec cette clé seront rejetées
            - L'appareil mobile ne pourra plus envoyer de positions
            - Une nouvelle clé peut être générée après révocation

            **Utilisation :** Utile en cas de perte/vol de l'appareil ou pour renouveler une clé compromise.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Clé API révoquée avec succès"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule"
        )
    })
    public Mono<ResponseEntity<Void>> revokeApiKey(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    locationService.revokeApiKey(vehicleId, user);
                    return ResponseEntity.noContent().<Void>build();
                }
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/api/location/my-api-keys")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(
        summary = "Récupérer toutes mes clés API",
        description = "Récupère la liste de toutes les clés API associées aux véhicules de l'utilisateur connecté. " +
                      "Utile pour avoir une vue d'ensemble de toutes les clés actives et leur état."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des clés API récupérée avec succès",
            content = @Content(schema = @Schema(implementation = ApiKeyListResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié - Token JWT manquant ou invalide"
        )
    })
    public Mono<ResponseEntity<ApiKeyListResponse>> getMyApiKeys() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> locationService.getMyApiKeys(user)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}
