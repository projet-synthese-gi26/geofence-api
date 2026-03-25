package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.request.RouteDTORequest;
import ink.yowyob.geofence.dto.response.MultipleRoutesDTOResponse;
import ink.yowyob.geofence.dto.response.RouteDTOResponse;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.service.RouteService;
import ink.yowyob.geofence.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
@AllArgsConstructor
@Slf4j
@Tag(name = "Routes", description = "Gestion des routes et trajets autorisés pour les véhicules")
@SecurityRequirement(name = "bearer-jwt")
public class RouteController {

    private final RouteService routeService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(
        summary = "Créer une nouvelle route",
        description = "Crée une route avec des points de passage (waypoints) et une tolérance de déviation. " +
                      "La route peut être assignée à des véhicules pour surveiller leurs déplacements."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Route créée avec succès",
            content = @Content(schema = @Schema(implementation = RouteDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Données invalides (waypoints manquants, tolérance négative, etc.)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié - Token JWT manquant ou invalide"
        )
    })
    public Mono<ResponseEntity<RouteDTOResponse>> createRoute(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Informations de la route à créer",
                required = true,
                content = @Content(schema = @Schema(implementation = RouteDTORequest.class))
            )
            @RequestBody RouteDTORequest routeRequest) {
        log.info("Requête de création de route reçue: nom={}, segments={}",
            routeRequest.name(),
            routeRequest.authorizedSegments() != null ? routeRequest.authorizedSegments().size() : 0);

        return getCurrentUser()
            .flatMap(user -> {
                log.info("Utilisateur authentifié: {}", user.getUsername());
                return routeService.createRoute(routeRequest, user);
            })
            .map(route -> ResponseEntity.status(HttpStatus.CREATED).body(route))
            .onErrorResume(ex -> {
                log.error("Erreur lors de la création de la route: ", ex);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }

    @GetMapping
    @Operation(
        summary = "Récupérer mes routes",
        description = "Retourne toutes les routes créées par l'utilisateur connecté"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des routes récupérée avec succès",
            content = @Content(schema = @Schema(implementation = MultipleRoutesDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié"
        )
    })
    public Mono<ResponseEntity<MultipleRoutesDTOResponse>> getUserRoutes() {
        return getCurrentUser()
            .flatMap(routeService::getUserRoutes)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/all")
    @Operation(
        summary = "Récupérer toutes les routes (Admin)",
        description = "Retourne toutes les routes du système. Réservé aux administrateurs."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste de toutes les routes",
            content = @Content(schema = @Schema(implementation = MultipleRoutesDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Rôle ADMIN requis"
        )
    })
    public Mono<ResponseEntity<MultipleRoutesDTOResponse>> getAllRoutes() {
        return getCurrentUser()
            .flatMap(routeService::getAllRoutes)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/{routeId}")
    @Operation(
        summary = "Récupérer une route par ID",
        description = "Retourne les détails d'une route spécifique"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route trouvée",
            content = @Content(schema = @Schema(implementation = RouteDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous n'êtes pas propriétaire de cette route"
        )
    })
    public Mono<ResponseEntity<RouteDTOResponse>> getRoute(
            @Parameter(description = "ID de la route", required = true)
            @PathVariable UUID routeId) {
        return getCurrentUser()
            .flatMap(user -> routeService.getRoute(routeId, user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PutMapping("/{routeId}")
    @Operation(
        summary = "Modifier une route",
        description = "Met à jour les informations d'une route existante"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route modifiée avec succès",
            content = @Content(schema = @Schema(implementation = RouteDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé"
        )
    })
    public Mono<ResponseEntity<RouteDTOResponse>> updateRoute(
            @Parameter(description = "ID de la route à modifier", required = true)
            @PathVariable UUID routeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Nouvelles informations de la route",
                required = true
            )
            @RequestBody RouteDTORequest routeRequest) {
        return getCurrentUser()
            .flatMap(user -> routeService.updateRoute(routeId, routeRequest, user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @DeleteMapping("/{routeId}")
    @Operation(
        summary = "Supprimer une route",
        description = "Supprime définitivement une route"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Route supprimée avec succès"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé"
        )
    })
    public Mono<ResponseEntity<Void>> deleteRoute(
            @Parameter(description = "ID de la route à supprimer", required = true)
            @PathVariable UUID routeId) {
        return getCurrentUser()
            .flatMap(user -> routeService.deleteRoute(routeId, user))
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/search")
    @Operation(
        summary = "Rechercher des routes",
        description = "Recherche des routes par mot-clé dans le nom ou la description"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Résultats de la recherche",
            content = @Content(schema = @Schema(implementation = MultipleRoutesDTOResponse.class))
        )
    })
    public Mono<ResponseEntity<MultipleRoutesDTOResponse>> searchRoutes(
            @Parameter(description = "Mot-clé de recherche", required = true, example = "Paris")
            @RequestParam String keyword) {
        return getCurrentUser()
            .flatMap(user -> routeService.searchRoutes(keyword, user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(
        summary = "Routes actives d'un véhicule",
        description = "Retourne toutes les routes assignées à un véhicule spécifique"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des routes du véhicule",
            content = @Content(schema = @Schema(implementation = RouteDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé"
        )
    })
    public Flux<RouteDTOResponse> getActiveRoutesByVehicle(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId) {
        return getCurrentUser()
            .flatMapMany(user -> routeService.getActiveRoutesByVehicle(vehicleId, user));
    }

    @PostMapping("/{routeId}/assign-vehicle/{vehicleId}")
    @Operation(
        summary = "Assigner un véhicule à une route",
        description = "Associe un véhicule à une route pour surveiller ses déplacements. " +
                      "Le système générera des alertes si le véhicule dévie de cette route."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Véhicule assigné avec succès",
            content = @Content(schema = @Schema(implementation = RouteDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route ou véhicule non trouvé"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire de la route et du véhicule"
        )
    })
    public Mono<ResponseEntity<RouteDTOResponse>> assignVehicleToRoute(
            @Parameter(description = "ID de la route", required = true)
            @PathVariable UUID routeId,
            @Parameter(description = "ID du véhicule à assigner", required = true)
            @PathVariable UUID vehicleId) {
        return getCurrentUser()
            .flatMap(user -> routeService.assignVehicleToRoute(routeId, vehicleId, user))
            .map(ResponseEntity::ok)
            .onErrorResume(ex -> {
                log.error("Erreur lors de l'assignation du véhicule {} à la route {}: ", vehicleId, routeId, ex);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }

    @DeleteMapping("/{routeId}/remove-vehicle/{vehicleId}")
    @Operation(
        summary = "Retirer un véhicule d'une route",
        description = "Désassocie un véhicule d'une route"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Véhicule retiré avec succès",
            content = @Content(schema = @Schema(implementation = RouteDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route ou véhicule non trouvé"
        )
    })
    public Mono<ResponseEntity<RouteDTOResponse>> removeVehicleFromRoute(
            @Parameter(description = "ID de la route", required = true)
            @PathVariable UUID routeId,
            @Parameter(description = "ID du véhicule à retirer", required = true)
            @PathVariable UUID vehicleId) {
        return getCurrentUser()
            .flatMap(user -> routeService.removeVehicleFromRoute(routeId, vehicleId, user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PostMapping("/vehicle/{vehicleId}/check-position")
    @Operation(
        summary = "Vérifier si un véhicule est sur sa route",
        description = "Vérifie si la position actuelle du véhicule correspond à sa route assignée"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vérification effectuée - true si sur la route, false sinon"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé"
        )
    })
    public Mono<ResponseEntity<Boolean>> checkVehiclePosition(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Position actuelle du véhicule [longitude, latitude]",
                required = true
            )
            @RequestBody PointDTO currentPosition) {
        return getCurrentUser()
            .flatMap(user -> routeService.isVehicleOnRoute(vehicleId, routeService.convertToPoint(currentPosition), user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false));
    }

    @PostMapping("/{routeId}/deviation-distance")
    @Operation(
        summary = "Calculer la distance de déviation",
        description = "Calcule la distance (en mètres) entre une position et la route la plus proche"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Distance calculée en mètres"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route non trouvée"
        )
    })
    public Mono<ResponseEntity<Double>> calculateDeviationDistance(
            @Parameter(description = "ID de la route", required = true)
            @PathVariable UUID routeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Position actuelle [longitude, latitude]",
                required = true
            )
            @RequestBody PointDTO currentPosition) {
        return getCurrentUser()
            .flatMap(user -> routeService.calculateDeviationDistance(routeId, routeService.convertToPoint(currentPosition), user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Double.MAX_VALUE));
    }

    @PostMapping("/vehicle/{vehicleId}/detect-deviations")
    @Operation(
        summary = "Détecter les déviations de route",
        description = "Détecte si un véhicule dévie de ses routes assignées au-delà de la tolérance définie"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des IDs de routes où une déviation est détectée"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé"
        )
    })
    public Flux<UUID> detectRouteDeviations(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Position actuelle [longitude, latitude]",
                required = true
            )
            @RequestBody PointDTO currentPosition,
            @Parameter(description = "Tolérance de déviation en mètres", example = "100.0")
            @RequestParam(defaultValue = "100.0") Double toleranceMeters) {
        return getCurrentUser()
            .flatMapMany(user -> routeService.detectRouteDeviations(vehicleId, routeService.convertToPoint(currentPosition), toleranceMeters, user));
    }

    @PostMapping("/{routeId}/progress")
    @Operation(
        summary = "Calculer le progrès sur la route",
        description = "Calcule le pourcentage de progression d'un véhicule sur une route (0.0 à 1.0)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pourcentage de progression (ex: 0.75 = 75%)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route non trouvée"
        )
    })
    public Mono<ResponseEntity<Double>> calculateRouteProgress(
            @Parameter(description = "ID de la route", required = true)
            @PathVariable UUID routeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Position actuelle [longitude, latitude]",
                required = true
            )
            @RequestBody PointDTO currentPosition) {
        return getCurrentUser()
            .flatMap(user -> routeService.calculateRouteProgress(routeId, routeService.convertToPoint(currentPosition), user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(0.0));
    }

    @PostMapping("/suggest-alternatives")
    @Operation(
        summary = "Suggérer des routes alternatives",
        description = "Suggère des routes alternatives entre deux points basées sur les routes existantes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste de routes alternatives suggérées"
        )
    })
    public Mono<ResponseEntity<List<RouteDTOResponse>>> suggestAlternativeRoutes(
            @Parameter(description = "Longitude du point de départ", required = true, example = "2.3522")
            @RequestParam Double startLng,
            @Parameter(description = "Latitude du point de départ", required = true, example = "48.8566")
            @RequestParam Double startLat,
            @Parameter(description = "Longitude du point d'arrivée", required = true, example = "2.2945")
            @RequestParam Double endLng,
            @Parameter(description = "Latitude du point d'arrivée", required = true, example = "48.8584")
            @RequestParam Double endLat) {

        PointDTO startPoint = new PointDTO(List.of(startLng, startLat));
        PointDTO endPoint = new PointDTO(List.of(endLng, endLat));

        return getCurrentUser()
            .flatMap(user -> routeService.suggestAlternativeRoutes(
                routeService.convertToPoint(startPoint),
                routeService.convertToPoint(endPoint),
                user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of()));
    }

    @PostMapping("/{routeId}/optimize")
    @Operation(
        summary = "Optimiser les segments d'une route",
        description = "Optimise automatiquement les segments d'une route pour réduire la distance totale"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route optimisée avec succès",
            content = @Content(schema = @Schema(implementation = RouteDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Route non trouvée"
        )
    })
    public Mono<ResponseEntity<RouteDTOResponse>> optimizeRouteSegments(
            @Parameter(description = "ID de la route à optimiser", required = true)
            @PathVariable UUID routeId) {
        return getCurrentUser()
            .flatMap(user -> routeService.optimizeRouteSegments(routeId, user))
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Méthode utilitaire pour obtenir l'utilisateur actuel
     */
    private Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .cast(SecurityContext.class)
            .map(SecurityContext::getAuthentication)
            .cast(Authentication.class)
            .map(Authentication::getName)
            .flatMap(username -> Mono.fromCallable(() ->
                userRepository.findByUsernameWithRole(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
            ));
    }
}
