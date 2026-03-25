package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.response.SystemStatisticsDTO;
import ink.yowyob.geofence.dto.response.UserStatisticsDTO;
import ink.yowyob.geofence.dto.response.VehicleStatisticsDTO;
import ink.yowyob.geofence.model.User;

import java.util.UUID;

public interface StatisticsService {
    SystemStatisticsDTO getSystemStatistics(User user);
    UserStatisticsDTO getUserStatistics(User user);
    VehicleStatisticsDTO getVehicleStatistics(UUID vehicleId, User user);
}