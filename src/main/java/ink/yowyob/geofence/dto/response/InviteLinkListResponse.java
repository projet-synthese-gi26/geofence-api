package ink.yowyob.geofence.dto.response;

import java.util.List;

public record InviteLinkListResponse(
        List<InviteLinkDTO> inviteLinks,
        int totalItems
) {}
