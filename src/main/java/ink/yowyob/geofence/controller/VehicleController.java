package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.VehicleDTORequest;
import ink.yowyob.geofence.dto.response.MultipleVehicleDTOResponse;
import ink.yowyob.geofence.dto.response.VehicleDTOResponse;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.FileStorageService;
import ink.yowyob.geofence.service.Implementation.VehicleServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.UUID;

@Tag(
    name = "Véhicules",
    description = "API de gestion des véhicules - Créer, modifier, supprimer des véhicules et les associer aux zones de géofence"
)
@Slf4j
@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class VehicleController {
    private final VehicleServiceImpl vehicleService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    private Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    log.info(user.getUsername());
                    return user;
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    @Operation(
        summary = "Récupérer mes véhicules",
        description = "Récupère la liste des véhicules appartenant à l'utilisateur connecté. " +
                      "Chaque véhicule inclut ses informations de base et les zones de géofence qui lui sont associées."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des véhicules récupérée avec succès",
            content = @Content(schema = @Schema(implementation = MultipleVehicleDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié - Token JWT manquant ou invalide"
        )
    })
    public Mono<ResponseEntity<MultipleVehicleDTOResponse>> index() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> vehicleService.getMyVehicles(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
        summary = "Récupérer tous les véhicules (Admin)",
        description = "Récupère la liste de tous les véhicules du système. " +
                      "Accès réservé aux administrateurs et managers."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste de tous les véhicules récupérée avec succès",
            content = @Content(schema = @Schema(implementation = MultipleVehicleDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN ou MANAGER requis"
        )
    })
    public Mono<ResponseEntity<MultipleVehicleDTOResponse>> admin() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> vehicleService.getVehicles(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Créer un véhicule",
        description = "Créer un nouveau véhicule avec possibilité d'ajouter une image et de l'associer à des zones de géofence. " +
                      "Les données du véhicule sont envoyées en JSON et l'image (optionnelle) en multipart/form-data."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Véhicule créé avec succès",
            content = @Content(schema = @Schema(implementation = VehicleDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Données invalides - Vérifiez les champs requis (brand, model, licensePlate)"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Données du véhicule et image optionnelle",
        content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = VehicleDTORequest.class)
        )
    )
    public Mono<ResponseEntity<VehicleDTOResponse>> create(
            @RequestPart("vehicle") VehicleDTORequest vehicleDTORequest,
            @RequestPart(value = "image", required = false) Mono<FilePart> image
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> vehicleService.createVehicle(vehicleDTORequest, user))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(response -> {
                            return image
                                    .flatMap(img -> fileStorageService.storeVehicleImageReactive(img, response.id())
                                            .map(imageUrl -> vehicleService.updateVehicleImage(response.id(), imageUrl, user)))
                                    .defaultIfEmpty(response);
                        }))
                .map(response -> ResponseEntity.status(201).body(response));
    }

    @GetMapping("/{vehicleId}")
    @Operation(
        summary = "Récupérer un véhicule",
        description = "Récupère les détails complets d'un véhicule spécifique incluant ses zones de géofence associées."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Véhicule trouvé",
            content = @Content(schema = @Schema(implementation = VehicleDTOResponse.class))
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
    public Mono<ResponseEntity<VehicleDTOResponse>> getVehicle(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> vehicleService.getVehicle(vehicleId, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(response -> ResponseEntity.status(200).body(response));
    }

    @PutMapping(value = "/{vehicleId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Modifier un véhicule",
        description = "Modifie les informations d'un véhicule existant. " +
                      "Permet de mettre à jour les données de base, l'image et les associations aux zones de géofence."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Véhicule modifié avec succès",
            content = @Content(schema = @Schema(implementation = VehicleDTOResponse.class))
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
        description = "Nouvelles données du véhicule et image optionnelle",
        content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = VehicleDTORequest.class)
        )
    )
    public Mono<ResponseEntity<VehicleDTOResponse>> editVehicle(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId,
            @RequestPart("vehicle") VehicleDTORequest vehicleDTORequest,
            @RequestPart(value = "image", required = false) FilePart image
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> vehicleService.editVehicle(vehicleDTORequest, vehicleId, user))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(response -> {
                            if (image != null) {
                                return Mono.fromCallable(() -> {
                                            try {
                                                String imageUrl = fileStorageService.storeVehicleImageFromFilePart(image, vehicleId);
                                                return vehicleService.updateVehicleImage(vehicleId, imageUrl, user);
                                            } catch (IOException e) {
                                                return response;
                                            }
                                        })
                                        .subscribeOn(Schedulers.boundedElastic());
                            }
                            return Mono.just(response);
                        }))
                .map(response -> ResponseEntity.status(200).body(response));
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(
        summary = "Supprimer un véhicule",
        description = "Supprime un véhicule et son image associée. " +
                      "Cette opération est irréversible et supprime aussi toutes les associations aux zones de géofence."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Véhicule supprimé avec succès"
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
    public Mono<ResponseEntity<Void>> deleteVehicle(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    VehicleDTOResponse vehicle = vehicleService.getVehicle(vehicleId, user);

                    if (vehicle.imageUrl() != null) {
                        fileStorageService.deleteVehicleImage(vehicle.imageUrl());
                    }

                    vehicleService.deleteVehicle(vehicleId, user);
                    return ResponseEntity.status(204).<Void>build();
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{vehicleId}/image")
    @Operation(
        summary = "Mettre à jour l'image du véhicule",
        description = "Met à jour uniquement l'image d'un véhicule existant sans modifier les autres données."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Image mise à jour avec succès",
            content = @Content(schema = @Schema(implementation = VehicleDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule non trouvé"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erreur lors du stockage de l'image"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Nouvelle image du véhicule",
        content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
    )
    public Mono<ResponseEntity<VehicleDTOResponse>> updateVehicleImage(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId,
            @RequestPart("image") FilePart image
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                            try {
                                String imageUrl = fileStorageService.storeVehicleImageFromFilePart(image, vehicleId);
                                return vehicleService.updateVehicleImage(vehicleId, imageUrl, user);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to store image", e);
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @PostMapping("/{vehicleId}/geofence/{type}/{zoneId}")
    @Operation(
        summary = "Assigner un véhicule à une zone de géofence",
        description = "Associe un véhicule à une zone de géofence pour surveiller ses déplacements. " +
                      "Le système générera des alertes lorsque le véhicule entre ou sort de cette zone."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Véhicule assigné à la zone avec succès",
            content = @Content(schema = @Schema(implementation = VehicleDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule ou zone non trouvé(e)"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule et de la zone"
        )
    })
    public Mono<ResponseEntity<VehicleDTOResponse>> assignToGeofenceZone(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId,
            @Parameter(description = "Type de zone : 'c' pour cercle, 'p' pour polygone", required = true)
            @PathVariable String type,
            @Parameter(description = "ID de la zone de géofence", required = true)
            @PathVariable UUID zoneId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> vehicleService.assignToGeofenceZone(vehicleId, zoneId, type, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(response -> ResponseEntity.status(200).body(response));
    }

    @DeleteMapping("/{vehicleId}/geofence/{zoneId}")
    @Operation(
        summary = "Retirer un véhicule d'une zone de géofence",
        description = "Supprime l'association entre un véhicule et une zone de géofence. " +
                      "Les alertes ne seront plus générées pour cette combinaison véhicule/zone."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Véhicule retiré de la zone avec succès",
            content = @Content(schema = @Schema(implementation = VehicleDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Véhicule ou zone non trouvé(e)"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Vous devez être propriétaire du véhicule"
        )
    })
    public Mono<ResponseEntity<VehicleDTOResponse>> removeFromGeofenceZone(
            @Parameter(description = "ID du véhicule", required = true)
            @PathVariable UUID vehicleId,
            @Parameter(description = "ID de la zone de géofence", required = true)
            @PathVariable UUID zoneId
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> vehicleService.removeFromGeofenceZone(vehicleId, zoneId, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(response -> ResponseEntity.status(200).body(response));
    }
}
