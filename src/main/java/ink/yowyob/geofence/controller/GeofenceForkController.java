package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.ForkGeofenceRequest;
import ink.yowyob.geofence.dto.response.GeofenceForkDTO;
import ink.yowyob.geofence.dto.response.GeofenceForkInfoDTO;
import ink.yowyob.geofence.dto.response.GeofenceForkListResponse;
import ink.yowyob.geofence.dto.response.GeofenceWithForkInfoDTO;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.GeofenceForkService;
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
@RequestMapping("/api/geofence/fork")
@RequiredArgsConstructor
public class GeofenceForkController {

    private final GeofenceForkService forkService;
    private final UserRepository userRepository;

    /**
     * @api {post} /geofence/fork Créer un fork de géofence
     * @apiName ForkGeofence
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Créer une copie (fork) d'une géofence existante
     *
     * @apiUse UserPermission
     *
     * @apiBody {String} geofenceId Identifiant de la géofence à forker (requis)
     * @apiBody {String="c","p"} geofenceType Type de géofence (requis)
     * @apiBody {String{1-100}} newTitle Nouveau titre pour le fork (requis)
     * @apiBody {String{0-500}} [newDescription] Nouvelle description
     * @apiBody {String{0-500}} [forkReason] Raison du fork
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *       "geofenceId": "123e4567-e89b-12d3-a456-426614174000",
     *       "geofenceType": "c",
     *       "newTitle": "Ma Zone Bureau",
     *       "newDescription": "Version personnalisée de la zone bureau",
     *       "forkReason": "Adaptation aux besoins spécifiques"
     *     }
     *
     * @apiSuccess {String} id Identifiant du fork
     * @apiSuccess {Object} originalGeofence Géofence originale
     * @apiSuccess {Object} forkedGeofence Nouvelle géofence créée
     * @apiSuccess {Object} forkedBy Utilisateur ayant créé le fork
     * @apiSuccess {String} forkedAt Date de création du fork
     * @apiSuccess {String} [forkReason] Raison du fork
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "id": "789e0123-e89b-12d3-a456-426614174002",
     *       "originalGeofence": {
     *         "id": "123e4567-e89b-12d3-a456-426614174000",
     *         "title": "Zone Bureau",
     *         "type": "circle"
     *       },
     *       "forkedGeofence": {
     *         "id": "456e7890-e89b-12d3-a456-426614174001",
     *         "title": "Ma Zone Bureau",
     *         "type": "circle"
     *       },
     *       "forkedBy": {
     *         "username": "mariemartin"
     *       },
     *       "forkedAt": "2024-01-08T10:30:00Z",
     *       "forkReason": "Adaptation aux besoins spécifiques"
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiError (Error 403) Forbidden Accès refusé à cette géofence
     * @apiUse ErrorResponse
     */

    /**
     * @api {post} /geofence/fork Créer un fork de géofence
     * @apiName ForkGeofenceV2
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Créer une copie (fork) d'une géofence existante (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiBody {String} geofenceId Identifiant de la géofence à forker (requis)
     * @apiBody {String="c","p"} geofenceType Type de géofence (requis)
     * @apiBody {String{1-100}} newTitle Nouveau titre pour le fork (requis)
     * @apiBody {String{0-500}} [newDescription] Nouvelle description
     * @apiBody {String{0-500}} [forkReason] Raison du fork
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *       "geofenceId": "123e4567-e89b-12d3-a456-426614174000",
     *       "geofenceType": "c",
     *       "newTitle": "Ma Zone Bureau",
     *       "newDescription": "Version personnalisée de la zone bureau",
     *       "forkReason": "Adaptation aux besoins spécifiques"
     *     }
     *
     * @apiSuccess {String} id Identifiant du fork
     * @apiSuccess {Object} originalGeofence Géofence originale
     * @apiSuccess {Object} forkedGeofence Nouvelle géofence créée
     * @apiSuccess {Object} forkedBy Utilisateur ayant créé le fork
     * @apiSuccess {String} forkedAt Date de création du fork
     * @apiSuccess {String} [forkReason] Raison du fork
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "id": "789e0123-e89b-12d3-a456-426614174002",
     *       "originalGeofence": {
     *         "id": "123e4567-e89b-12d3-a456-426614174000",
     *         "title": "Zone Bureau",
     *         "type": "circle"
     *       },
     *       "forkedGeofence": {
     *         "id": "456e7890-e89b-12d3-a456-426614174001",
     *         "title": "Ma Zone Bureau",
     *         "type": "circle"
     *       },
     *       "forkedBy": {
     *         "username": "mariemartin"
     *       },
     *       "forkedAt": "2024-01-08T10:30:00Z",
     *       "forkReason": "Adaptation aux besoins spécifiques"
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiError (Error 403) Forbidden Accès refusé à cette géofence
     * @apiUse ErrorResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<GeofenceForkDTO>> forkGeofence(@Valid @RequestBody ForkGeofenceRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                            return forkService.forkGeofence(request, user);
                        }
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /geofence/fork/of/:geofenceId Forks d'une géofence
     * @apiName GetForksOfGeofence
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les forks d'une géofence spécifique
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence originale
     *
     * @apiSuccess {Object[]} forks Liste des forks
     * @apiSuccess {Number} totalItems Nombre total de forks
     *
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /geofence/fork/of/:geofenceId Forks d'une géofence
     * @apiName GetForksOfGeofenceV2
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les forks d'une géofence spécifique (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence originale
     *
     * @apiSuccess {Object[]} forks Liste des forks
     * @apiSuccess {Number} totalItems Nombre total de forks
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/of/{geofenceId}")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<GeofenceForkListResponse>> getForksOfGeofence(@PathVariable UUID geofenceId) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName)
            .flatMap(username -> Mono.fromCallable(() -> {
                var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                return forkService.getForksOfGeofence(geofenceId, user);
            }))
            .subscribeOn(Schedulers.boundedElastic())
            .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /geofence/fork/my-forks Mes forks
     * @apiName GetMyForks
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les forks créés par l'utilisateur connecté
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} forks Liste des forks créés
     * @apiSuccess {Number} totalItems Nombre total de forks
     *
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /geofence/fork/my-forks Mes forks
     * @apiName GetMyForksV2
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère tous les forks créés par l'utilisateur connecté (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} forks Liste des forks créés
     * @apiSuccess {Number} totalItems Nombre total de forks
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/my-forks")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<GeofenceForkListResponse>> getMyForks() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return forkService.getMyForks(user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /geofence/fork/info/:geofenceId/:geofenceType Informations de fork
     * @apiName GetForkInfo
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère les informations de fork d'une géofence (si c'est un fork)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence
     * @apiParam (Path) {String="c","p"} geofenceType Type de géofence
     *
     * @apiSuccess {String} originalId Identifiant de la géofence originale
     * @apiSuccess {String} originalTitle Titre de la géofence originale
     * @apiSuccess {Object} originalOwner Propriétaire original
     * @apiSuccess {String} forkedAt Date de création du fork
     * @apiSuccess {String} [forkReason] Raison du fork
     *
     * @apiError (Error 404) NotFound Géofence non trouvée ou n'est pas un fork
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /geofence/fork/info/:geofenceId/:geofenceType Informations de fork
     * @apiName GetForkInfoV2
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère les informations de fork d'une géofence (si c'est un fork) (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence
     * @apiParam (Path) {String="c","p"} geofenceType Type de géofence
     *
     * @apiSuccess {String} originalId Identifiant de la géofence originale
     * @apiSuccess {String} originalTitle Titre de la géofence originale
     * @apiSuccess {Object} originalOwner Propriétaire original
     * @apiSuccess {String} forkedAt Date de création du fork
     * @apiSuccess {String} [forkReason] Raison du fork
     *
     * @apiError (Error 404) NotFound Géofence non trouvée ou n'est pas un fork
     * @apiUse ErrorResponse
     */
    @GetMapping("/info/{geofenceId}/{geofenceType}")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<GeofenceForkInfoDTO>> getForkInfo(
            @PathVariable UUID geofenceId,
            @PathVariable String geofenceType) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    return forkService.getForkInfo(geofenceId, geofenceType, user);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(forkInfo -> {
                    if (forkInfo == null) {
                        return ResponseEntity.notFound().<GeofenceForkInfoDTO>build();
                    }
                    return ResponseEntity.ok(forkInfo);
                });
    }

    /**
     * @api {get} /geofence/fork/details/:geofenceId/:geofenceType Détails avec info fork
     * @apiName GetGeofenceWithForkInfo
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère les détails d'une géofence avec ses informations de fork
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence
     * @apiParam (Path) {String="c","p"} geofenceType Type de géofence
     *
     * @apiSuccess {String} id Identifiant de la géofence
     * @apiSuccess {String} title Titre de la géofence
     * @apiSuccess {String} [description] Description
     * @apiSuccess {String="circle","polygon"} type Type de géofence
     * @apiSuccess {Object} owner Propriétaire
     * @apiSuccess {Boolean} isOriginal Cette géofence a des forks
     * @apiSuccess {Boolean} isFork Cette géofence est un fork
     * @apiSuccess {Object} [forkInfo] Informations du fork (si isFork=true)
     * @apiSuccess {Number} forkCount Nombre de forks de cette géofence
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "id": "123e4567-e89b-12d3-a456-426614174000",
     *       "title": "Zone Bureau",
     *       "description": "Périmètre autorisé",
     *       "type": "circle",
     *       "owner": {
     *         "username": "jeandupont"
     *       },
     *       "isOriginal": true,
     *       "isFork": false,
     *       "forkInfo": null,
     *       "forkCount": 3
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiUse ErrorResponse
     */

    /**
     * @api {get} /geofence/fork/details/:geofenceId/:geofenceType Détails avec info fork
     * @apiName GetGeofenceWithForkInfoV2
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Récupère les détails d'une géofence avec ses informations de fork (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} geofenceId Identifiant de la géofence
     * @apiParam (Path) {String="c","p"} geofenceType Type de géofence
     *
     * @apiSuccess {String} id Identifiant de la géofence
     * @apiSuccess {String} title Titre de la géofence
     * @apiSuccess {String} [description] Description
     * @apiSuccess {String="circle","polygon"} type Type de géofence
     * @apiSuccess {Object} owner Propriétaire
     * @apiSuccess {Boolean} isOriginal Cette géofence a des forks
     * @apiSuccess {Boolean} isFork Cette géofence est un fork
     * @apiSuccess {Object} [forkInfo] Informations du fork (si isFork=true)
     * @apiSuccess {Number} forkCount Nombre de forks de cette géofence
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "id": "123e4567-e89b-12d3-a456-426614174000",
     *       "title": "Zone Bureau",
     *       "description": "Périmètre autorisé",
     *       "type": "circle",
     *       "owner": {
     *         "username": "jeandupont"
     *       },
     *       "isOriginal": true,
     *       "isFork": false,
     *       "forkInfo": null,
     *       "forkCount": 3
     *     }
     *
     * @apiError (Error 404) NotFound Géofence non trouvée
     * @apiUse ErrorResponse
     */
    @GetMapping("/details/{geofenceId}/{geofenceType}")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<GeofenceWithForkInfoDTO>> getGeofenceWithForkInfo(
            @PathVariable UUID geofenceId,
            @PathVariable String geofenceType) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName)
            .flatMap(username -> Mono.fromCallable(() -> {
                var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                return forkService.getGeofenceWithForkInfo(geofenceId, geofenceType, user);
            }))
            .subscribeOn(Schedulers.boundedElastic())
            .map(ResponseEntity::ok);
    }

    /**
     * @api {delete} /geofence/fork/:forkId Supprimer un fork
     * @apiName DeleteFork
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Supprimer un fork et la géofence associée
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} forkId Identifiant du fork
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 204 No Content
     *
     * @apiError (Error 404) NotFound Fork non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le créateur de ce fork
     * @apiUse ErrorResponse
     */

    /**
     * @api {delete} /geofence/fork/:forkId Supprimer un fork
     * @apiName DeleteForkV2
     * @apiGroup Geofence Fork
     * @apiVersion 2.0.0
     * @apiDescription Supprimer un fork et la géofence associée (version 2.0.0 avec améliorations)
     *
     * @apiUse UserPermission
     *
     * @apiParam (Path) {String} forkId Identifiant du fork
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 204 No Content
     *
     * @apiError (Error 404) NotFound Fork non trouvé
     * @apiError (Error 403) Forbidden Vous n'êtes pas le créateur de ce fork
     * @apiUse ErrorResponse
     */
    @DeleteMapping("/{forkId}")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<Void>> deleteFork(@PathVariable UUID forkId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() -> {
                    var user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                    forkService.deleteFork(forkId, user);
                    return ResponseEntity.noContent().<Void>build();
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

}