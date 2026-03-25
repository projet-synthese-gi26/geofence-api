package ink.yowyob.geofence.dto.response;

public record InviteLinkDetailsResponse(
        InviteLinkDTO inviteLink,
        GeofenceZoneSimpleDTO geofenceZone,
        String fullInviteUrl
) {}