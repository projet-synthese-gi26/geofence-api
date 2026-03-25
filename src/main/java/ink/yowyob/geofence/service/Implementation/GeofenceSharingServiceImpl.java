package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.ShareStatus;
import ink.yowyob.geofence.dto.request.ShareGeofenceRequest;
import ink.yowyob.geofence.dto.response.GeofenceShareDTO;
import ink.yowyob.geofence.dto.response.GeofenceShareListResponse;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.mapper.GeofenceShareDTOMapper;
import ink.yowyob.geofence.model.GeofenceShare;
import ink.yowyob.geofence.model.GeofenceZone;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.CircleGeofenceZoneRepository;
import ink.yowyob.geofence.repository.GeofenceShareRepository;
import ink.yowyob.geofence.repository.PolygonGeofenceZoneRepository;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.GeofenceSharingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class GeofenceSharingServiceImpl implements GeofenceSharingService {
    private final GeofenceShareRepository geofenceShareRepository;
    private final UserRepository userRepository;
    private final CircleGeofenceZoneRepository circleGeofenceZoneRepository;
    private final PolygonGeofenceZoneRepository polygonGeofenceZoneRepository;
    private final GeofenceShareDTOMapper geofenceShareDTOMapper;

    @Override
    public GeofenceShareDTO shareGeofence(UUID geofenceId, String type, ShareGeofenceRequest request, User user) {

        // Get the geofence zone
        GeofenceZone geofenceZone;
        if (Objects.equals(type, "c")) {
            geofenceZone = circleGeofenceZoneRepository.findById(geofenceId)
                    .orElseThrow(() -> new IllegalStateException("Circle geofence zone with id " + geofenceId + " does not exist"));
        } else if (Objects.equals(type, "p")) {
            geofenceZone = polygonGeofenceZoneRepository.findById(geofenceId)
                    .orElseThrow(() -> new IllegalStateException("Polygon geofence zone with id " + geofenceId + " does not exist"));
        } else {
            throw new IllegalArgumentException("Invalid geofence zone type: " + type);
        }

        // Check if the current user is the owner of the geofence
        if (!Objects.equals(geofenceZone.getUser().getUuid(), user.getUuid())) {
            throw new BadCredentialsException("User is not authorized to share this geofence zone");
        }

        assertSameOrganization(user, geofenceZone.getUser());

        // Get the target user
        User targetUser = userRepository.findByUuidAndOrganization_Id(request.targetUserId(), user.getOrganization().getId())
                .orElseThrow(() -> new UsernameNotFoundException("Target user not found"));

        // Create the geofence share with PENDING status
        GeofenceShare geofenceShare = new GeofenceShare();
        geofenceShare.setGeofenceZone(geofenceZone);
        geofenceShare.setSharedBy(user);
        geofenceShare.setSharedWith(targetUser);
        geofenceShare.setSharedAt(LocalDateTime.now());
        geofenceShare.setExpiresAt(request.expiresAt());
        geofenceShare.setCanEdit(request.canEdit());
        geofenceShare.setStatus(ShareStatus.PENDING); // Nouveau: status en attente

        // Save and return
        GeofenceShare savedShare = geofenceShareRepository.save(geofenceShare);

        return geofenceShareDTOMapper.apply(savedShare);
    }

    @Override
    public GeofenceShareDTO updateGeofenceShare(UUID shareId, ShareGeofenceRequest request, User user) {

        // Get the geofence share
        GeofenceShare geofenceShare = geofenceShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalStateException("Geofence share with id " + shareId + " does not exist"));

        assertSameOrganization(user, geofenceShare.getSharedBy());
        assertSameOrganization(user, geofenceShare.getSharedWith());
        assertSameOrganization(user, geofenceShare.getGeofenceZone().getUser());

        // Check if the current user is the owner of the share
        if (!Objects.equals(geofenceShare.getSharedBy().getUuid(), user.getUuid())) {
            throw new BadCredentialsException("User is not authorized to update this geofence share");
        }

        // Update the target user if specified
        if (request.targetUserId() != null) {
            User targetUser = userRepository.findByUuidAndOrganization_Id(request.targetUserId(), user.getOrganization().getId())
                    .orElseThrow(() -> new UsernameNotFoundException("Target user not found"));
            geofenceShare.setSharedWith(targetUser);
        }

        // Update other fields
        if (request.expiresAt() != null) {
            geofenceShare.setExpiresAt(request.expiresAt());
        }
        geofenceShare.setCanEdit(request.canEdit());

        // Save and return
        GeofenceShare updatedShare = geofenceShareRepository.save(geofenceShare);
        return geofenceShareDTOMapper.apply(updatedShare);
    }

    @Override
    public void deleteGeofenceShare(UUID shareId, User user) {

        // Get the geofence share
        GeofenceShare geofenceShare = geofenceShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalStateException("Geofence share with id " + shareId + " does not exist"));

        assertSameOrganization(user, geofenceShare.getSharedBy());
        assertSameOrganization(user, geofenceShare.getSharedWith());

        // Check if the current user is the owner of the share
        if (!Objects.equals(geofenceShare.getSharedBy().getUuid(), user.getUuid())) {
            throw new BadCredentialsException("User is not authorized to delete this geofence share");
        }

        // Delete the share
        geofenceShareRepository.delete(geofenceShare);
    }

    @Override
    public GeofenceShareListResponse getSharedGeofences(User user) {

        // Get all ACCEPTED geofence shares shared with the current user
        List<GeofenceShare> shares = geofenceShareRepository.findBySharedWithAndStatusAndExpiresAtAfter(
                user, ShareStatus.ACCEPTED, LocalDateTime.now());

        // Convert to DTOs
        List<GeofenceShareDTO> shareDTOs = shares.stream()
                .map(geofenceShareDTOMapper)
                .collect(Collectors.toList());

        return new GeofenceShareListResponse(shareDTOs, shareDTOs.size());
    }

    @Override
    public GeofenceShareListResponse getMySharedGeofences(User user) {

        // Get all geofence shares shared by the current user
        List<GeofenceShare> shares = geofenceShareRepository.findBySharedBy(user);

        // Convert to DTOs
        List<GeofenceShareDTO> shareDTOs = shares.stream()
                .map(geofenceShareDTOMapper)
                .collect(Collectors.toList());

        return new GeofenceShareListResponse(shareDTOs, shareDTOs.size());
    }

    @Override
    public GeofenceShareListResponse getPendingInvitations(User user) {

        // Get all pending invitations for the current user
        List<GeofenceShare> pendingShares = geofenceShareRepository
                .findBySharedWithAndStatusOrderBySharedAtDesc(user, ShareStatus.PENDING);

        // Convert to DTOs
        List<GeofenceShareDTO> shareDTOs = pendingShares.stream()
                .map(geofenceShareDTOMapper)
                .collect(Collectors.toList());

        return new GeofenceShareListResponse(shareDTOs, shareDTOs.size());
    }

    @Override
    public GeofenceShareDTO respondToInvitation(UUID shareId, boolean accept, User user) {

        // Get the geofence share
        GeofenceShare geofenceShare = geofenceShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalStateException("Geofence share with id " + shareId + " does not exist"));

        assertSameOrganization(user, geofenceShare.getSharedBy());
        assertSameOrganization(user, geofenceShare.getSharedWith());

        // Check if the current user is the target of the share
        if (!Objects.equals(geofenceShare.getSharedWith().getUuid(), user.getUuid())) {
            throw new BadCredentialsException("User is not authorized to respond to this invitation");
        }

        // Check if the invitation is still pending
        if (geofenceShare.getStatus() != ShareStatus.PENDING) {
            throw new BadCredentialsException("This invitation has already been responded to");
        }

        // Check if the invitation has not expired
        if (geofenceShare.getExpiresAt() != null && geofenceShare.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("This invitation has expired");
        }

        // Update the status based on user response
        geofenceShare.setStatus(accept ? ShareStatus.ACCEPTED : ShareStatus.REFUSED);
        geofenceShare.setRespondedAt(LocalDateTime.now());

        // Save and return
        GeofenceShare updatedShare = geofenceShareRepository.save(geofenceShare);

        return geofenceShareDTOMapper.apply(updatedShare);
    }

    private void assertSameOrganization(User actor, User otherUser) {
        if (actor.getOrganization() == null || otherUser.getOrganization() == null ||
                !Objects.equals(actor.getOrganization().getId(), otherUser.getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant access denied");
        }
    }
}