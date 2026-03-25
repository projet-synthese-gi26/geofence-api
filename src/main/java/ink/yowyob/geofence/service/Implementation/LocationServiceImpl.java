package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.AlertTypeEnum;
import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.request.CreateApiKeyRequest;
import ink.yowyob.geofence.dto.request.LocationUpdateRequest;
import ink.yowyob.geofence.dto.response.*;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.exception.ResourceNotFoundException;
import ink.yowyob.geofence.mapper.LocationDTOMapper;
import ink.yowyob.geofence.mapper.VehicleApiKeyDTOMapper;
import ink.yowyob.geofence.model.*;
import ink.yowyob.geofence.repository.LocationRepository;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.repository.VehicleApiKeyRepository;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.AlertService;
import ink.yowyob.geofence.service.LocationService;
import ink.yowyob.geofence.service.RealTimeService;
import ink.yowyob.geofence.dto.response.SimpleVehicleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final RealTimeService realTimeService;
    private final LocationDTOMapper locationMapper;
    private final VehicleApiKeyDTOMapper apiKeyMapper;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public LocationUpdateResponse updateLocationFromDevice(String apiKey, LocationUpdateRequest request) {
        // Valider la clé API
        VehicleApiKey vehicleApiKey = apiKeyRepository.findByApiKeyAndIsActiveTrue(apiKey)
                .orElseThrow(() -> new BadCredentialsException("Clé API invalide ou inactive"));

        if (!vehicleApiKey.isValid()) {
            throw new BadCredentialsException("Clé API expirée");
        }

        Vehicle vehicle = vehicleApiKey.getVehicle();

        // Créer la nouvelle position
        Point position = geometryFactory.createPoint(
                new Coordinate(request.longitude(), request.latitude())
        );

        Location location = new Location();
        location.setVehicle(vehicle);
        location.setPosition(position);
        location.setSpeed(request.speed());
        location.setHeading(request.heading());
        location.setAltitude(request.altitude());
        location.setAccuracy(request.accuracy());
        location.setSource(request.source());

        // Sauvegarder la position
        Location savedLocation = locationRepository.save(location);
        locationRepository.flush(); // Forcer la sauvegarde immédiate en base

        // Mettre à jour la dernière utilisation de la clé
        vehicleApiKey.updateLastUsed();
        apiKeyRepository.save(vehicleApiKey);

        // Vérifier les géofences et générer les alertes de façon asynchrone
        List<AlertDTO> alerts = new ArrayList<>();
        try {
            alerts = checkGeofencesAndGenerateAlerts(vehicle, savedLocation);
        } catch (Exception e) {
            log.warn("Erreur lors de la génération d'alertes pour le véhicule {}: {}", vehicle.getLicensePlate(), e.getMessage());
            // Continuer sans alertes - l'important est que la position soit sauvegardée
        }

        // Diffuser la mise à jour en temps réel
        try {
            LocationDTO locationDTO = locationMapper.apply(savedLocation);
            SimpleVehicleDTO vehicleDTO = new SimpleVehicleDTO(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                vehicle.getImageUrl()
            );
            realTimeService.broadcastLocationUpdate(locationDTO, vehicleDTO, vehicle.getUser());
            
            // Diffuser les alertes si il y en a
            for (AlertDTO alert : alerts) {
                realTimeService.broadcastAlert(alert, vehicle.getUser());
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la diffusion temps réel pour le véhicule {}: {}", vehicle.getLicensePlate(), e.getMessage());
        }

        return new LocationUpdateResponse(
                true,
                "Position mise à jour avec succès",
                locationMapper.apply(savedLocation),
                alerts
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LocationListResponse getLocationHistory(UUID vehicleId, int page, int size, User user) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé"));

        assertSameOrganization(user, vehicle.getUser());

        // Vérifier les permissions
        boolean isAuthorized = Objects.equals(vehicle.getUser().getUuid(), user.getUuid()) ||
                user.getRole().getName() == UserRole.ADMIN ||
                user.getRole().getName() == UserRole.MANAGER;

        if (!isAuthorized) {
            throw new BadCredentialsException("Accès refusé à l'historique de ce véhicule");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Location> locationPage = locationRepository.findByVehicleOrderByTimestampDescWithVehicle(vehicle, pageable);

        List<LocationDTO> locationDTOs = locationPage.getContent().stream()
                .map(locationMapper)
                .toList();

        return new LocationListResponse(
                locationDTOs,
                locationDTOs.size(),
                page,
                size,
                locationPage.getTotalElements(),
                locationPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDTO getLatestLocation(UUID vehicleId, User user) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé"));

        assertSameOrganization(user, vehicle.getUser());

        // Vérifier les permissions
        boolean isAuthorized = Objects.equals(vehicle.getUser().getUuid(), user.getUuid()) ||
                user.getRole().getName() == UserRole.ADMIN ||
                user.getRole().getName() == UserRole.MANAGER;

        if (!isAuthorized) {
            throw new BadCredentialsException("Accès refusé à la position de ce véhicule");
        }

        Pageable latestPageable = PageRequest.of(0, 1);
        List<Location> latestLocations = locationRepository.findByVehicleOrderByTimestampDescWithVehicleList(vehicle, latestPageable);
        
        if (latestLocations.isEmpty()) {
            throw new ResourceNotFoundException("Aucune position trouvée pour ce véhicule");
        }
        
        Location latestLocation = latestLocations.get(0);

        return locationMapper.apply(latestLocation);
    }

    @Override
    @Transactional
    public void deleteLocation(UUID locationId, User user) {

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Position non trouvée"));

        assertSameOrganization(user, location.getVehicle().getUser());

        // Vérifier les permissions
        boolean isAuthorized = Objects.equals(location.getVehicle().getUser().getUuid(), user.getUuid()) ||
                user.getRole().getName() == UserRole.ADMIN;

        if (!isAuthorized) {
            throw new BadCredentialsException("Accès refusé pour supprimer cette position");
        }

        locationRepository.delete(location);
    }

    @Override
    @Transactional
    public VehicleApiKeyDTO generateApiKey(CreateApiKeyRequest request, User user) {

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé"));

        assertSameOrganization(user, vehicle.getUser());

        // Vérifier les permissions
        boolean isAuthorized = Objects.equals(vehicle.getUser().getUuid(), user.getUuid()) ||
                user.getRole().getName() == UserRole.ADMIN;

        if (!isAuthorized) {
            throw new BadCredentialsException("Accès refusé pour ce véhicule");
        }

        // Désactiver l'ancienne clé si elle existe
        apiKeyRepository.findByVehicle(vehicle).ifPresent(oldKey -> {
            oldKey.setActive(false);
            apiKeyRepository.save(oldKey);
        });

        // Générer une nouvelle clé
        String apiKey = generateSecureApiKey(vehicle);

        VehicleApiKey vehicleApiKey = VehicleApiKey.builder()
                .apiKey(apiKey)
                .vehicle(vehicle)
                .isActive(true)
                .expiresAt(request.expiresAt())
                .build();

        VehicleApiKey savedApiKey = apiKeyRepository.save(vehicleApiKey);


        return apiKeyMapper.apply(savedApiKey);
    }

    @Override
    public VehicleApiKeyDTO getApiKeyForVehicle(UUID vehicleId, User user) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé"));

        assertSameOrganization(user, vehicle.getUser());

        // Vérifier les permissions
        boolean isAuthorized = Objects.equals(vehicle.getUser().getUuid(), user.getUuid()) ||
                user.getRole().getName() == UserRole.ADMIN;

        if (!isAuthorized) {
            throw new BadCredentialsException("Accès refusé pour ce véhicule");
        }

        VehicleApiKey apiKey = apiKeyRepository.findByVehicle(vehicle)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune clé API trouvée pour ce véhicule"));

        return apiKeyMapper.apply(apiKey);
    }

    @Override
    @Transactional
    public void revokeApiKey(UUID vehicleId, User user) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé"));

        assertSameOrganization(user, vehicle.getUser());

        // Vérifier les permissions
        boolean isAuthorized = Objects.equals(vehicle.getUser().getUuid(), user.getUuid()) ||
                user.getRole().getName() == UserRole.ADMIN;

        if (!isAuthorized) {
            throw new BadCredentialsException("Accès refusé pour ce véhicule");
        }

        apiKeyRepository.deactivateByVehicleId(vehicleId);

    }

    @Override
    public ApiKeyListResponse getMyApiKeys(User user) {

        List<Vehicle> userVehicles = vehicleRepository.findByUser(user);
        List<VehicleApiKeyDTO> apiKeys = new ArrayList<>();

        for (Vehicle vehicle : userVehicles) {
            apiKeyRepository.findByVehicle(vehicle)
                    .ifPresent(apiKey -> apiKeys.add(apiKeyMapper.apply(apiKey)));
        }

        return new ApiKeyListResponse(apiKeys, apiKeys.size());
    }

    // Méthodes privées

    private String generateSecureApiKey(Vehicle vehicle) {
        try {
            // Utiliser l'ID du véhicule, timestamp et random pour l'unicité
            String data = vehicle.getId().toString() +
                    System.currentTimeMillis() +
                    secureRandom.nextLong();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Convertir en hexadécimal et prendre les 64 premiers caractères
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "vk_" + hexString.toString().substring(0, 60); // Préfixe + 60 chars
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération de la clé API", e);
        }
    }

    private List<AlertDTO> checkGeofencesAndGenerateAlerts(Vehicle vehicle, Location currentLocation) {
        List<AlertDTO> alerts = new ArrayList<>();
        LocalDateTime currentTime = currentLocation.getTimestamp();

        // Récupérer la position précédente du véhicule (avant la position actuelle)
        Optional<Location> previousLocationOpt = locationRepository
                .findTopByVehicleAndTimestampBeforeOrderByTimestampDesc(vehicle, currentTime);

        // Récupérer les zones de géofence associées au véhicule
        for (GeofenceZone zone : vehicle.getGeofenceZones()) {
            // Vérifier si la zone est active à ce moment (géofence intelligente)
            if (!zone.isActiveAtTime(currentTime)) {
                // Zone inactive, vérifier si le véhicule y est quand même
                if (isPointInsideGeofence(currentLocation.getPosition(), zone)) {
                    AlertDTO alert = alertService.createAlert(
                            vehicle, zone, AlertTypeEnum.ZONE_TEMPORAL_VIOLATION, currentLocation,
                            "Véhicule " + vehicle.getLicensePlate() + " dans zone " + zone.getTitle() + " inactive à cette heure"
                    );
                    alerts.add(alert);
                }
                continue; // Passer à la zone suivante
            }

            boolean isCurrentlyInside = isPointInsideGeofence(currentLocation.getPosition(), zone);

            // Si c'est la première position, générer seulement une alerte d'entrée si nécessaire
            if (previousLocationOpt.isEmpty()) {
                if (isCurrentlyInside) {
                    AlertDTO alert = alertService.createAlert(
                            vehicle, zone, AlertTypeEnum.ZONE_ENTER, currentLocation,
                            "Véhicule " + vehicle.getLicensePlate() + " est entré dans la zone " + zone.getTitle()
                    );
                    alerts.add(alert);
                    
                    // Vérifications intelligentes pour la première entrée
                    alerts.addAll(checkIntelligentGeofenceViolations(vehicle, zone, currentLocation, null));
                }
                continue;
            }

            // Vérifier l'état précédent
            Location previousLocation = previousLocationOpt.get();
            boolean wasPreviouslyInside = isPointInsideGeofence(previousLocation.getPosition(), zone);

            // Générer des alertes seulement s'il y a un changement d'état
            if (!wasPreviouslyInside && isCurrentlyInside) {
                // Le véhicule vient d'entrer dans la zone
                AlertDTO alert = alertService.createAlert(
                        vehicle, zone, AlertTypeEnum.ZONE_ENTER, currentLocation,
                        "Véhicule " + vehicle.getLicensePlate() + " est entré dans la zone " + zone.getTitle()
                );
                alerts.add(alert);
                log.info("ZONE_ENTER: Véhicule {} est entré dans la zone {}",
                        vehicle.getLicensePlate(), zone.getTitle());

            } else if (wasPreviouslyInside && !isCurrentlyInside) {
                // Le véhicule vient de sortir de la zone
                AlertDTO alert = alertService.createAlert(
                        vehicle, zone, AlertTypeEnum.ZONE_EXIT, currentLocation,
                        "Véhicule " + vehicle.getLicensePlate() + " est sorti de la zone " + zone.getTitle()
                );
                alerts.add(alert);
                log.info("ZONE_EXIT: Véhicule {} est sorti de la zone {}",
                        vehicle.getLicensePlate(), zone.getTitle());
                        
                // Vérifier le temps de séjour en sortant
                alerts.addAll(checkDwellTimeViolations(vehicle, zone, currentLocation, previousLocation));
            }
            
            // Vérifications intelligentes continues (vitesse, etc.)
            if (isCurrentlyInside) {
                alerts.addAll(checkIntelligentGeofenceViolations(vehicle, zone, currentLocation, previousLocation));
            }
        }

        return alerts;
    }

    private boolean isPointInsideGeofence(Point point, GeofenceZone zone) {
        if (zone instanceof CircleGeofenceZone circle) {
            // Distance entre le point et le centre du cercle
            double distance = point.distance(circle.getCenter());
            // Convertir le rayon en degrés (approximation)
            double radiusInDegrees = circle.getRadius() / 111000.0; // 1 degré ≈ 111 km
            return distance <= radiusInDegrees;
        } else if (zone instanceof PolygonGeofenceZone polygon) {
            // Utiliser la méthode contains de JTS
            return polygon.getPolygon().contains(point);
        }
        return false;
    }
    
    // Nouvelles méthodes pour géofence intelligente
    
    private List<AlertDTO> checkIntelligentGeofenceViolations(Vehicle vehicle, GeofenceZone zone, 
                                                              Location currentLocation, Location previousLocation) {
        List<AlertDTO> alerts = new ArrayList<>();
        
        // Vérification de vitesse
        if (zone.getIsConditionalEnabled() != null && zone.getIsConditionalEnabled() && zone.getMaxSpeed() != null && 
            currentLocation.getSpeed() != null && currentLocation.getSpeed() > zone.getMaxSpeed()) {
            
            AlertDTO alert = alertService.createAlert(
                    vehicle, zone, AlertTypeEnum.ZONE_SPEED_VIOLATION, currentLocation,
                    String.format("Véhicule %s dépasse la limite de vitesse (%.1f km/h > %.1f km/h) dans la zone %s",
                            vehicle.getLicensePlate(), currentLocation.getSpeed(), zone.getMaxSpeed(), zone.getTitle())
            );
            alerts.add(alert);
        }
        
        return alerts;
    }
    
    private List<AlertDTO> checkDwellTimeViolations(Vehicle vehicle, GeofenceZone zone, 
                                                    Location currentLocation, Location previousLocation) {
        List<AlertDTO> alerts = new ArrayList<>();
        
        if (zone.getIsConditionalEnabled() == null || !zone.getIsConditionalEnabled() || (zone.getMaxDwellTime() == null && zone.getMinDwellTime() == null)) {
            return alerts;
        }
        
        try {
            // Calculer le temps de séjour approximatif
            Optional<Location> firstEntryOpt = locationRepository
                    .findFirstEntryIntoGeofence(vehicle, zone.getId(), currentLocation.getTimestamp());
            
            if (firstEntryOpt.isPresent()) {
                Location firstEntry = firstEntryOpt.get();
                Duration dwellTime = Duration.between(firstEntry.getTimestamp(), currentLocation.getTimestamp());
                long dwellMinutes = dwellTime.toMinutes();
                
                // Vérifier temps maximum
                if (zone.getMaxDwellTime() != null && dwellMinutes > zone.getMaxDwellTime()) {
                    AlertDTO alert = alertService.createAlert(
                            vehicle, zone, AlertTypeEnum.ZONE_DWELL_TIME_EXCEEDED, currentLocation,
                            String.format("Véhicule %s a dépassé le temps de séjour maximum (%d min > %d min) dans la zone %s",
                                    vehicle.getLicensePlate(), dwellMinutes, zone.getMaxDwellTime(), zone.getTitle())
                    );
                    alerts.add(alert);
                }
                
                // Vérifier temps minimum (seulement lors de la sortie)
                if (zone.getMinDwellTime() != null && dwellMinutes < zone.getMinDwellTime()) {
                    AlertDTO alert = alertService.createAlert(
                            vehicle, zone, AlertTypeEnum.ZONE_DWELL_TIME_INSUFFICIENT, currentLocation,
                            String.format("Véhicule %s n'a pas respecté le temps de séjour minimum (%d min < %d min) dans la zone %s",
                                    vehicle.getLicensePlate(), dwellMinutes, zone.getMinDwellTime(), zone.getTitle())
                    );
                    alerts.add(alert);
                }
            }
        } catch (Exception e) {
            log.warn("Erreur lors du calcul du temps de séjour pour le véhicule {} dans la zone {}: {}", 
                    vehicle.getLicensePlate(), zone.getTitle(), e.getMessage());
        }
        
        return alerts;
    }

    private void assertSameOrganization(User actor, User resourceOwner) {
        if (actor.getOrganization() == null || resourceOwner.getOrganization() == null ||
                !Objects.equals(actor.getOrganization().getId(), resourceOwner.getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant access denied");
        }
    }
}