package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.AlertTypeEnum;
import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.response.AlertCountDTO;
import ink.yowyob.geofence.dto.response.SystemStatisticsDTO;
import ink.yowyob.geofence.dto.response.UserStatisticsDTO;
import ink.yowyob.geofence.dto.response.VehicleStatisticsDTO;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.model.Alert;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.repository.AlertRepository;
import ink.yowyob.geofence.repository.CircleGeofenceZoneRepository;
import ink.yowyob.geofence.repository.PolygonGeofenceZoneRepository;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.StatisticsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertRepository alertRepository;
    private final CircleGeofenceZoneRepository circleGeofenceZoneRepository;
    private final PolygonGeofenceZoneRepository polygonGeofenceZoneRepository;

    @Override
    public SystemStatisticsDTO getSystemStatistics(User user) {
        // Check if current user is admin or manager

        boolean isAuthorized = user.getRole().getName() == UserRole.ADMIN ||
                user.getRole().getName() == UserRole.MANAGER;

        if (!isAuthorized) {
            throw new BadCredentialsException("User is not authorized to view system statistics");
        }

        UUID organizationId = user.getOrganization().getId();

        // Calculate statistics
        List<User> tenantUsers = userRepository.findByOrganization_Id(organizationId);
        List<Vehicle> tenantVehicles = vehicleRepository.findByUser_Organization_Id(organizationId);

        long totalUsers = tenantUsers.size();
        long totalVehicles = tenantVehicles.size();
        long totalCircleGeofences = circleGeofenceZoneRepository.findByUser_Organization_Id(organizationId).size();
        long totalPolygonGeofences = polygonGeofenceZoneRepository.findByUser_Organization_Id(organizationId).size();

        List<Alert> allAlerts = tenantVehicles.isEmpty()
            ? List.of()
            : alertRepository.findByVehicleInOrderByTimestampDesc(tenantVehicles);
        long totalAlerts = allAlerts.size();

        // Get alerts by type
        Map<AlertTypeEnum, Long> alertsByType = new HashMap<>();

        for (Alert alert : allAlerts) {
            AlertTypeEnum type = alert.getType().getType();
            alertsByType.put(type, alertsByType.getOrDefault(type, 0L) + 1);
        }

        List<AlertCountDTO> alertCounts = alertsByType.entrySet().stream()
                .map(entry -> new AlertCountDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // Calculate alerts over time (last 30 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        Map<String, Long> alertsPerDay = new HashMap<>();
        for (Alert alert : allAlerts) {
            if (alert.getTimestamp().isAfter(thirtyDaysAgo)) {
                String date = alert.getTimestamp().toLocalDate().toString();
                alertsPerDay.put(date, alertsPerDay.getOrDefault(date, 0L) + 1);
            }
        }

        return new SystemStatisticsDTO(
                totalUsers,
                totalVehicles,
                totalCircleGeofences + totalPolygonGeofences,
                totalAlerts,
                alertCounts,
                alertsPerDay
        );
    }

    @Override
    public UserStatisticsDTO getUserStatistics(User user) {

        // Get all vehicles belonging to the user
        List<Vehicle> userVehicles = vehicleRepository.findByUser(user);

        // Calculate statistics
        int totalVehicles = userVehicles.size();

        int totalCircleGeofences = circleGeofenceZoneRepository.findByUser(user).size();
        int totalPolygonGeofences = polygonGeofenceZoneRepository.findByUser(user).size();
        int totalGeofences = totalCircleGeofences + totalPolygonGeofences;

        // Get all alerts for user's vehicles
        List<Alert> userAlerts = alertRepository.findByVehicleInOrderByTimestampDesc(userVehicles);
        int totalAlerts = userAlerts.size();

        // Count alerts by type
        Map<AlertTypeEnum, Long> alertsByType = new HashMap<>();
        for (Alert alert : userAlerts) {
            AlertTypeEnum type = alert.getType().getType();
            alertsByType.put(type, alertsByType.getOrDefault(type, 0L) + 1);
        }

        List<AlertCountDTO> alertCounts = alertsByType.entrySet().stream()
                .map(entry -> new AlertCountDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // Calculate alerts over time (last 30 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        Map<String, Long> alertsPerDay = new HashMap<>();
        for (Alert alert : userAlerts) {
            if (alert.getTimestamp().isAfter(thirtyDaysAgo)) {
                String date = alert.getTimestamp().toLocalDate().toString();
                alertsPerDay.put(date, alertsPerDay.getOrDefault(date, 0L) + 1);
            }
        }

        // Count alerts by vehicle
        Map<UUID, Long> alertsByVehicle = new HashMap<>();
        for (Alert alert : userAlerts) {
            UUID vehicleId = alert.getVehicle().getId();
            alertsByVehicle.put(vehicleId, alertsByVehicle.getOrDefault(vehicleId, 0L) + 1);
        }

        // Get the most active vehicle
        UUID mostActiveVehicleId = null;
        long maxAlerts = 0;
        for (Map.Entry<UUID, Long> entry : alertsByVehicle.entrySet()) {
            if (entry.getValue() > maxAlerts) {
                maxAlerts = entry.getValue();
                mostActiveVehicleId = entry.getKey();
            }
        }

        // Find the most common alert type
        AlertTypeEnum mostCommonAlertType = null;
        long maxAlertTypeCount = 0;
        for (Map.Entry<AlertTypeEnum, Long> entry : alertsByType.entrySet()) {
            if (entry.getValue() > maxAlertTypeCount) {
                maxAlertTypeCount = entry.getValue();
                mostCommonAlertType = entry.getKey();
            }
        }

        return new UserStatisticsDTO(
                totalVehicles,
                totalGeofences,
                totalAlerts,
                alertCounts,
                alertsPerDay,
                mostActiveVehicleId,
                mostCommonAlertType
        );
    }

    @Override
    public VehicleStatisticsDTO getVehicleStatistics(UUID vehicleId, User user) {

        // Get the vehicle
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + vehicleId + " does not exist"));

        if (user.getOrganization() == null || vehicle.getUser().getOrganization() == null ||
            !Objects.equals(user.getOrganization().getId(), vehicle.getUser().getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant access denied");
        }

        // Check if the user is authorized to access this vehicle's statistics
        boolean isAuthorized = Objects.equals(vehicle.getUser().getUuid(), user.getUuid()) ||
                user.getRole().getName() == UserRole.ADMIN ||
                user.getRole().getName() == UserRole.MANAGER;

        if (!isAuthorized) {
            throw new BadCredentialsException("User is not authorized to view this vehicle's statistics");
        }

        // Get all alerts for this vehicle
        List<Alert> vehicleAlerts = alertRepository.findByVehicle(vehicle);
        int totalAlerts = vehicleAlerts.size();

        // Count alerts by type
        Map<AlertTypeEnum, Long> alertsByType = new HashMap<>();
        for (Alert alert : vehicleAlerts) {
            AlertTypeEnum type = alert.getType().getType();
            alertsByType.put(type, alertsByType.getOrDefault(type, 0L) + 1);
        }

        List<AlertCountDTO> alertCounts = alertsByType.entrySet().stream()
                .map(entry -> new AlertCountDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // Calculate alerts over time (last 30 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        Map<String, Long> alertsPerDay = new HashMap<>();
        for (Alert alert : vehicleAlerts) {
            if (alert.getTimestamp().isAfter(thirtyDaysAgo)) {
                String date = alert.getTimestamp().toLocalDate().toString();
                alertsPerDay.put(date, alertsPerDay.getOrDefault(date, 0L) + 1);
            }
        }

        // Find the most common alert type
        AlertTypeEnum mostCommonAlertType = null;
        long maxAlertTypeCount = 0;
        for (Map.Entry<AlertTypeEnum, Long> entry : alertsByType.entrySet()) {
            if (entry.getValue() > maxAlertTypeCount) {
                maxAlertTypeCount = entry.getValue();
                mostCommonAlertType = entry.getKey();
            }
        }

        // Get the number of geofence zones associated with this vehicle
        int associatedGeofenceZones = vehicle.getGeofenceZones().size();

        return new VehicleStatisticsDTO(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getLicensePlate(),
                totalAlerts,
                associatedGeofenceZones,
                alertCounts,
                alertsPerDay,
                mostCommonAlertType
        );
    }
}