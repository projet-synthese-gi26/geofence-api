package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.ForkGeofenceRequest;
import ink.yowyob.geofence.dto.response.GeofenceForkListResponse;
import ink.yowyob.geofence.dto.response.GeofenceWithForkInfoDTO;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.dto.response.GeofenceForkDTO;
import ink.yowyob.geofence.dto.response.GeofenceForkInfoDTO;

import java.util.UUID;

public interface GeofenceForkService {
    GeofenceForkDTO forkGeofence(ForkGeofenceRequest request, User user);
    GeofenceForkListResponse getForksOfGeofence(UUID geofenceId, User user);
    GeofenceForkListResponse getMyForks(User user);
    GeofenceForkInfoDTO getForkInfo(UUID geofenceId, String geofenceType, User user);
    void deleteFork(UUID forkId, User user);
    GeofenceWithForkInfoDTO getGeofenceWithForkInfo(UUID geofenceId, String geofenceType, User user);
}