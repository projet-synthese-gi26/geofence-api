package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.ShareGeofenceRequest;
import ink.yowyob.geofence.dto.response.GeofenceShareDTO;
import ink.yowyob.geofence.dto.response.GeofenceShareListResponse;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.Implementation.GeofenceSharingServiceImpl;
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
@RequestMapping("/api/geofence/share")
@RequiredArgsConstructor
public class GeofenceSharingController {
    private final GeofenceSharingServiceImpl geofenceSharingService;
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
     * @api {post} /geofence/share/:type/:geofenceId Partager une géofence
     * @apiName ShareGeofence
     * @apiGroup Geofence Sharing
     * @apiVersion 2.0.0
     * @apiDescription Partager une zone de géofence avec un autre utilisateur
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la zone à partager
     * @apiParam (Path) {String="c","p"} type Type de zone (c=circle, p=polygon)
     *
     * @apiBody {String} targetUserId Identifiant de l'utilisateur cible (requis)
     * @apiBody {String} [expiresAt] Date d'expiration du partage (ISO 8601)
     * @apiBody {Boolean} [canEdit=false] Autoriser la modification
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *       "targetUserId": "456e7890-e89b-12d3-a456-426614174001",
     *       "expiresAt": "2024-12-31T23:59:59Z",
     *       "canEdit": true
     *     }
     *
     * @apiSuccess (201) {String} id Identifiant du partage
     * @apiSuccess (201) {Object} geofenceZone Zone partagée
     * @apiSuccess (201) {Object} sharedBy Utilisateur qui partage
     * @apiSuccess (201) {Object} sharedWith Utilisateur qui reçoit
     * @apiSuccess (201) {String} sharedAt Date de partage
     * @apiSuccess (201) {String} [expiresAt] Date d'expiration
     * @apiSuccess (201) {Boolean} canEdit Autorisation de modification
     * @apiSuccess (201) {String="PENDING","ACCEPTED","REFUSED"} status Statut du partage
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 201 Created
     *     {
     *       "id": "789e0123-e89b-12d3-a456-426614174002",
     *       "geofenceZone": {
     *         "id": "123e4567-e89b-12d3-a456-426614174000",
     *         "title": "Zone Bureau",
     *         "type": "circle"
     *       },
     *       "sharedBy": {
     *         "uuid": "456e7890-e89b-12d3-a456-426614174001",
     *         "username": "jeandupont"
     *       },
     *       "sharedWith": {
     *         "uuid": "012e3456-e89b-12d3-a456-426614174003",
     *         "username": "mariemartin"
     *       },
     *       "sharedAt": "2024-01-08T10:30:00Z",
     *       "expiresAt": "2024-12-31T23:59:59Z",
     *       "canEdit": true,
     *       "status": "PENDING",
     *       "respondedAt": null
     *     }
     *
     * @apiError (Error 404) NotFound Zone ou utilisateur non trouvé
     * @apiError (Error 403) Forbidden Accès refusé - Vous n'êtes pas propriétaire
     * @apiUse ErrorResponse
     */
    @PostMapping("/{type}/{geofenceId}")
    public Mono<ResponseEntity<GeofenceShareDTO>> shareGeofence(
            @PathVariable UUID geofenceId, @PathVariable String type,
            @RequestBody ShareGeofenceRequest request) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> geofenceSharingService.shareGeofence(geofenceId, type, request, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(result -> ResponseEntity.status(201).body(result));
    }

    /**
     * @api {put} /geofence/share/:shareId Modifier un partage
     * @apiName UpdateGeofenceShare
     * @apiGroup Geofence Sharing
     * @apiVersion 2.0.0
     * @apiDescription Modifier les paramètres d'un partage existant
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} shareId Identifiant du partage
     *
     * @apiBody {String} [targetUserId] Nouvel utilisateur cible
     * @apiBody {String} [expiresAt] Nouvelle date d'expiration (ISO 8601)
     * @apiBody {Boolean} [canEdit] Nouvelle autorisation de modification
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *       "expiresAt": "2025-12-31T23:59:59Z",
     *       "canEdit": false
     *     }
     *
     * @apiSuccess {String} id Identifiant du partage
     * @apiSuccess {Object} geofenceZone Zone partagée
     * @apiSuccess {Object} sharedBy Utilisateur qui partage
     * @apiSuccess {Object} sharedWith Utilisateur qui reçoit
     * @apiSuccess {String} sharedAt Date de partage
     * @apiSuccess {String} [expiresAt] Date d'expiration
     * @apiSuccess {Boolean} canEdit Autorisation de modification
     * @apiSuccess {String="PENDING","ACCEPTED","REFUSED"} status Statut du partage
     *
     * @apiError (Error 404) NotFound Partage non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le propriétaire de ce partage
     * @apiUse ErrorResponse
     */
    @PutMapping("/{shareId}")
    public Mono<ResponseEntity<GeofenceShareDTO>> updateGeofenceShare(
            @PathVariable UUID shareId, @RequestBody ShareGeofenceRequest request) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> geofenceSharingService.updateGeofenceShare(shareId, request, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {delete} /geofence/share/:shareId Supprimer un partage
     * @apiName DeleteGeofenceShare
     * @apiGroup Geofence Sharing
     * @apiVersion 2.0.0
     * @apiDescription Supprimer un partage de géofence
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} shareId Identifiant du partage
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 204 No Content
     *
     * @apiError (Error 404) NotFound Partage non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le propriétaire de ce partage
     * @apiUse ErrorResponse
     */
    @DeleteMapping("/{shareId}")
    public Mono<ResponseEntity<Void>> deleteGeofenceShare(@PathVariable UUID shareId) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    geofenceSharingService.deleteGeofenceShare(shareId, user);
                    return ResponseEntity.noContent().<Void>build();
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * @api {get} /geofence/share/shared-with-me Géofences partagées avec moi
     * @apiName GetSharedGeofences
     * @apiGroup Geofence Sharing
     * @apiVersion 2.0.0
     * @apiDescription Récupère les zones de géofence partagées avec l'utilisateur connecté (acceptées)
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} shares Liste des partages acceptés
     * @apiSuccess {Number} totalItems Nombre total de partages
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "shares": [
     *         {
     *           "id": "789e0123-e89b-12d3-a456-426614174002",
     *           "geofenceZone": {
     *             "id": "123e4567-e89b-12d3-a456-426614174000",
     *             "title": "Zone Bureau",
     *             "type": "circle"
     *           },
     *           "sharedBy": {
     *             "username": "jeandupont"
     *           },
     *           "status": "ACCEPTED",
     *           "canEdit": true
     *         }
     *       ],
     *       "totalItems": 1
     *     }
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/shared-with-me")
    public Mono<ResponseEntity<GeofenceShareListResponse>> getSharedGeofences() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> geofenceSharingService.getSharedGeofences(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /geofence/share/my-shares Mes partages créés
     * @apiName GetMySharedGeofences
     * @apiGroup Geofence Sharing
     * @apiVersion 2.0.0
     * @apiDescription Récupère les partages de géofences créés par l'utilisateur connecté
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} shares Liste des partages créés
     * @apiSuccess {Number} totalItems Nombre total de partages
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "shares": [
     *         {
     *           "id": "789e0123-e89b-12d3-a456-426614174002",
     *           "geofenceZone": {
     *             "id": "123e4567-e89b-12d3-a456-426614174000",
     *             "title": "Zone Bureau",
     *             "type": "circle"
     *           },
     *           "sharedWith": {
     *             "username": "mariemartin"
     *           },
     *           "status": "ACCEPTED",
     *           "canEdit": true,
     *           "sharedAt": "2024-01-08T10:30:00Z"
     *         }
     *       ],
     *       "totalItems": 1
     *     }
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/my-shares")
    public Mono<ResponseEntity<GeofenceShareListResponse>> getMySharedGeofences() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> geofenceSharingService.getMySharedGeofences(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /geofence/share/invitations/pending Invitations en attente
     * @apiName GetPendingInvitations
     * @apiGroup Geofence Sharing
     * @apiVersion 2.0.0
     * @apiDescription Récupère les invitations de partage en attente de réponse
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} shares Liste des invitations en attente
     * @apiSuccess {Number} totalItems Nombre total d'invitations
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/invitations/pending")
    public Mono<ResponseEntity<GeofenceShareListResponse>> getPendingInvitations() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> geofenceSharingService.getPendingInvitations(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {post} /geofence/share/invitations/:shareId/respond Répondre à une invitation
     * @apiName RespondToInvitation
     * @apiGroup Geofence Sharing
     * @apiVersion 2.0.0
     * @apiDescription Accepter ou refuser une invitation de partage
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} shareId Identifiant de l'invitation
     * @apiQuery {Boolean} accept Accepter (true) ou refuser (false) l'invitation
     *
     * @apiParamExample {curl} Request-Example:
     *     curl -X POST "http://localhost:8080/api/geofence/share/invitations/789e0123-e89b-12d3-a456-426614174002/respond?accept=true" \
     *       -H "Authorization: Bearer your-jwt-token"
     *
     * @apiSuccess {Object} share Invitation mise à jour
     * @apiSuccess {String="ACCEPTED","REFUSED"} share.status Nouveau statut
     * @apiSuccess {String} share.respondedAt Date de réponse
     *
     * @apiError (Error 404) NotFound Invitation non trouvée
     * @apiError (Error 403) Forbidden Vous n'êtes pas le destinataire de cette invitation
     * @apiError (Error 400) BadRequest Invitation déjà traitée ou expirée
     * @apiUse ErrorResponse
     */
    @PostMapping("/invitations/{shareId}/respond")
    public Mono<ResponseEntity<GeofenceShareDTO>> respondToInvitation(
            @PathVariable UUID shareId, @RequestParam boolean accept) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> geofenceSharingService.respondToInvitation(shareId, accept, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }
}