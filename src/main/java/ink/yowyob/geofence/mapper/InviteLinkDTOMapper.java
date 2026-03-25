package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.response.InviteLinkDTO;
import ink.yowyob.geofence.model.GeofenceInviteLink;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class InviteLinkDTOMapper implements Function<GeofenceInviteLink, InviteLinkDTO> {

    private final UserDTOMapper userDTOMapper;

    public InviteLinkDTOMapper(UserDTOMapper userDTOMapper) {
        this.userDTOMapper = userDTOMapper;
    }

    @Override
    public InviteLinkDTO apply(GeofenceInviteLink inviteLink) {
        return new InviteLinkDTO(
                inviteLink.getId(),
                inviteLink.getInviteCode(),
                inviteLink.getGeofenceId(),
                inviteLink.getGeofenceType(),
                userDTOMapper.apply(inviteLink.getCreatedBy()),
                inviteLink.getCreatedAt(),
                inviteLink.getExpiresAt(),
                inviteLink.isCanEdit(),
                inviteLink.isActive(),
                inviteLink.getMaxUses(),
                inviteLink.getCurrentUses(),
                inviteLink.isExpired(),
                inviteLink.canBeUsed()
        );
    }
}