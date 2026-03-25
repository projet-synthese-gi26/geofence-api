package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.dto.request.CreateInviteLinkRequest;
import ink.yowyob.geofence.dto.response.GeofenceZoneSimpleDTO;
import ink.yowyob.geofence.dto.response.InviteLinkDTO;
import ink.yowyob.geofence.dto.response.InviteLinkDetailsResponse;
import ink.yowyob.geofence.dto.response.InviteLinkListResponse;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.exception.ResourceNotFoundException;
import ink.yowyob.geofence.mapper.InviteLinkDTOMapper;
import ink.yowyob.geofence.model.*;
import ink.yowyob.geofence.repository.*;
import ink.yowyob.geofence.service.GeofenceInviteLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeofenceInviteLinkServiceImpl implements GeofenceInviteLinkService {

    private final GeofenceInviteLinkRepository inviteLinkRepository;
    private final CircleGeofenceZoneRepository circleRepository;
    private final PolygonGeofenceZoneRepository polygonRepository;
    private final UserRepository userRepository;
    private final GeofenceShareRepository geofenceShareRepository; // AJOUTÉ
    private final InviteLinkDTOMapper inviteLinkMapper;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 12;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public InviteLinkDTO createInviteLink(CreateInviteLinkRequest request, User user) {

        // Vérifier que la géofence existe et appartient à l'utilisateur
        verifyGeofenceOwnership(request.geofenceId(), request.geofenceType(), user);

        // Générer un code unique
        String inviteCode = generateUniqueInviteCode();

        GeofenceInviteLink inviteLink = GeofenceInviteLink.builder()
                .inviteCode(inviteCode)
                .geofenceId(request.geofenceId())
                .geofenceType(request.geofenceType())
                .createdBy(user)
                .expiresAt(request.expiresAt())
                .canEdit(request.canEdit())
                .isActive(true)
                .maxUses(request.maxUses())
                .currentUses(0)
                .build();

        GeofenceInviteLink saved = inviteLinkRepository.save(inviteLink);

        return inviteLinkMapper.apply(saved);
    }

    @Override
    public InviteLinkDetailsResponse getInviteLinkDetails(String inviteCode) {
        GeofenceInviteLink inviteLink = inviteLinkRepository
                .findByInviteCodeAndIsActiveTrue(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Lien d'invitation non trouvé ou expiré"));

        // Récupérer les détails de la géofence
        GeofenceZoneSimpleDTO geofenceDetails = getGeofenceSimpleDetails(
                inviteLink.getGeofenceId(), inviteLink.getGeofenceType());

        String fullUrl = frontendUrl + "/invite/" + inviteCode;


        return new InviteLinkDetailsResponse(
                inviteLinkMapper.apply(inviteLink),
                geofenceDetails,
                fullUrl
        );
    }

    @Override
    @Transactional
    public void joinGeofenceViaInvite(String inviteCode, User user) {

        GeofenceInviteLink inviteLink = inviteLinkRepository
                .findByInviteCodeAndIsActiveTrue(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Lien d'invitation non trouvé ou expiré"));

        if (!inviteLink.canBeUsed()) {
            throw new BadCredentialsException("Ce lien d'invitation n'est plus valide");
        }

        // Vérifier si l'utilisateur n'est pas déjà partagé avec cette géofence
        if (isGeofenceAlreadySharedWithUser(inviteLink.getGeofenceId(), user)) {
            throw new BadCredentialsException("Vous avez déjà accès à cette géofence");
        }

        // Récupérer la géofence pour créer le partage
        GeofenceZone geofenceZone = getGeofenceZone(inviteLink.getGeofenceId(), inviteLink.getGeofenceType());

        if (inviteLink.getCreatedBy().getOrganization() == null || user.getOrganization() == null ||
                !inviteLink.getCreatedBy().getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant invite usage is not allowed");
        }

        if (geofenceZone.getUser().getOrganization() == null || user.getOrganization() == null ||
                !geofenceZone.getUser().getOrganization().getId().equals(user.getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant access denied");
        }

        // Créer directement le partage sans passer par le service de partage
        GeofenceShare newShare = new GeofenceShare();
        newShare.setGeofenceZone(geofenceZone);
        newShare.setSharedBy(inviteLink.getCreatedBy()); // Le créateur du lien
        newShare.setSharedWith(user); // L'utilisateur qui rejoint
        newShare.setSharedAt(LocalDateTime.now());
        newShare.setExpiresAt(inviteLink.getExpiresAt()); // Même expiration que le lien
        newShare.setCanEdit(inviteLink.isCanEdit());

        geofenceShareRepository.save(newShare);

        // Incrémenter le compteur d'utilisations
        inviteLink.setCurrentUses(inviteLink.getCurrentUses() + 1);
        inviteLinkRepository.save(inviteLink);

    }

    @Override
    public InviteLinkListResponse getMyInviteLinks(User user) {

        List<GeofenceInviteLink> inviteLinks = inviteLinkRepository
                .findByCreatedByUuidOrderByCreatedAtDesc(user.getUuid());

        List<InviteLinkDTO> dtos = inviteLinks.stream()
                .map(inviteLinkMapper)
                .toList();

        return new InviteLinkListResponse(dtos, dtos.size());
    }

    @Override
    public InviteLinkListResponse getInviteLinksForGeofence(UUID geofenceId, String geofenceType, User user) {

        // Vérifier la propriété
        verifyGeofenceOwnership(geofenceId, geofenceType, user);

        List<GeofenceInviteLink> inviteLinks = inviteLinkRepository
                .findActiveInviteLinksForGeofence(user.getUuid(), geofenceId, geofenceType);

        List<InviteLinkDTO> dtos = inviteLinks.stream()
                .map(inviteLinkMapper)
                .toList();

        return new InviteLinkListResponse(dtos, dtos.size());
    }

    @Override
    @Transactional
    public void deactivateInviteLink(String inviteCode, User user) {

        GeofenceInviteLink inviteLink = inviteLinkRepository
                .findByInviteCodeAndIsActiveTrue(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Lien d'invitation non trouvé"));

        if (!inviteLink.getCreatedBy().getUuid().equals(user.getUuid())) {
            throw new BadCredentialsException("Vous n'êtes pas autorisé à désactiver ce lien");
        }

        inviteLink.setActive(false);
        inviteLinkRepository.save(inviteLink);

    }

    @Override
    @Transactional
    public void deleteInviteLink(UUID inviteLinkId, User user) {

        GeofenceInviteLink inviteLink = inviteLinkRepository.findById(inviteLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Lien d'invitation non trouvé"));

        if (!inviteLink.getCreatedBy().getUuid().equals(user.getUuid())) {
            throw new BadCredentialsException("Vous n'êtes pas autorisé à supprimer ce lien");
        }

        inviteLinkRepository.delete(inviteLink);
    }

    // Méthodes privées
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (inviteLinkRepository.existsByInviteCode(code));
        return code;
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

        private void verifyGeofenceOwnership(UUID geofenceId, String geofenceType, User user) {
        boolean isOwner = false;
        if ("c".equals(geofenceType)) {
            isOwner = circleRepository.findById(geofenceId)
                .map(circle -> circle.getUser().getUuid().equals(user.getUuid()) &&
                    circle.getUser().getOrganization() != null &&
                    user.getOrganization() != null &&
                    circle.getUser().getOrganization().getId().equals(user.getOrganization().getId()))
                    .orElse(false);
        } else if ("p".equals(geofenceType)) {
            isOwner = polygonRepository.findById(geofenceId)
                .map(polygon -> polygon.getUser().getUuid().equals(user.getUuid()) &&
                    polygon.getUser().getOrganization() != null &&
                    user.getOrganization() != null &&
                    polygon.getUser().getOrganization().getId().equals(user.getOrganization().getId()))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException("Géofence non trouvée ou vous n'en êtes pas le propriétaire");
        }

        if (!isOwner) {
            throw new ResourceNotFoundException("Géofence non trouvée ou vous n'en êtes pas le propriétaire");
        }
    }

    private GeofenceZoneSimpleDTO getGeofenceSimpleDetails(UUID geofenceId, String geofenceType) {
        if ("c".equals(geofenceType)) {
            CircleGeofenceZone circle = circleRepository.findById(geofenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Géofence circulaire non trouvée"));
            return new GeofenceZoneSimpleDTO(circle.getId(), circle.getTitle(), "circle");
        } else {
            PolygonGeofenceZone polygon = polygonRepository.findById(geofenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Géofence polygonale non trouvée"));
            return new GeofenceZoneSimpleDTO(polygon.getId(), polygon.getTitle(), "polygon");
        }
    }

    // NOUVELLES MÉTHODES AJOUTÉES

    /**
     * Vérifier si une géofence est déjà partagée avec un utilisateur
     */
    private boolean isGeofenceAlreadySharedWithUser(UUID geofenceId, User user) {
        List<GeofenceShare> existingShares = geofenceShareRepository.findBySharedWith(user);
        return existingShares.stream()
                .anyMatch(share -> share.getGeofenceZone().getId().equals(geofenceId));
    }

    /**
     * Récupérer une géofence (circle ou polygon) par son ID et type
     */
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
}
