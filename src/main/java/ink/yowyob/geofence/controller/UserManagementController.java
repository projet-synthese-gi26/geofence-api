package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.UpdateUserRoleRequest;
import ink.yowyob.geofence.dto.response.UserDTO;
import ink.yowyob.geofence.dto.response.UserListResponse;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.Implementation.UserManagementServiceImpl;
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

/**
 * @apiDefine UserObject
 * @apiSuccess {String} uuid Identifiant unique de l'utilisateur
 * @apiSuccess {String} username Nom d'utilisateur
 * @apiSuccess {String} firstname Prénom
 * @apiSuccess {String} lastname Nom de famille
 * @apiSuccess {String} phoneNumber Numéro de téléphone
 * @apiSuccess {String} email Adresse email
 * @apiSuccess {String} DOB Date de naissance (YYYY-MM-DD)
 * @apiSuccess {String="USER","MANAGER","ADMIN"} Role Rôle de l'utilisateur
 */

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserManagementController {
    private final UserManagementServiceImpl userManagementService;
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
     * @api {get} /admin/users Récupérer tous les utilisateurs
     * @apiName GetAllUsers
     * @apiGroup User Management
     * @apiVersion 2.0.0
     * @apiDescription Récupère la liste de tous les utilisateurs (accès admin uniquement)
     *
     * @apiUse AdminPermission
     *
     * @apiQuery {Number{0-}} [page=0] Numéro de page
     * @apiQuery {Number{1-100}} [size=20] Nombre d'éléments par page
     *
     * @apiSuccess {Object[]} users Liste des utilisateurs
     * @apiSuccess {Number} totalItems Nombre total d'utilisateurs
     * @apiUse UserObject
     *
     * @apiError (Error 403) Forbidden Accès refusé - Droits admin requis
     * @apiUse ErrorResponse
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<UserListResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> userManagementService.getAllUsers(page, size, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {get} /admin/users/:userId Récupérer un utilisateur
     * @apiName GetUserById
     * @apiGroup User Management
     * @apiVersion 2.0.0
     * @apiDescription Récupère les détails d'un utilisateur spécifique (accès admin uniquement)
     *
     * @apiUse AdminPermission
     *
     * @apiParam (Path) {String} userId Identifiant de l'utilisateur
     *
     * @apiSuccess {Object} user Données de l'utilisateur
     * @apiUse UserObject
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *       "uuid": "123e4567-e89b-12d3-a456-426614174000",
     *       "username": "jeandupont",
     *       "firstname": "Jean",
     *       "lastname": "Dupont",
     *       "phoneNumber": "+237123456789",
     *       "email": "jean.dupont@example.com",
     *       "DOB": "1990-01-15",
     *       "Role": "USER"
     *     }
     *
     * @apiError (Error 404) NotFound Utilisateur non trouvé
     * @apiError (Error 403) Forbidden Accès refusé - Droits admin requis
     * @apiUse ErrorResponse
     */
    @GetMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<UserDTO>> getUserById(@PathVariable UUID userId) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> userManagementService.getUserById(userId, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {put} /admin/users/:userId/role Modifier le rôle d'un utilisateur
     * @apiName UpdateUserRole
     * @apiGroup User Management
     * @apiVersion 2.0.0
     * @apiDescription Modifie le rôle d'un utilisateur (accès admin uniquement)
     *
     * @apiUse AdminPermission
     *
     * @apiParam (Path) {String} userId Identifiant de l'utilisateur
     *
     * @apiBody {String="USER","MANAGER","ADMIN"} role Nouveau rôle (requis)
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *       "role": "MANAGER"
     *     }
     *
     * @apiSuccess {Object} user Utilisateur mis à jour
     * @apiUse UserObject
     *
     * @apiError (Error 404) NotFound Utilisateur non trouvé
     * @apiError (Error 403) Forbidden Accès refusé - Droits admin requis
     * @apiError (Error 400) BadRequest Rôle invalide
     * @apiUse ErrorResponse
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/users/{userId}/role")
    public Mono<ResponseEntity<UserDTO>> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody UpdateUserRoleRequest request
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> userManagementService.updateUserRole(userId, request, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    /**
     * @api {delete} /admin/users/:userId Supprimer un utilisateur
     * @apiName DeleteUser
     * @apiGroup User Management
     * @apiVersion 2.0.0
     * @apiDescription Supprimer définitivement un utilisateur (accès admin uniquement)
     *
     * @apiUse AdminPermission
     *
     * @apiParam (Path) {String} userId Identifiant de l'utilisateur
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 204 No Content
     *
     * @apiError (Error 404) NotFound Utilisateur non trouvé
     * @apiError (Error 403) Forbidden Accès refusé - Droits admin requis
     * @apiUse ErrorResponse
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{userId}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable UUID userId) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                    userManagementService.deleteUser(userId, user);
                    return ResponseEntity.noContent().<Void>build();
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * @api {get} /users/for-sharing Utilisateurs pour partage
     * @apiName GetUsersForSharing
     * @apiGroup User Management
     * @apiVersion 2.0.0
     * @apiDescription Récupère la liste des utilisateurs pour le partage de géofences
     *
     * @apiUse UserPermission
     *
     * @apiSuccess {Object[]} users Liste des utilisateurs (sauf l'utilisateur connecté)
     * @apiSuccess {Number} totalItems Nombre total d'utilisateurs
     * @apiUse UserObject
     *
     * @apiUse ErrorResponse
     */
    @GetMapping("/users/for-sharing")
    public Mono<ResponseEntity<UserListResponse>> getUsersForSharing() {
        return getCurrentUser()
                .flatMap(user ->Mono.fromCallable(() -> userManagementService.getUsersForSharing(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}