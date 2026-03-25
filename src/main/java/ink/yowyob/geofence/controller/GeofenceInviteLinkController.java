package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.CreateInviteLinkRequest;
import ink.yowyob.geofence.dto.response.InviteLinkDTO;
import ink.yowyob.geofence.dto.response.InviteLinkDetailsResponse;
import ink.yowyob.geofence.dto.response.InviteLinkListResponse;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.GeofenceInviteLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * @apiDefine UserPermission
 * @apiPermission user
 * @apiHeader {String} Authorization Bearer token (JWT)
 * @apiHeaderExample {json} Header-Example:
 *     {
 *       "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 *     }
 */

/**
 * @apiDefine AdminPermission
 * @apiPermission admin
 * @apiHeader {String} Authorization Bearer token (JWT) - Rôle ADMIN requis
 * @apiHeaderExample {json} Header-Example:
 *     {
 *       "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 *     }
 */

/**
 * @apiDefine ManagerPermission
 * @apiPermission manager
 * @apiHeader {String} Authorization Bearer token (JWT) - Rôle ADMIN ou MANAGER requis
 * @apiHeaderExample {json} Header-Example:
 *     {
 *       "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 *     }
 */

/**
 * @apiDefine ErrorResponse
 * @apiError (Error 4xx) {Number} status Code d'erreur HTTP
 * @apiError (Error 4xx) {String} message Message d'erreur
 * @apiErrorExample {json} Error-Response:
 *     HTTP/1.1 400 Bad Request
 *     {
 *       "status": 400,
 *       "message": "Données invalides"
 *     }
 */

@RestController
@RequestMapping("/api/geofence/invite")
@RequiredArgsConstructor
public class GeofenceInviteLinkController {

    private final GeofenceInviteLinkService inviteLinkService;
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

    /**
     * @api {post} /geofence/invite Créer un lien d'invitation
     * @apiName CreateInviteLink
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Créer un lien d'invitation pour partager une géofence publiquement
     *
     * @apiUse UserPermission
     *
     * @apiBody {String} geofenceId Identifiant de la géofence (requis)
     * @apiBody {String="c","p"} geofenceType Type de géofence (requis)
     * @apiBody {String} [expiresAt] Date d'expiration (ISO 8601)
     * @apiBody {Boolean} [canEdit=false] Autoriser la modification
     * @apiBody {Number{-1,1-}} [maxUses=-1] Nombre max d'utilisations (-1 = illimité)
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *       "geofenceId": "123e4567-e89b-12d3-a456-426614174000",
     *       "geofenceType": "c",
     *       "expiresAt": "2024-12-31T23:59:59Z",
     *       "canEdit": false,
     *       "maxUses": 10
     *     }
     *
     * @apiSuccess {String} id Identifiant du lien
     * @apiSuccess {String} inviteCode Code d'invitation (12 caractères)
     * @apiSuccess {String} geofenceId Identifiant de la géofence
     * @apiSuccess {String} geofenceType Type de géofence
     * @apiSuccess {Object} createdBy Utilisateur créateur
     * @apiSuccess {String} createdAt Date de création
     * @apiSuccess {String} [expiresAt] Date d'expiration
     * @apiSuccess {Boolean} canEdit Autorisation de modification
     * @apiSuccess {Boolean} isActive Lien actif
     * @apiSuccess {Number} maxUses Nombre max d'utilisations
     * @apiSuccess {Number} currentUses Nombre d'utilisations actuelles
     * @apiSuccess {Boolean} canBeUsed Lien utilisable
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "id": "456e7890-e89b-12d3-a456-426614174001",
     *       "inviteCode": "ABC123XYZ789",
     *       "geofenceId": "123e4567-e89b-12d3-a456-426614174000",
     *       "geofenceType": "c",
     *       "createdBy": {
     *         "username": "jeandupont"
     *       },
     *       "createdAt": "2024-01-08T10:30:00Z",
     *       "expiresAt": "2024-12-31T23:59:59Z",
     *       "canEdit": false,
     *       "isActive": true,
     *       "maxUses": 10,
     *       "currentUses": 0,
     *       "canBeUsed": true
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiError (Error 403) Forbidden Vous n'êtes pas propriétaire de cette géofence
     * @apiUse ErrorResponse
     */

    /**
     * @api {post} /geofence/invite Créer un lien d'invitation
     * @apiName CreateInviteLinkV2
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Créer un lien d'invitation pour partager une géofence publiquement (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiBody {String} geofenceId Identifiant de la géofence (requis)
     * @apiBody {String="c","p"} geofenceType Type de géofence (requis)
     * @apiBody {String} [expiresAt] Date d'expiration (ISO 8601)
     * @apiBody {Boolean} [canEdit=false] Autoriser la modification
     * @apiBody {Number{-1,1-}} [maxUses=-1] Nombre max d'utilisations (-1 = illimité)
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *       "geofenceId": "123e4567-e89b-12d3-a456-426614174000",
     *       "geofenceType": "c",
     *       "expiresAt": "2024-12-31T23:59:59Z",
     *       "canEdit": false,
     *       "maxUses": 10
     *     }
     *
     * @apiSuccess {String} id Identifiant du lien
     * @apiSuccess {String} inviteCode Code d'invitation (12 caractères)
     * @apiSuccess {String} geofenceId Identifiant de la géofence
     * @apiSuccess {String} geofenceType Type de géofence
     * @apiSuccess {Object} createdBy Utilisateur créateur
     * @apiSuccess {String} createdAt Date de création
     * @apiSuccess {String} [expiresAt] Date d'expiration
     * @apiSuccess {Boolean} canEdit Autorisation de modification
     * @apiSuccess {Boolean} isActive Lien actif
     * @apiSuccess {Number} maxUses Nombre max d'utilisations
     * @apiSuccess {Number} currentUses Nombre d'utilisations actuelles
     * @apiSuccess {Boolean} canBeUsed Lien utilisable
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "id": "456e7890-e89b-12d3-a456-426614174001",
     *       "inviteCode": "ABC123XYZ789",
     *       "geofenceId": "123e4567-e89b-12d3-a456-426614174000",
     *       "geofenceType": "c",
     *       "createdBy": {
     *         "username": "jeandupont"
     *       },
     *       "createdAt": "2024-01-08T10:30:00Z",
     *       "expiresAt": "2024-12-31T23:59:59Z",
     *       "canEdit": false,
     *       "isActive": true,
     *       "maxUses": 10,
     *       "currentUses": 0,
     *       "canBeUsed": true
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiError (Error 403) Forbidden Vous n'êtes pas propriétaire de cette géofence
     * @apiUse ErrorResponse
     */
    @PostMapping
    public Mono<ResponseEntity<InviteLinkDTO>> createInviteLink(@Valid @RequestBody CreateInviteLinkRequest request) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> inviteLinkService.createInviteLink(request, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /geofence/invite/:inviteCode Détails d'un lien d'invitation
     * @apiName GetInviteLinkDetails
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Récupère les détails d'un lien d'invitation (accès public)
     *
     * @apiParam (Path) {String} inviteCode Code d'invitation (12 caractères)
     *
     * @apiSuccess {Object} inviteLink Détails du lien
     * @apiSuccess {Object} geofenceZone Géofence associée
     * @apiSuccess {String} fullInviteUrl URL complète d'invitation
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "inviteLink": {
     *         "inviteCode": "ABC123XYZ789",
     *         "canEdit": false,
     *         "maxUses": 10,
     *         "currentUses": 3,
     *         "canBeUsed": true
     *       },
     *       "geofenceZone": {
     *         "id": "123e4567-e89b-12d3-a456-426614174000",
     *         "title": "Zone Bureau",
     *         "type": "circle"
     *       },
     *       "fullInviteUrl": "http://localhost:3000/invite/ABC123XYZ789"
     *     }
     *
     * @apiError (Error 404) NotFound Lien non trouvé ou expiré
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /geofence/invite/:inviteCode Détails d'un lien d'invitation
     * @apiName GetInviteLinkDetailsV2
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Récupère les détails d'un lien d'invitation (accès public) (version 2.0.0 avec améliorations)
     *
     * @apiParam (Path) {String} inviteCode Code d'invitation (12 caractères)
     *
     * @apiSuccess {Object} inviteLink Détails du lien
     * @apiSuccess {Object} geofenceZone Géofence associée
     * @apiSuccess {String} fullInviteUrl URL complète d'invitation
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "inviteLink": {
     *         "inviteCode": "ABC123XYZ789",
     *         "canEdit": false,
     *         "maxUses": 10,
     *         "currentUses": 3,
     *         "canBeUsed": true
     *       },
     *       "geofenceZone": {
     *         "id": "123e4567-e89b-12d3-a456-426614174000",
     *         "title": "Zone Bureau",
     *         "type": "circle"
     *       },
     *       "fullInviteUrl": "http://localhost:3000/invite/ABC123XYZ789"
     *     }
     *
     * @apiError (Error 404) NotFound Lien non trouvé ou expiré
     * @apiUse ErrorResponse
     */
    @GetMapping("/{inviteCode}")
    public Mono<ResponseEntity<InviteLinkDetailsResponse>> getInviteLinkDetails(@PathVariable String inviteCode) {
        return Mono.fromCallable(() -> inviteLinkService.getInviteLinkDetails(inviteCode))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /**
     * @api {post} /geofence/invite/join/:inviteCode Rejoindre via invitation
     * @apiName JoinGeofenceViaInvite
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Rejoindre une géofence en utilisant un code d'invitation
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} inviteCode Code d'invitation
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiError (Error 404) NotFound Lien non trouvé ou expiré
     * @apiError (Error 400) BadRequest Lien non valide ou limite atteinte
     * @apiError (Error 409) Conflict Vous avez déjà accès à cette géofence
     * @apiUse ErrorResponse
     */

    /**
     * @api {post} /geofence/invite/join/:inviteCode Rejoindre via invitation
     * @apiName JoinGeofenceViaInviteV2
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Rejoindre une géofence en utilisant un code d'invitation (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} inviteCode Code d'invitation
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiError (Error 404) NotFound Lien non trouvé ou expiré
     * @apiError (Error 400) BadRequest Lien non valide ou limite atteinte
     * @apiError (Error 409) Conflict Vous avez déjà accès à cette géofence
     * @apiUse ErrorResponse
     */
    @PostMapping("/join/{inviteCode}")
    public Mono<ResponseEntity<Void>> joinGeofenceViaInvite(@PathVariable String inviteCode) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    inviteLinkService.joinGeofenceViaInvite(inviteCode, user);
                    return ResponseEntity.ok().<Void>build();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * @api {get} /geofence/invite/my-links Mes liens d'invitation
     * @apiName GetMyInviteLinks
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les liens d'invitation créés par l'utilisateur
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} inviteLinks Liste des liens d'invitation
     * @apiSuccess {Number} totalItems Nombre total de liens
     *
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /geofence/invite/my-links Mes liens d'invitation
     * @apiName GetMyInviteLinksV2
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les liens d'invitation créés par l'utilisateur (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} inviteLinks Liste des liens d'invitation
     * @apiSuccess {Number} totalItems Nombre total de liens
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/my-links")
    public Mono<ResponseEntity<InviteLinkListResponse>> getMyInviteLinks() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> inviteLinkService.getMyInviteLinks(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /geofence/invite/geofence/:geofenceId/:geofenceType Liens d'une géofence
     * @apiName GetInviteLinksForGeofence
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les liens d'invitation actifs pour une géofence spécifique
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence
     * @apiParam (Path) {String="c","p"} geofenceType Type de géofence
     *
     * @apiSuccess {Object[]} inviteLinks Liste des liens d'invitation
     * @apiSuccess {Number} totalItems Nombre total de liens
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "inviteLinks": [
     *         {
     *           "id": "456e7890-e89b-12d3-a456-426614174001",
     *           "inviteCode": "ABC123XYZ789",
     *           "canEdit": false,
     *           "maxUses": 10,
     *           "currentUses": 3,
     *           "isActive": true,
     *           "canBeUsed": true
     *         }
     *       ],
     *       "totalItems": 1
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiError (Error 403) Forbidden Vous n'êtes pas propriétaire de cette géofence
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /geofence/invite/geofence/:geofenceId/:geofenceType Liens d'une géofence
     * @apiName GetInviteLinksForGeofenceV2
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les liens d'invitation actifs pour une géofence spécifique (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence
     * @apiParam (Path) {String="c","p"} geofenceType Type de géofence
     *
     * @apiSuccess {Object[]} inviteLinks Liste des liens d'invitation
     * @apiSuccess {Number} totalItems Nombre total de liens
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "inviteLinks": [
     *         {
     *           "id": "456e7890-e89b-12d3-a456-426614174001",
     *           "inviteCode": "ABC123XYZ789",
     *           "canEdit": false,
     *           "maxUses": 10,
     *           "currentUses": 3,
     *           "isActive": true,
     *           "canBeUsed": true
     *         }
     *       ],
     *       "totalItems": 1
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiError (Error 403) Forbidden Vous n'êtes pas propriétaire de cette géofence
     * @apiUse ErrorResponse
     */
    @GetMapping("/geofence/{geofenceId}/{geofenceType}")
    public Mono<ResponseEntity<InviteLinkListResponse>> getInviteLinksForGeofence(
            @PathVariable UUID geofenceId, @PathVariable String geofenceType) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> inviteLinkService.getInviteLinksForGeofence(geofenceId, geofenceType, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }


    /**
     * @api {put} /geofence/invite/:inviteCode/deactivate Désactiver un lien
     * @apiName DeactivateInviteLink
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Désactiver un lien d'invitation sans le supprimer
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} inviteCode Code d'invitation à désactiver
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiError (Error 404) NotFound Lien d'invitation non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le créateur de ce lien
     * @apiUse ErrorResponse
     */

    /**
     * @api {put} /geofence/invite/:inviteCode/deactivate Désactiver un lien
     * @apiName DeactivateInviteLinkV2
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Désactiver un lien d'invitation sans le supprimer (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} inviteCode Code d'invitation à désactiver
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiError (Error 404) NotFound Lien d'invitation non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le créateur de ce lien
     * @apiUse ErrorResponse
     */
    @PutMapping("/{inviteCode}/deactivate")
    public Mono<ResponseEntity<Void>> deactivateInviteLink(@PathVariable String inviteCode) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    inviteLinkService.deactivateInviteLink(inviteCode, user);
                    return ResponseEntity.ok().<Void>build();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * @api {delete} /geofence/invite/:inviteLinkId Supprimer un lien d'invitation
     * @apiName DeleteInviteLink
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Supprimer définitivement un lien d'invitation
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} inviteLinkId Identifiant du lien d'invitation
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 204 No Content
     *
     * @apiError (Error 404) NotFound Lien d'invitation non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le créateur de ce lien
     * @apiUse ErrorResponse
     */

    /**
     * @api {delete} /geofence/invite/:inviteLinkId Supprimer un lien d'invitation
     * @apiName DeleteInviteLinkV2
     * @apiGroup Invite Links
     * @apiVersion 2.0.0
     * @apiDescription Supprimer définitivement un lien d'invitation (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} inviteLinkId Identifiant du lien d'invitation
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 204 No Content
     *
     * @apiError (Error 404) NotFound Lien d'invitation non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le créateur de ce lien
     * @apiUse ErrorResponse
     */
    @DeleteMapping("/{inviteLinkId}")
    public Mono<ResponseEntity<Void>> deleteInviteLink(@PathVariable UUID inviteLinkId) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    inviteLinkService.deleteInviteLink(inviteLinkId, user);
                    return ResponseEntity.noContent().<Void>build();
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}