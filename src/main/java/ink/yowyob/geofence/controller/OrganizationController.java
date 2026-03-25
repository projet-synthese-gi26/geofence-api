package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.TenantDTORequest;
import ink.yowyob.geofence.dto.response.TenantDTOResponse;
import ink.yowyob.geofence.dto.response.TenantListResponse;
import ink.yowyob.geofence.service.OrganizationService;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Tag(
    name = "Tenants / Organisations",
    description = "API de gestion des tenants/organisations - Réservé aux administrateurs. " +
                  "Permet de créer, modifier et gérer les différents tenants du système multi-tenant."
)
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearer-jwt")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(
        summary = "Récupérer tous les tenants",
        description = """
            Récupère la liste complète de tous les tenants/organisations enregistrés dans le système.
            
            **Accès réservé aux ADMIN uniquement.**
            
            Les informations retournées incluent :
            - Les détails de l'organisation (nom, domaine, contact)
            - La clé API du tenant
            - Le plan d'abonnement et les quotas (utilisateurs, véhicules, géofences)
            - Le statut (actif/inactif, interne/externe)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste des tenants récupérée avec succès",
            content = @Content(schema = @Schema(implementation = TenantListResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN requis"
        )
    })
    public Mono<ResponseEntity<TenantListResponse>> getAllTenants() {
        return Mono.fromCallable(() -> organizationService.getAllTenants())
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{tenantId}")
    @Operation(
        summary = "Récupérer un tenant par son ID",
        description = "Récupère les détails complets d'un tenant/organisation spécifique. " +
                      "**Accès réservé aux ADMIN uniquement.**"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tenant trouvé",
            content = @Content(schema = @Schema(implementation = TenantDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tenant non trouvé"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN requis"
        )
    })
    public Mono<ResponseEntity<TenantDTOResponse>> getTenantById(
            @Parameter(description = "ID du tenant", required = true)
            @PathVariable UUID tenantId
    ) {
        return Mono.fromCallable(() -> organizationService.getTenantById(tenantId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @PostMapping
    @Operation(
        summary = "Créer un nouveau tenant",
        description = """
            Crée un nouveau tenant/organisation dans le système.
            
            **Accès réservé aux ADMIN uniquement.**
            
            **Fonctionnalités :**
            - Génération automatique d'une clé API unique pour le tenant
            - Configuration des quotas (utilisateurs, véhicules, géofences)
            - Définition du plan d'abonnement (FREE, BASIC, PREMIUM, ENTERPRISE, UNLIMITED)
            - Configuration des webhooks pour notifications externes
            
            **Plans d'abonnement disponibles :**
            - **FREE** : Usage limité (10 users, 50 vehicles, 20 geofences)
            - **BASIC** : Usage standard (50 users, 200 vehicles, 100 geofences)
            - **PREMIUM** : Usage avancé (200 users, 1000 vehicles, 500 geofences)
            - **ENTERPRISE** : Usage sur mesure
            - **UNLIMITED** : Aucune limite (réservé au tenant interne)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Tenant créé avec succès",
            content = @Content(schema = @Schema(implementation = TenantDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Données invalides - Vérifiez les champs requis"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN requis"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Données du tenant à créer",
        content = @Content(schema = @Schema(implementation = TenantDTORequest.class))
    )
    public Mono<ResponseEntity<TenantDTOResponse>> createTenant(
            @Valid @RequestBody TenantDTORequest request
    ) {
        return Mono.fromCallable(() -> organizationService.createTenant(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(tenant -> ResponseEntity.status(201).body(tenant));
    }

    @PutMapping("/{tenantId}")
    @Operation(
        summary = "Mettre à jour un tenant",
        description = """
            Met à jour les informations d'un tenant/organisation existant.
            
            **Accès réservé aux ADMIN uniquement.**
            
            **Restrictions :**
            - Les tenants internes (frontend) ne peuvent pas être modifiés
            - La clé API ne peut pas être changée
            - Le statut actif/inactif doit être géré via les endpoints dédiés
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tenant mis à jour avec succès",
            content = @Content(schema = @Schema(implementation = TenantDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tenant non trouvé"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Modification d'un tenant interne interdite"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN requis"
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Nouvelles données du tenant",
        content = @Content(schema = @Schema(implementation = TenantDTORequest.class))
    )
    public Mono<ResponseEntity<TenantDTOResponse>> updateTenant(
            @Parameter(description = "ID du tenant", required = true)
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDTORequest request
    ) {
        return Mono.fromCallable(() -> organizationService.updateTenant(tenantId, request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{tenantId}/deactivate")
    @Operation(
        summary = "Désactiver un tenant",
        description = """
            Désactive un tenant/organisation (soft delete).
            
            **Accès réservé aux ADMIN uniquement.**
            
            **Effet :**
            - Le tenant ne pourra plus utiliser l'API
            - Les utilisateurs du tenant ne pourront plus se connecter
            - Les données sont conservées et peuvent être réactivées
            
            **Restriction :** Les tenants internes ne peuvent pas être désactivés.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Tenant désactivé avec succès"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tenant non trouvé"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Désactivation d'un tenant interne interdite"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN requis"
        )
    })
    public Mono<ResponseEntity<Void>> deactivateTenant(
            @Parameter(description = "ID du tenant", required = true)
            @PathVariable UUID tenantId
    ) {
        return Mono.fromCallable(() -> {
                    organizationService.deactivateTenant(tenantId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{tenantId}/activate")
    @Operation(
        summary = "Activer un tenant",
        description = "Active ou réactive un tenant/organisation. " +
                      "**Accès réservé aux ADMIN uniquement.**"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tenant activé avec succès",
            content = @Content(schema = @Schema(implementation = TenantDTOResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tenant non trouvé"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN requis"
        )
    })
    public Mono<ResponseEntity<TenantDTOResponse>> activateTenant(
            @Parameter(description = "ID du tenant", required = true)
            @PathVariable UUID tenantId
    ) {
        return Mono.fromCallable(() -> organizationService.activateTenant(tenantId))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{tenantId}")
    @Operation(
        summary = "Supprimer définitivement un tenant",
        description = """
            Supprime définitivement un tenant/organisation et toutes ses données associées.
            
            **Accès réservé aux ADMIN uniquement.**
            
            ⚠️ **ATTENTION : Cette opération est IRRÉVERSIBLE !**
            
            **Données supprimées :**
            - L'organisation elle-même
            - Tous les utilisateurs du tenant
            - Tous les véhicules, géofences, routes, alertes, etc.
            
            **Restriction :** Les tenants internes ne peuvent pas être supprimés.
            
            **Recommandation :** Préférer la désactivation pour conserver les données.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Tenant supprimé définitivement"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tenant non trouvé"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Suppression d'un tenant interne interdite"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé - Droits ADMIN requis"
        )
    })
    public Mono<ResponseEntity<Void>> deleteTenant(
            @Parameter(description = "ID du tenant", required = true)
            @PathVariable UUID tenantId
    ) {
        return Mono.fromCallable(() -> {
                    organizationService.deleteTenant(tenantId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
