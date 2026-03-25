package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.response.AlertDTO;
import ink.yowyob.geofence.dto.response.AlertListResponse;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.Implementation.AlertServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    name = "Alertes",
    description = "API de gestion des alertes - Récupérer et consulter les alertes générées par le système de geofencing"
)
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class AlertController {
    private final AlertServiceImpl alertService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(
        summary = "Récupérer mes alertes",
        description = """
            Récupère la liste des alertes de l'utilisateur connecté avec pagination.

            **Types d'alertes générées :**
            - **ZONE_ENTER** : Un véhicule est entré dans une zone de géofence
            - **ZONE_EXIT** : Un véhicule est sorti d'une zone de géofence
            - **SPEED_LIMIT** : Dépassement de la vitesse maximale autorisée
            - **ROUTE_DEVIATION** : Déviation d'une route assignée
            - **BATTERY_LOW** : Niveau de batterie faible (si applicable)
            - **SYSTEM_ERROR** : Erreur système

            Les alertes sont triées par date décroissante (les plus récentes en premier).
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des alertes récupérée avec succès",
            content = @Content(schema = @Schema(implementation = AlertListResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié - Token JWT manquant ou invalide"
        )
    })
    public Mono<ResponseEntity<AlertListResponse>> getMyAlerts(
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Nombre d'éléments par page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return alertService.getMyAlerts(page, size, user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
        summary = "Récupérer toutes les alertes (Admin)",
        description = "Récupère toutes les alertes du système avec pagination. " +
                      "Accès réservé aux administrateurs et managers pour la supervision globale."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Toutes les alertes récupérées avec succès",
            content = @Content(schema = @Schema(implementation = AlertListResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN ou MANAGER requis"
        )
    })
    public Mono<ResponseEntity<AlertListResponse>> getAllAlerts(
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Nombre d'éléments par page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return alertService.getAllAlerts(page, size, user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{alertId}")
    @Operation(
        summary = "Récupérer une alerte spécifique",
        description = "Récupère les détails complets d'une alerte incluant : " +
                      "le type, le message, l'horodatage, la localisation, le véhicule concerné et la zone de géofence associée."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Alerte trouvée",
            content = @Content(schema = @Schema(implementation = AlertDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Alerte non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - L'alerte doit être associée à l'un de vos véhicules"
        )
    })
    public Mono<ResponseEntity<AlertDTO>> getAlert(
            @Parameter(description = "ID de l'alerte", required = true)
            @PathVariable UUID alertId
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return alertService.getAlert(alertId, user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}
