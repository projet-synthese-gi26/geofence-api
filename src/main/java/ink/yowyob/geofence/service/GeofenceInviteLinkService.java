package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.CreateInviteLinkRequest;
import ink.yowyob.geofence.dto.response.InviteLinkDTO;
import ink.yowyob.geofence.dto.response.InviteLinkDetailsResponse;
import ink.yowyob.geofence.dto.response.InviteLinkListResponse;
import ink.yowyob.geofence.model.User;

import java.util.UUID;

public interface GeofenceInviteLinkService {
    InviteLinkDTO createInviteLink(CreateInviteLinkRequest request, User user);
    InviteLinkDetailsResponse getInviteLinkDetails(String inviteCode);
    void joinGeofenceViaInvite(String inviteCode, User user);
    InviteLinkListResponse getMyInviteLinks(User user);
    InviteLinkListResponse getInviteLinksForGeofence(UUID geofenceId, String geofenceType, User user);
    void deactivateInviteLink(String inviteCode, User user);
    void deleteInviteLink(UUID inviteLinkId, User user);
}