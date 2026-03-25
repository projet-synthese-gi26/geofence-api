package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.CreateApiKeyRequest;
import ink.yowyob.geofence.dto.request.LocationUpdateRequest;
import ink.yowyob.geofence.dto.response.*;
import ink.yowyob.geofence.model.User;

import java.util.UUID;

public interface LocationService {

    /**
     * Met à jour la position d'un véhicule depuis un appareil mobile (non protégé)
     */
    LocationUpdateResponse updateLocationFromDevice(String apiKey, LocationUpdateRequest request);

    /**
     * Récupère l'historique des positions d'un véhicule
     */
    LocationListResponse getLocationHistory(UUID vehicleId, int page, int size, User user);

    /**
     * Récupère la dernière position d'un véhicule
     */
    LocationDTO getLatestLocation(UUID vehicleId, User user);

    /**
     * Supprime une position
     */
    void deleteLocation(UUID locationId, User user);

    /**
     * Génère une nouvelle clé API pour un véhicule
     */
    VehicleApiKeyDTO generateApiKey(CreateApiKeyRequest request, User user);

    /**
     * Récupère la clé API d'un véhicule
     */
    VehicleApiKeyDTO getApiKeyForVehicle(UUID vehicleId, User user);

    /**
     * Révoque/désactive la clé API d'un véhicule
     */
    void revokeApiKey(UUID vehicleId, User user);

    /**
     * Récupère toutes les clés API de l'utilisateur connecté
     */
    ApiKeyListResponse getMyApiKeys(User user);
}