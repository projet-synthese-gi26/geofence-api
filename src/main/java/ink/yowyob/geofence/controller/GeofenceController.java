package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.GeofenceZoneDTORequest;
import ink.yowyob.geofence.dto.response.CircleGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.GeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.MultipleGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.PolygonGeofenceZoneDTOResponse;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.Implementation.GeofenceServiceImpl;
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

import java.util.List;
import java.util.UUID;

@Tag(
    name = "Zones de Géofence",
    description = "API de gestion des zones géographiques - Créer et gérer des zones circulaires ou polygonales avec contraintes temporelles et conditionnelles"
)
@RestController
@RequestMapping("/api/geofence")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class GeofenceController {
    private final GeofenceServiceImpl geofenceService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(
        summary = "Récupérer mes zones de géofence",
        description = "Récupère toutes les zones de géofence (cercles et polygones) appartenant à l'utilisateur connecté. " +
                      "Les zones incluent leurs contraintes temporelles et conditionnelles si configurées."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Zones récupérées avec succès",
            content = @Content(schema = @Schema(implementation = MultipleGeofenceZoneDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié - Token JWT manquant ou invalide"
        )
    })
    public Mono<ResponseEntity<MultipleGeofenceZoneDTOResponse>> index() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return geofenceService.getMyGeofenceZones(user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(200).body(result));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
        summary = "Récupérer toutes les zones (Admin)",
        description = "Récupère toutes les zones de géofence du système. " +
                      "Accès réservé aux administrateurs et managers."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Toutes les zones récupérées avec succès",
            content = @Content(schema = @Schema(implementation = MultipleGeofenceZoneDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN ou MANAGER requis"
        )
    })
    public Mono<ResponseEntity<MultipleGeofenceZoneDTOResponse>> admin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return geofenceService.getGeofenceZones(user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(200).body(result));
    }

    @GetMapping("/circles")
    @Operation(
        summary = "Récupérer mes zones circulaires",
        description = "Récupère uniquement les zones circulaires de l'utilisateur connecté. " +
                      "Chaque zone contient un centre (coordonnées) et un rayon en mètres."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Zones circulaires récupérées avec succès",
            content = @Content(schema = @Schema(implementation = List.class))
        )
    })
    public Mono<ResponseEntity<List<CircleGeofenceZoneDTOResponse>>> getCircles() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return geofenceService.getCirclesGeofenceZone(user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(200).body(result));
    }

    @GetMapping("/polygons")
    @Operation(
        summary = "Récupérer mes zones polygonales",
        description = "Récupère uniquement les zones polygonales de l'utilisateur connecté. " +
                      "Chaque zone contient une géométrie définie par une liste de coordonnées."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Zones polygonales récupérées avec succès",
            content = @Content(schema = @Schema(implementation = List.class))
        )
    })
    public Mono<ResponseEntity<List<PolygonGeofenceZoneDTOResponse>>> getPolygons() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return geofenceService.getPolygonsGeofenceZone(user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(200).body(result));
    }

    @PostMapping
    @Operation(
        summary = "Créer une zone de géofence",
        description = """
            Créer une nouvelle zone de géofence circulaire ou polygonale avec support des fonctionnalités intelligentes :

            **Types de zones :**
            - **circle** : Zone circulaire définie par un centre et un rayon
            - **polygon** : Zone polygonale définie par une liste de coordonnées

            **Contraintes temporelles (optionnel) :**
            - Définir des plages horaires (startTime, endTime)
            - Spécifier les jours actifs (MONDAY, TUESDAY, etc.)
            - La zone ne génère des alertes que pendant ces périodes

            **Contraintes conditionnelles (optionnel) :**
            - **maxSpeed** : Vitesse maximale autorisée dans la zone (km/h)
            - **maxDwellTime** : Temps de séjour maximum autorisé (minutes)
            - **minDwellTime** : Temps de séjour minimum requis (minutes)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Zone créée avec succès",
            content = @Content(schema = @Schema(implementation = GeofenceZoneDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Données invalides - Vérifiez le type et les coordonnées"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Données de la zone à créer avec contraintes optionnelles",
        content = @Content(schema = @Schema(implementation = GeofenceZoneDTORequest.class))
    )
    public Mono<ResponseEntity<GeofenceZoneDTOResponse>> create(
            @RequestBody GeofenceZoneDTORequest geofenceZoneDTORequest
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                            var user = userRepository.findByUsername(username)
                                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                            return geofenceService.createGeofenceZone(geofenceZoneDTORequest,user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(201).body(result));
    }

    @GetMapping(path="{type}/{zoneId}")
    @Operation(
        summary = "Récupérer une zone de géofence",
        description = "Récupère les détails complets d'une zone de géofence incluant toutes ses contraintes et véhicules associés."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Zone trouvée",
            content = @Content(schema = @Schema(implementation = GeofenceZoneDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Zone non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire de la zone"
        )
    })
    public Mono<ResponseEntity<GeofenceZoneDTOResponse>> getZone(
            @Parameter(description = "ID de la zone", required = true)
            @PathVariable UUID zoneId,
            @Parameter(description = "Type de zone : 'c' pour cercle, 'p' pour polygone", required = true)
            @PathVariable String type
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return geofenceService.getGeofenceZone(zoneId, type, user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(200).body(result));
    }

    @PutMapping(path="{type}/{zoneId}")
    @Operation(
        summary = "Modifier une zone de géofence",
        description = """
            Modifie une zone de géofence existante avec toutes ses propriétés :

            - Modifier la géométrie (centre/rayon pour cercles, coordonnées pour polygones)
            - Mettre à jour le titre et la description
            - Activer/désactiver les contraintes temporelles
            - Modifier les plages horaires et jours actifs
            - Activer/désactiver les contraintes conditionnelles
            - Ajuster les limites de vitesse et temps de séjour
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Zone modifiée avec succès",
            content = @Content(schema = @Schema(implementation = GeofenceZoneDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Zone non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire de la zone"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Nouvelles données de la zone",
        content = @Content(schema = @Schema(implementation = GeofenceZoneDTORequest.class))
    )
    public Mono<ResponseEntity<GeofenceZoneDTOResponse>> editZone(
            @Parameter(description = "ID de la zone", required = true)
            @PathVariable UUID zoneId,
            @Parameter(description = "Type de zone : 'c' pour cercle, 'p' pour polygone", required = true)
            @PathVariable String type,
            @RequestBody GeofenceZoneDTORequest geofenceZoneDTORequest
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                            var user = userRepository.findByUsername(username)
                                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                            return geofenceService.editGeofenceZone(geofenceZoneDTORequest, zoneId, type,user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> ResponseEntity.status(200).body(result));
    }


    @DeleteMapping(path="{type}/{zoneId}")
    @Operation(
        summary = "Supprimer une zone de géofence",
        description = "Supprime une zone de géofence et toutes ses associations avec les véhicules. " +
                      "Cette opération est irréversible."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Zone supprimée avec succès"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Zone non trouvée"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire de la zone"
        )
    })
    public Mono<ResponseEntity<Void>> deleteZone(
            @Parameter(description = "ID de la zone", required = true)
            @PathVariable UUID zoneId,
            @Parameter(description = "Type de zone : 'c' pour cercle, 'p' pour polygone", required = true)
            @PathVariable String type
    ) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    geofenceService.deleteGeofenceZone(zoneId, type, user);
                    return ResponseEntity.noContent().build();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
