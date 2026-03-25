package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.request.VehicleDTORequest;
import ink.yowyob.geofence.dto.response.MultipleVehicleDTOResponse;
import ink.yowyob.geofence.dto.response.VehicleDTOResponse;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.mapper.VehicleDTOMapper;
import ink.yowyob.geofence.model.GeofenceZone;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.model.Vehicle;
import ink.yowyob.geofence.repository.CircleGeofenceZoneRepository;
import ink.yowyob.geofence.repository.PolygonGeofenceZoneRepository;
import ink.yowyob.geofence.repository.VehicleRepository;
import ink.yowyob.geofence.service.VehicleService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository vehicleRepository;
    private final CircleGeofenceZoneRepository circleGeofenceZoneRepository;
    private final PolygonGeofenceZoneRepository polygonGeofenceZoneRepository;
    private final VehicleDTOMapper vehicleDTOMapper;

    @Transactional
    @Override
    public VehicleDTOResponse getVehicle(UUID id, User currentUser) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + id + " does not exist"));

        validateSameTenant(vehicle.getUser(), currentUser);

        // Vérifier que l'utilisateur a le droit de voir ce véhicule
        boolean isAdmin = Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(currentUser.getRole().getName(), UserRole.MANAGER);

        if (!isAdmin && !Objects.equals(vehicle.getUser().getUuid(), currentUser.getUuid())) {
            throw new BadCredentialsException("The user " + currentUser.getUsername() + " does not have permission to view this vehicle");
        }

        return vehicleDTOMapper.apply(vehicle);
    }

    @Transactional
    @Override
    public MultipleVehicleDTOResponse getVehicles(User currentUser) {
        // Seul un admin peut voir tous les véhicules

        boolean isAdmin = Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(currentUser.getRole().getName(), UserRole.MANAGER);

        if (!isAdmin) {
            throw new BadCredentialsException("Only admins can view all vehicles");
        }

        List<VehicleDTOResponse> vehicles = vehicleRepository
            .findByUser_Organization_Id(currentUser.getOrganization().getId())
                .stream()
                .map(vehicleDTOMapper)
                .collect(Collectors.toList());

        return new MultipleVehicleDTOResponse(vehicles);
    }

    @Transactional
    @Override
    public VehicleDTOResponse createVehicle(VehicleDTORequest vehicleDTORequest, User currentUser) {

        Vehicle vehicle = new Vehicle();
        vehicle.setUser(currentUser);
        vehicle.setBrand(vehicleDTORequest.brand());
        vehicle.setModel(vehicleDTORequest.model());
        vehicle.setLicensePlate(vehicleDTORequest.licensePlate());
        vehicle.setDescription(vehicleDTORequest.description());

        // Ajouter des zones de geofence si spécifiées
        if (vehicleDTORequest.geofenceZoneIds() != null && !vehicleDTORequest.geofenceZoneIds().isEmpty()) {
            Set<GeofenceZone> geofenceZones = new HashSet<>();

            for (UUID zoneId : vehicleDTORequest.geofenceZoneIds()) {
                GeofenceZone zone = resolveGeofenceZone(zoneId);
                validateSameTenant(zone.getUser(), currentUser);
                geofenceZones.add(zone);
            }

            vehicle.setGeofenceZones(geofenceZones);
        }

        return vehicleDTOMapper.apply(vehicleRepository.save(vehicle));
    }

    @Transactional
    @Override
    public MultipleVehicleDTOResponse getMyVehicles(User currentUser) {

        List<VehicleDTOResponse> vehicles = vehicleRepository.findByUser(currentUser)
                .stream()
                .map(vehicleDTOMapper)
                .collect(Collectors.toList());

        return new MultipleVehicleDTOResponse(vehicles);
    }

    @Transactional
    @Override
    public VehicleDTOResponse editVehicle(VehicleDTORequest vehicleDTORequest, UUID id, User currentUser) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + id + " does not exist"));

        validateSameTenant(vehicle.getUser(), currentUser);

        // Vérifier que l'utilisateur a le droit de modifier ce véhicule
        boolean isAdmin = Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(currentUser.getRole().getName(), UserRole.MANAGER);

        if (!isAdmin && !Objects.equals(vehicle.getUser().getUuid(), currentUser.getUuid())) {
            throw new BadCredentialsException("The user " + currentUser.getUsername() + " does not have permission to edit this vehicle");
        }

        // Mettre à jour les champs
        if (vehicleDTORequest.brand() != null) {
            vehicle.setBrand(vehicleDTORequest.brand());
        }

        if (vehicleDTORequest.model() != null) {
            vehicle.setModel(vehicleDTORequest.model());
        }

        if (vehicleDTORequest.licensePlate() != null) {
            vehicle.setLicensePlate(vehicleDTORequest.licensePlate());
        }

        if (vehicleDTORequest.description() != null) {
            vehicle.setDescription(vehicleDTORequest.description());
        }

        // Mettre à jour les zones de geofence si spécifiées
        if (vehicleDTORequest.geofenceZoneIds() != null) {
            Set<GeofenceZone> geofenceZones = new HashSet<>();

            for (UUID zoneId : vehicleDTORequest.geofenceZoneIds()) {
                GeofenceZone zone = resolveGeofenceZone(zoneId);
                validateSameTenant(zone.getUser(), currentUser);
                geofenceZones.add(zone);
            }

            vehicle.setGeofenceZones(geofenceZones);
        }

        return vehicleDTOMapper.apply(vehicleRepository.save(vehicle));
    }

    @Transactional
    @Override
    public void deleteVehicle(UUID id, User currentUser) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + id + " does not exist"));

        validateSameTenant(vehicle.getUser(), currentUser);

        // Vérifier que l'utilisateur a le droit de supprimer ce véhicule
        boolean isAdmin = Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(currentUser.getRole().getName(), UserRole.MANAGER);

        if (!isAdmin && !Objects.equals(vehicle.getUser().getUuid(), currentUser.getUuid())) {
            throw new BadCredentialsException("The user " + currentUser.getUsername() + " does not have permission to delete this vehicle");
        }

        vehicleRepository.deleteById(id);
    }

    @Transactional
    @Override
    public VehicleDTOResponse updateVehicleImage(UUID id, String imageUrl, User currentUser) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + id + " does not exist"));

        validateSameTenant(vehicle.getUser(), currentUser);

        // Vérifier que l'utilisateur a le droit de modifier ce véhicule
        boolean isAdmin = Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(currentUser.getRole().getName(), UserRole.MANAGER);

        if (!isAdmin && !Objects.equals(vehicle.getUser().getUuid(), currentUser.getUuid())) {
            throw new BadCredentialsException("The user " + currentUser.getUsername() + " does not have permission to edit this vehicle");
        }

        // Mettre à jour l'URL de l'image
        vehicle.setImageUrl(imageUrl);

        return vehicleDTOMapper.apply(vehicleRepository.save(vehicle));
    }

    @Transactional
    @Override
    public VehicleDTOResponse assignToGeofenceZone(UUID vehicleId, UUID zoneId, String type, User currentUser) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + vehicleId + " does not exist"));

        validateSameTenant(vehicle.getUser(), currentUser);

        // Vérifier que l'utilisateur a le droit de modifier ce véhicule
        boolean isAdmin = Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(currentUser.getRole().getName(), UserRole.MANAGER);

        if (!isAdmin && !Objects.equals(vehicle.getUser().getUuid(), currentUser.getUuid())) {
            throw new BadCredentialsException("The user " + currentUser.getUsername() + " does not have permission to modify this vehicle");
        }

        GeofenceZone zone = null;

        // Trouver la zone de geofence en fonction du type
        if (Objects.equals(type, "c")) {
            zone = circleGeofenceZoneRepository.findById(zoneId)
                    .orElseThrow(() -> new IllegalStateException("Circle geofence zone with id " + zoneId + " does not exist"));
        } else if (Objects.equals(type, "p")) {
            zone = polygonGeofenceZoneRepository.findById(zoneId)
                    .orElseThrow(() -> new IllegalStateException("Polygon geofence zone with id " + zoneId + " does not exist"));
        } else {
            throw new IllegalArgumentException("Invalid geofence zone type: " + type);
        }

        validateSameTenant(zone.getUser(), currentUser);

        // Ajouter la zone au véhicule
        vehicle.getGeofenceZones().add(zone);

        return vehicleDTOMapper.apply(vehicleRepository.save(vehicle));
    }

    @Transactional
    @Override
    public VehicleDTOResponse removeFromGeofenceZone(UUID vehicleId, UUID zoneId, User currentUser) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Vehicle with id " + vehicleId + " does not exist"));

        validateSameTenant(vehicle.getUser(), currentUser);

        // Vérifier que l'utilisateur a le droit de modifier ce véhicule
        boolean isAdmin = Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN) ||
                Objects.equals(currentUser.getRole().getName(), UserRole.MANAGER);

        if (!isAdmin && !Objects.equals(vehicle.getUser().getUuid(), currentUser.getUuid())) {
            throw new BadCredentialsException("The user " + currentUser.getUsername() + " does not have permission to modify this vehicle");
        }

        // Trouver la zone et la supprimer du véhicule
        vehicle.setGeofenceZones(vehicle.getGeofenceZones().stream()
                .filter(zone -> !Objects.equals(zone.getId(), zoneId))
                .collect(Collectors.toSet()));

        return vehicleDTOMapper.apply(vehicleRepository.save(vehicle));
    }

    private void validateSameTenant(User resourceOwner, User currentUser) {
        if (resourceOwner == null || currentUser == null ||
                resourceOwner.getOrganization() == null || currentUser.getOrganization() == null ||
                !Objects.equals(resourceOwner.getOrganization().getId(), currentUser.getOrganization().getId())) {
            throw new BadCredentialsException("Cross-tenant access is not allowed");
        }
    }

    private GeofenceZone resolveGeofenceZone(UUID zoneId) {
        return circleGeofenceZoneRepository.findById(zoneId)
                .map(zone -> (GeofenceZone) zone)
                .orElseGet(() -> polygonGeofenceZoneRepository.findById(zoneId)
                        .map(zone -> (GeofenceZone) zone)
                        .orElseThrow(() -> new IllegalStateException("Geofence zone with id " + zoneId + " does not exist")));
    }
}