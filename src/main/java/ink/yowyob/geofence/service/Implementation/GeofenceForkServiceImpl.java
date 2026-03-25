package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.dto.request.ForkGeofenceRequest;
import ink.yowyob.geofence.dto.response.GeofenceWithForkInfoDTO;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.exception.ResourceNotFoundException;
import ink.yowyob.geofence.dto.response.GeofenceForkListResponse;
import ink.yowyob.geofence.mapper.GeofenceForkDTOMapper;
import ink.yowyob.geofence.mapper.UserDTOMapper;
import ink.yowyob.geofence.dto.response.GeofenceForkDTO;
import ink.yowyob.geofence.dto.response.GeofenceForkInfoDTO;
import ink.yowyob.geofence.model.*;
import ink.yowyob.geofence.repository.*;
import ink.yowyob.geofence.service.GeofenceForkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeofenceForkServiceImpl implements GeofenceForkService {

    private final GeofenceForkRepository forkRepository;
    private final CircleGeofenceZoneRepository circleRepository;
    private final PolygonGeofenceZoneRepository polygonRepository;
    private final GeofenceShareRepository shareRepository;
    private final UserRepository userRepository;
    private final GeofenceForkDTOMapper forkMapper;
    private final UserDTOMapper userMapper;

    @Override
    @Transactional
    public GeofenceForkDTO forkGeofence(ForkGeofenceRequest request, User user) {

        // Récupérer la géofence originale
        GeofenceZone originalGeofence = getGeofenceZone(request.geofenceId(), request.geofenceType());

        // Vérifier que l'utilisateur a accès à cette géofence (propriétaire ou partagée)
        if (!hasAccessToGeofence(originalGeofence, user)) {
            throw new BadCredentialsException("Vous n'avez pas accès à cette géofence");
        }

        // Créer la copie de la géofence
        GeofenceZone forkedGeofence = createGeofenceCopy(originalGeofence, request, user);

        // Créer l'enregistrement de fork
        GeofenceFork fork = new GeofenceFork(
                originalGeofence,
                forkedGeofence,
                user,
                request.forkReason()
        );

        GeofenceFork savedFork = forkRepository.save(fork);

        return forkMapper.apply(savedFork);
    }

    @Override
    public GeofenceForkListResponse getForksOfGeofence(UUID geofenceId, User user) {
        GeofenceZone geofence = resolveGeofenceById(geofenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Géofence non trouvée"));
        if (!hasAccessToGeofence(geofence, user)) {
            throw new BadCredentialsException("Vous n'avez pas accès à cette géofence");
        }

        List<GeofenceFork> forks = forkRepository.findByOriginalGeofenceIdOrderByForkedAtDesc(geofenceId);

        List<GeofenceForkDTO> dtos = forks.stream()
                .map(forkMapper)
                .toList();

        return new GeofenceForkListResponse(dtos, dtos.size());
    }

    @Override
    public GeofenceForkListResponse getMyForks(User user) {

        List<GeofenceFork> forks = forkRepository.findByForkedByOrderByForkedAtDesc(user);

        List<GeofenceForkDTO> dtos = forks.stream()
                .map(forkMapper)
                .toList();

        return new GeofenceForkListResponse(dtos, dtos.size());
    }

    @Override
    public GeofenceForkInfoDTO getForkInfo(UUID geofenceId, String geofenceType, User user) {
        GeofenceZone geofence = getGeofenceZone(geofenceId, geofenceType);
        if (!hasAccessToGeofence(geofence, user)) {
            throw new BadCredentialsException("Vous n'avez pas accès à cette géofence");
        }

        Optional<GeofenceFork> forkOpt = forkRepository.findByForkedGeofenceId(geofenceId);

        if (forkOpt.isEmpty()) {
            return null; // Cette géofence n'est pas un fork
        }

        GeofenceFork fork = forkOpt.get();
        GeofenceZone original = fork.getOriginalGeofence();

        return new GeofenceForkInfoDTO(
                original.getId().toString(),
                original.getTitle(),
                userMapper.apply(original.getUser()),
                fork.getForkedAt(),
                fork.getForkReason()
        );
    }

    @Override
    public GeofenceWithForkInfoDTO getGeofenceWithForkInfo(UUID geofenceId, String geofenceType, User user) {
        GeofenceZone geofence = getGeofenceZone(geofenceId, geofenceType);

        if (!hasAccessToGeofence(geofence, user)) {
            throw new BadCredentialsException("Vous n'avez pas accès à cette géofence");
        }

        // Vérifier si c'est un fork
        GeofenceForkInfoDTO forkInfo = getForkInfo(geofenceId, geofenceType, user);
        boolean isFork = forkInfo != null;

        // Compter les forks de cette géofence
        long forkCount = forkRepository.countByOriginalGeofenceId(geofenceId);
        boolean isOriginal = forkCount > 0;

        return new GeofenceWithForkInfoDTO(
                geofence.getId().toString(),
                geofence.getTitle(),
                geofence.getDescription(),
                geofenceType.equals("c") ? "circle" : "polygon",
                userMapper.apply(geofence.getUser()),
                isOriginal,
                isFork,
                forkInfo,
                forkCount
        );
    }

    @Override
    @Transactional
    public void deleteFork(UUID forkId, User user) {

        GeofenceFork fork = forkRepository.findById(forkId)
                .orElseThrow(() -> new ResourceNotFoundException("Fork non trouvé"));

        // Vérifier que l'utilisateur est le créateur du fork
        if (!fork.getForkedBy().getUuid().equals(user.getUuid())) {
            throw new BadCredentialsException("Vous n'êtes pas autorisé à supprimer ce fork");
        }

        // Supprimer la géofence forkée d'abord
        GeofenceZone forkedGeofence = fork.getForkedGeofence();
        if (forkedGeofence instanceof CircleGeofenceZone) {
            circleRepository.delete((CircleGeofenceZone) forkedGeofence);
        } else if (forkedGeofence instanceof PolygonGeofenceZone) {
            polygonRepository.delete((PolygonGeofenceZone) forkedGeofence);
        }

        // Puis supprimer l'enregistrement de fork
        forkRepository.delete(fork);

    }

    // Méthodes privées

    private GeofenceZone getGeofenceZone(UUID geofenceId, String geofenceType) {
        if ("c".equals(geofenceType)) {
            return circleRepository.findById(geofenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Géofence circulaire non trouvée"));
        } else if ("p".equals(geofenceType)) {
            return polygonRepository.findById(geofenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Géofence polygonale non trouvée"));
        } else {
            throw new BadCredentialsException("Type de géofence non valide: " + geofenceType);
        }
    }

    private boolean hasAccessToGeofence(GeofenceZone geofence, User user) {
        if (geofence.getUser().getOrganization() == null || user.getOrganization() == null ||
                !geofence.getUser().getOrganization().getId().equals(user.getOrganization().getId())) {
            return false;
        }

        // Vérifier si l'utilisateur est propriétaire
        if (geofence.getUser().getUuid().equals(user.getUuid())) {
            return true;
        }

        // Vérifier si la géofence est partagée avec l'utilisateur
        List<GeofenceShare> shares = shareRepository.findBySharedWith(user);
        return shares.stream()
                .anyMatch(share -> share.getGeofenceZone().getId().equals(geofence.getId()));
    }

    private Optional<GeofenceZone> resolveGeofenceById(UUID geofenceId) {
        Optional<? extends GeofenceZone> circle = circleRepository.findById(geofenceId);
        if (circle.isPresent()) {
            return Optional.of(circle.get());
        }
        Optional<? extends GeofenceZone> polygon = polygonRepository.findById(geofenceId);
        return polygon.map(g -> g);
    }

    private GeofenceZone createGeofenceCopy(GeofenceZone original, ForkGeofenceRequest request, User newOwner) {
        if (original instanceof CircleGeofenceZone circle) {
            CircleGeofenceZone newCircle = new CircleGeofenceZone();
            newCircle.setTitle(request.newTitle());
            newCircle.setDescription(request.newDescription() != null ? request.newDescription() : circle.getDescription());
            newCircle.setUser(newOwner);
            newCircle.setCenter(circle.getCenter());
            newCircle.setRadius(circle.getRadius());

            return circleRepository.save(newCircle);

        } else if (original instanceof PolygonGeofenceZone polygon) {
            PolygonGeofenceZone newPolygon = new PolygonGeofenceZone();
            newPolygon.setTitle(request.newTitle());
            newPolygon.setDescription(request.newDescription() != null ? request.newDescription() : polygon.getDescription());
            newPolygon.setUser(newOwner);
            newPolygon.setPolygon(polygon.getPolygon());

            return polygonRepository.save(newPolygon);

        } else {
            throw new BadCredentialsException("Type de géofence non supporté pour le fork");
        }
    }
}