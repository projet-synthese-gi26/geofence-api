package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.PointDTO;
import ink.yowyob.geofence.dto.PolygonDTO;
import ink.yowyob.geofence.dto.request.CircleGeofenceZoneDTORequest;
import ink.yowyob.geofence.dto.request.GeofenceZoneDTORequest;
import ink.yowyob.geofence.dto.request.PolygonGeofenceZoneDTORequest;
import ink.yowyob.geofence.dto.response.CircleGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.GeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.MultipleGeofenceZoneDTOResponse;
import ink.yowyob.geofence.dto.response.PolygonGeofenceZoneDTOResponse;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.mapper.CircleDTOMapper;
import ink.yowyob.geofence.mapper.PolygonDTOMapper;
import ink.yowyob.geofence.model.CircleGeofenceZone;
import ink.yowyob.geofence.model.PolygonGeofenceZone;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.CircleGeofenceZoneRepository;
import ink.yowyob.geofence.repository.PolygonGeofenceZoneRepository;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.GeofenceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class GeofenceServiceImpl implements GeofenceService {
    private PolygonGeofenceZoneRepository polygonGeofenceZoneRepository;
    private CircleGeofenceZoneRepository circleGeofenceZoneRepository;
    private UserRepository userRepository;
    private final PolygonDTOMapper polygonDTOMapper;
    private final CircleDTOMapper circleDTOMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "geofenceDetail", key = "#zoneId + '_' + #type")
    public GeofenceZoneDTOResponse getGeofenceZone(UUID zoneId, String type, User user) {
        log.debug("Cache MISS: Fetching geofence {} type {}", zoneId, type);
        if (Objects.equals(type,"p")) {
            return getPolygonGeofenceZone(zoneId, user);
        } else if (Objects.equals(type,"c")) {
            return getCircleGeofenceZone(zoneId, user);
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "geofences", key = "'all'")
    public MultipleGeofenceZoneDTOResponse getGeofenceZones(User user) {
        log.debug("Cache MISS: Fetching all geofences");
        return new MultipleGeofenceZoneDTOResponse(getPolygonsGeofenceZone(user), getCirclesGeofenceZone(user));
    }

    @Override
    @CacheEvict(value = {"geofences", "sharedGeofences"}, allEntries = true)
    public GeofenceZoneDTOResponse createGeofenceZone(GeofenceZoneDTORequest geofenceZoneDTORequest, User user) {
        log.debug("Cache EVICT: Creating new geofence, clearing caches");
//        // Récupérer le nom d'utilisateur depuis le contexte de sécurité
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//
//        // Charger l'utilisateur complet depuis la base de données
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        switch (geofenceZoneDTORequest) {
            case PolygonGeofenceZoneDTORequest polygonGeofenceZoneDTORequest-> {
                return createPolygonGeofenceZone(polygonGeofenceZoneDTORequest, user);
            }
            case CircleGeofenceZoneDTORequest circleGeofenceZoneDTORequest-> {
                return createCircleGeofenceZone(circleGeofenceZoneDTORequest, user);
            }

            case null, default -> {
                return null;
            }
        }

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "geofences", key = "'user_' + #user.id")
    public MultipleGeofenceZoneDTOResponse getMyGeofenceZones(User user) {
        log.debug("Cache MISS: Fetching geofences for user {}", user.getUsername());
//        // Récupérer le nom d'utilisateur depuis le contexte de sécurité
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//
//        // Charger l'utilisateur complet depuis la base de données
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        return new MultipleGeofenceZoneDTOResponse(getMyPolygonsGeofenceZone(user),getMyCirclesGeofenceZone(user));
    }

    @Override
    @CacheEvict(value = {"geofences", "geofenceDetail", "sharedGeofences"}, allEntries = true)
    public GeofenceZoneDTOResponse editGeofenceZone(GeofenceZoneDTORequest geofenceZoneDTORequest, UUID zoneId, String type, User user) {
        log.debug("Cache EVICT: Editing geofence {}, clearing caches", zoneId);
        // Récupérer le nom d'utilisateur depuis le contexte de sécurité
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//
//        // Charger l'utilisateur complet depuis la base de données
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (Objects.equals(type, "p") && geofenceZoneDTORequest instanceof PolygonGeofenceZoneDTORequest) {
            return editPolygonGeofenceZone((PolygonGeofenceZoneDTORequest) geofenceZoneDTORequest, zoneId, user);
        } else if (Objects.equals(type, "c") && geofenceZoneDTORequest instanceof CircleGeofenceZoneDTORequest) {
            return editCircleGeofenceZone((CircleGeofenceZoneDTORequest) geofenceZoneDTORequest, zoneId, user);
        }
        return null;
    }

    @Override
    public void deleteGeofenceZone(UUID zoneId, String type, User user) {
        // Récupérer le nom d'utilisateur depuis le contexte de sécurité
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//
//        // Charger l'utilisateur complet depuis la base de données
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        if (Objects.equals(type,"p")) {
            deletePolygonGeofenceZone(zoneId, user);
        } else if (Objects.equals(type,"c")) {
            deleteCircleGeofenceZone(zoneId, user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PolygonGeofenceZoneDTOResponse getPolygonGeofenceZone(UUID Id, User user) {
        PolygonGeofenceZone polygonGeofenceZone = polygonGeofenceZoneRepository.findById(Id).orElseThrow();
        assertCanReadWithinTenant(user, polygonGeofenceZone.getUser());
        return polygonDTOMapper.apply(polygonGeofenceZone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolygonGeofenceZoneDTOResponse> getPolygonsGeofenceZone(User user) {
        if (isAdminOrManager(user)) {
            return polygonGeofenceZoneRepository.findByUser_Organization_Id(user.getOrganization().getId())
                    .stream()
                    .map(polygonDTOMapper)
                    .collect(Collectors.toList());
        }
        return polygonGeofenceZoneRepository.findByUser(user)
                .stream()
                .map(polygonDTOMapper)
                .collect(Collectors.toList());
    }

    @Override
    public PolygonGeofenceZoneDTOResponse createPolygonGeofenceZone(PolygonGeofenceZoneDTORequest geofenceZoneDTORequest, User user) {
        PolygonGeofenceZone polygonGeofenceZone = new PolygonGeofenceZone();
        polygonGeofenceZone.setUser(user);
        polygonGeofenceZone.setPolygon(convertToPolygon(geofenceZoneDTORequest.polygon()));
        polygonGeofenceZone.setDescription(geofenceZoneDTORequest.description());
        polygonGeofenceZone.setTitle(geofenceZoneDTORequest.title());
        
        // Propriétés intelligentes
        polygonGeofenceZone.setIsTemporalEnabled(geofenceZoneDTORequest.isTemporalEnabled());
        if (geofenceZoneDTORequest.startTime() != null) {
            polygonGeofenceZone.setStartTime(java.time.LocalTime.parse(geofenceZoneDTORequest.startTime()));
        }
        if (geofenceZoneDTORequest.endTime() != null) {
            polygonGeofenceZone.setEndTime(java.time.LocalTime.parse(geofenceZoneDTORequest.endTime()));
        }
        if (geofenceZoneDTORequest.activeDays() != null) {
            polygonGeofenceZone.setActiveDays(java.util.Arrays.stream(geofenceZoneDTORequest.activeDays())
                .map(day -> java.time.DayOfWeek.valueOf(day))
                .collect(java.util.stream.Collectors.toSet()));
        }
        polygonGeofenceZone.setIsConditionalEnabled(geofenceZoneDTORequest.isConditionalEnabled());
        polygonGeofenceZone.setMaxSpeed(geofenceZoneDTORequest.maxSpeed());
        polygonGeofenceZone.setMaxDwellTime(geofenceZoneDTORequest.maxDwellTime());
        polygonGeofenceZone.setMinDwellTime(geofenceZoneDTORequest.minDwellTime());

        return polygonDTOMapper.apply(polygonGeofenceZoneRepository.save(polygonGeofenceZone));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolygonGeofenceZoneDTOResponse> getMyPolygonsGeofenceZone(User user) {
        return polygonGeofenceZoneRepository.findByUser(user)
                .stream()
                .map(polygonDTOMapper)
                .collect(Collectors.toList());
    }

    @Override
    public PolygonGeofenceZoneDTOResponse editPolygonGeofenceZone(PolygonGeofenceZoneDTORequest geofenceZoneDTORequest, UUID zoneId, User user) {
        PolygonGeofenceZone polygonGeofenceZone = polygonGeofenceZoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalStateException("polygon zone with id " + zoneId + " does not exist"));

        assertSameOrganization(user, polygonGeofenceZone.getUser());

        // Vérification des permissions
        if (!Objects.equals(polygonGeofenceZone.getUser().getUuid(), user.getUuid()) &&
                !Objects.equals(user.getRole().getName(), UserRole.ADMIN)) {
            throw new BadCredentialsException("the user " + user.getUsername() + " does not have permission to edit this zone");
        }

        // Mise à jour des champs
        if (geofenceZoneDTORequest.polygon() != null) {
            polygonGeofenceZone.setPolygon(convertToPolygon(geofenceZoneDTORequest.polygon()));
        }

        if (geofenceZoneDTORequest.description() != null) {
            polygonGeofenceZone.setDescription(geofenceZoneDTORequest.description());
        }

        if (geofenceZoneDTORequest.title() != null) {
            polygonGeofenceZone.setTitle(geofenceZoneDTORequest.title());
        }
        
        // Mise à jour des propriétés intelligentes
        if (geofenceZoneDTORequest.isTemporalEnabled() != null) {
            polygonGeofenceZone.setIsTemporalEnabled(geofenceZoneDTORequest.isTemporalEnabled());
        }
        if (geofenceZoneDTORequest.startTime() != null) {
            polygonGeofenceZone.setStartTime(java.time.LocalTime.parse(geofenceZoneDTORequest.startTime()));
        }
        if (geofenceZoneDTORequest.endTime() != null) {
            polygonGeofenceZone.setEndTime(java.time.LocalTime.parse(geofenceZoneDTORequest.endTime()));
        }
        if (geofenceZoneDTORequest.activeDays() != null) {
            polygonGeofenceZone.setActiveDays(java.util.Arrays.stream(geofenceZoneDTORequest.activeDays())
                .map(day -> java.time.DayOfWeek.valueOf(day))
                .collect(java.util.stream.Collectors.toSet()));
        }
        if (geofenceZoneDTORequest.isConditionalEnabled() != null) {
            polygonGeofenceZone.setIsConditionalEnabled(geofenceZoneDTORequest.isConditionalEnabled());
        }
        if (geofenceZoneDTORequest.maxSpeed() != null) {
            polygonGeofenceZone.setMaxSpeed(geofenceZoneDTORequest.maxSpeed());
        }
        if (geofenceZoneDTORequest.maxDwellTime() != null) {
            polygonGeofenceZone.setMaxDwellTime(geofenceZoneDTORequest.maxDwellTime());
        }
        if (geofenceZoneDTORequest.minDwellTime() != null) {
            polygonGeofenceZone.setMinDwellTime(geofenceZoneDTORequest.minDwellTime());
        }

        // Sauvegarde et retour de la réponse
        return polygonDTOMapper.apply(polygonGeofenceZoneRepository.save(polygonGeofenceZone));
    }

    @Override
    public void deletePolygonGeofenceZone(UUID zoneId, User user) {
        PolygonGeofenceZone polygonGeofenceZone = polygonGeofenceZoneRepository.findById(zoneId).orElseThrow(() ->new IllegalStateException("polygon zone with id "+zoneId+" does not exist"));
        assertSameOrganization(user, polygonGeofenceZone.getUser());
        if(!Objects.equals(polygonGeofenceZone.getUser().getUuid(), user.getUuid()) && !Objects.equals(user.getRole().getName(), UserRole.ADMIN)){
            throw new BadCredentialsException("the user "+ user.getUsername()+"does not have permission to delete this zone");
        }
        polygonGeofenceZoneRepository.deleteById(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public CircleGeofenceZoneDTOResponse getCircleGeofenceZone(UUID Id, User user) {
        CircleGeofenceZone circleGeofenceZone = circleGeofenceZoneRepository.findById(Id).orElseThrow();
        assertCanReadWithinTenant(user, circleGeofenceZone.getUser());
        return circleDTOMapper.apply(circleGeofenceZone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleGeofenceZoneDTOResponse> getCirclesGeofenceZone(User user) {
        if (isAdminOrManager(user)) {
            return circleGeofenceZoneRepository.findByUser_Organization_Id(user.getOrganization().getId())
                    .stream()
                    .map(circleDTOMapper)
                    .collect(Collectors.toList());
        }
        return circleGeofenceZoneRepository.findByUser(user)
                .stream()
                .map(circleDTOMapper)
                .collect(Collectors.toList());
    }

    @Override
    public CircleGeofenceZoneDTOResponse createCircleGeofenceZone(CircleGeofenceZoneDTORequest geofenceZoneDTORequest, User user) {
        CircleGeofenceZone circleGeofenceZone = new CircleGeofenceZone();
        circleGeofenceZone.setUser(user);
        circleGeofenceZone.setDescription(geofenceZoneDTORequest.description());
        circleGeofenceZone.setTitle(geofenceZoneDTORequest.title());
        circleGeofenceZone.setRadius(geofenceZoneDTORequest.radius());
        circleGeofenceZone.setCenter(convertToPoint(geofenceZoneDTORequest.center()));
        
        // Propriétés intelligentes
        circleGeofenceZone.setIsTemporalEnabled(geofenceZoneDTORequest.isTemporalEnabled());
        if (geofenceZoneDTORequest.startTime() != null) {
            circleGeofenceZone.setStartTime(java.time.LocalTime.parse(geofenceZoneDTORequest.startTime()));
        }
        if (geofenceZoneDTORequest.endTime() != null) {
            circleGeofenceZone.setEndTime(java.time.LocalTime.parse(geofenceZoneDTORequest.endTime()));
        }
        if (geofenceZoneDTORequest.activeDays() != null) {
            circleGeofenceZone.setActiveDays(java.util.Arrays.stream(geofenceZoneDTORequest.activeDays())
                .map(day -> java.time.DayOfWeek.valueOf(day))
                .collect(java.util.stream.Collectors.toSet()));
        }
        circleGeofenceZone.setIsConditionalEnabled(geofenceZoneDTORequest.isConditionalEnabled());
        circleGeofenceZone.setMaxSpeed(geofenceZoneDTORequest.maxSpeed());
        circleGeofenceZone.setMaxDwellTime(geofenceZoneDTORequest.maxDwellTime());
        circleGeofenceZone.setMinDwellTime(geofenceZoneDTORequest.minDwellTime());
        
        return circleDTOMapper.apply(circleGeofenceZoneRepository.save(circleGeofenceZone));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CircleGeofenceZoneDTOResponse> getMyCirclesGeofenceZone(User user) {
        return circleGeofenceZoneRepository.findByUser(user)
                .stream()
                .map(circleDTOMapper)
                .collect(Collectors.toList());
    }

    @Override
    public CircleGeofenceZoneDTOResponse editCircleGeofenceZone(CircleGeofenceZoneDTORequest geofenceZoneDTORequest, UUID zoneId, User user) {
        CircleGeofenceZone circleGeofenceZone = circleGeofenceZoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalStateException("circle zone with id " + zoneId + " does not exist"));

        assertSameOrganization(user, circleGeofenceZone.getUser());

        // Vérification des permissions
        if (!Objects.equals(circleGeofenceZone.getUser().getUuid(), user.getUuid()) &&
                !Objects.equals(user.getRole().getName(), UserRole.ADMIN)) {
            throw new BadCredentialsException("the user " + user.getUsername() + " does not have permission to edit this zone");
        }

        // Mise à jour des champs
        if (geofenceZoneDTORequest.center() != null) {
            circleGeofenceZone.setCenter(convertToPoint(geofenceZoneDTORequest.center()));
        }

        if (geofenceZoneDTORequest.radius() != null) {
            circleGeofenceZone.setRadius(geofenceZoneDTORequest.radius());
        }

        if (geofenceZoneDTORequest.description() != null) {
            circleGeofenceZone.setDescription(geofenceZoneDTORequest.description());
        }

        if (geofenceZoneDTORequest.title() != null) {
            circleGeofenceZone.setTitle(geofenceZoneDTORequest.title());
        }
        
        // Mise à jour des propriétés intelligentes
        if (geofenceZoneDTORequest.isTemporalEnabled() != null) {
            circleGeofenceZone.setIsTemporalEnabled(geofenceZoneDTORequest.isTemporalEnabled());
        }
        if (geofenceZoneDTORequest.startTime() != null) {
            circleGeofenceZone.setStartTime(java.time.LocalTime.parse(geofenceZoneDTORequest.startTime()));
        }
        if (geofenceZoneDTORequest.endTime() != null) {
            circleGeofenceZone.setEndTime(java.time.LocalTime.parse(geofenceZoneDTORequest.endTime()));
        }
        if (geofenceZoneDTORequest.activeDays() != null) {
            circleGeofenceZone.setActiveDays(java.util.Arrays.stream(geofenceZoneDTORequest.activeDays())
                .map(day -> java.time.DayOfWeek.valueOf(day))
                .collect(java.util.stream.Collectors.toSet()));
        }
        if (geofenceZoneDTORequest.isConditionalEnabled() != null) {
            circleGeofenceZone.setIsConditionalEnabled(geofenceZoneDTORequest.isConditionalEnabled());
        }
        if (geofenceZoneDTORequest.maxSpeed() != null) {
            circleGeofenceZone.setMaxSpeed(geofenceZoneDTORequest.maxSpeed());
        }
        if (geofenceZoneDTORequest.maxDwellTime() != null) {
            circleGeofenceZone.setMaxDwellTime(geofenceZoneDTORequest.maxDwellTime());
        }
        if (geofenceZoneDTORequest.minDwellTime() != null) {
            circleGeofenceZone.setMinDwellTime(geofenceZoneDTORequest.minDwellTime());
        }

        // Sauvegarde et retour de la réponse
        return circleDTOMapper.apply(circleGeofenceZoneRepository.save(circleGeofenceZone));
    }

    @Override
    public void deleteCircleGeofenceZone(UUID zoneId, User user) {
        CircleGeofenceZone circleGeofenceZone = circleGeofenceZoneRepository.findById(zoneId).orElseThrow(() ->new IllegalStateException("circle zone with id "+zoneId+" does not exist"));
        assertSameOrganization(user, circleGeofenceZone.getUser());
        if(!Objects.equals(circleGeofenceZone.getUser(), user) && !Objects.equals(user.getRole().getName(), UserRole.ADMIN)){
            throw new BadCredentialsException("the user "+ user.getUsername()+"does not have permission to delete this zone");
        }
        circleGeofenceZoneRepository.deleteById(zoneId);
    }

    @Override
    public Polygon convertToPolygon(PolygonDTO dto) {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

        // Vérification de l'existence de l'anneau principal
        if (dto.coordinates() == null || dto.coordinates().isEmpty()) {
            throw new IllegalArgumentException("Le polygone doit contenir au moins un anneau.");
        }

        List<List<Double>> ring = dto.coordinates().getFirst();

        if (ring.size() < 4) {
            throw new IllegalArgumentException("Un polygone valide doit avoir au moins 4 points (3 + retour au départ).");
        }

        List<Double> firstPoint = ring.getFirst();
        List<Double> lastPoint = ring.getLast();

        if (!firstPoint.equals(lastPoint)) {
            throw new IllegalArgumentException("Le polygone doit être fermé (le premier et dernier point doivent être identiques).");
        }

        // Conversion de l'anneau extérieur
        List<Coordinate> shellCoords = ring.stream()
                .map(coord -> new Coordinate(coord.getFirst(), coord.get(1)))
                .toList();
        LinearRing shell = factory.createLinearRing(shellCoords.toArray(new Coordinate[0]));
        Polygon polygon = factory.createPolygon(shell);

        polygon.setSRID(4326);
        return factory.createPolygon(shell);
    }

    @Override
    public Point convertToPoint(PointDTO dto) {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

        List<Double> coords = dto.coordinates();
        if (coords == null || coords.size() != 2) {
            throw new IllegalArgumentException("PointDTO must contain exactly 2 coordinates: [longitude, latitude]");
        }

        double longitude = coords.get(0);
        double latitude = coords.get(1);

        return factory.createPoint(new Coordinate(longitude, latitude));
    }

    private void assertSameOrganization(User actor, User resourceOwner) {
        if (actor.getOrganization() == null || resourceOwner.getOrganization() == null ||
                !Objects.equals(actor.getOrganization().getId(), resourceOwner.getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant access denied");
        }
    }

    private void assertCanReadWithinTenant(User actor, User resourceOwner) {
        assertSameOrganization(actor, resourceOwner);
        if (!Objects.equals(resourceOwner.getUuid(), actor.getUuid()) && !isAdminOrManager(actor)) {
            throw new BadCredentialsException("the user " + actor.getUsername() + " does not have permission to view this zone");
        }
    }

    private boolean isAdminOrManager(User user) {
        return Objects.equals(user.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(user.getRole().getName(), UserRole.MANAGER);
    }

}
