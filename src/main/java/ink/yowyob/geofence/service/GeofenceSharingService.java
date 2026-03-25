package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.ShareGeofenceRequest;
import ink.yowyob.geofence.dto.response.GeofenceShareDTO;
import ink.yowyob.geofence.dto.response.GeofenceShareListResponse;
import ink.yowyob.geofence.model.User;

import java.util.UUID;

public interface GeofenceSharingService {
    GeofenceShareDTO shareGeofence(UUID geofenceId, String type, ShareGeofenceRequest request, User user);
    GeofenceShareDTO updateGeofenceShare(UUID shareId, ShareGeofenceRequest request, User user);
    void deleteGeofenceShare(UUID shareId, User user);
    GeofenceShareListResponse getSharedGeofences(User user);
    GeofenceShareListResponse getMySharedGeofences(User user);
    GeofenceShareListResponse getPendingInvitations(User user);
    GeofenceShareDTO respondToInvitation(UUID shareId, boolean accept, User user);
}