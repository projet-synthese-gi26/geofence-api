package ink.yowyob.geofence.service;

import ink.yowyob.geofence.Enum.AlertTypeEnum;
import ink.yowyob.geofence.dto.response.AlertDTO;
import ink.yowyob.geofence.dto.response.AlertListResponse;
import ink.yowyob.geofence.model.GeofenceZone;
import ink.yowyob.geofence.model.Location;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.model.Vehicle;

import java.util.UUID;

public interface AlertService {
    AlertDTO createAlert(Vehicle vehicle, GeofenceZone zone, AlertTypeEnum alertType, Location location, String message);
    AlertListResponse getMyAlerts(Integer page, Integer size, User user);
    AlertListResponse getAllAlerts(Integer page, Integer size, User user);
    AlertDTO getAlert(UUID id, User user);
}