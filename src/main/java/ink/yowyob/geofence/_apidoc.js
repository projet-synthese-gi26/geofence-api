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

/**
 * @apiDefine VehicleObject
 * @apiSuccess {String} id Identifiant unique du véhicule
 * @apiSuccess {String} brand Marque du véhicule
 * @apiSuccess {String} model Modèle du véhicule
 * @apiSuccess {String} licensePlate Plaque d'immatriculation
 * @apiSuccess {String} [description] Description du véhicule
 * @apiSuccess {String} [imageUrl] URL de l'image du véhicule
 * @apiSuccess {Object} user Propriétaire du véhicule
 * @apiSuccess {Object[]} geofenceZones Zones de géofence associées
 */

/**
 * @apiDefine AlertObject
 * @apiSuccess {String} id Identifiant unique de l'alerte
 * @apiSuccess {String="ZONE_EXIT","ZONE_ENTER","SPEED_LIMIT","BATTERY_LOW","SYSTEM_ERROR"} type Type d'alerte
 * @apiSuccess {String} timestamp Date et heure de l'alerte (ISO 8601)
 * @apiSuccess {String} message Message de l'alerte
 * @apiSuccess {Object} [location] Localisation de l'alerte
 * @apiSuccess {Object} vehicle Véhicule concerné
 * @apiSuccess {Object} [geofenceZone] Zone de géofence concernée
 */

/**
 * @apiDefine GeofenceObject
 * @apiSuccess {String} id Identifiant unique de la zone
 * @apiSuccess {String} title Titre de la zone
 * @apiSuccess {String} [description] Description de la zone
 * @apiSuccess {String="circle","polygon"} type Type de géofence
 * @apiSuccess {Object} user Propriétaire de la zone
 * @apiSuccess {Object[]} vehicles Véhicules associés à cette zone
 */