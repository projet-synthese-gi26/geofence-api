package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String brand;
    private String model;
    private String licensePlate;
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_uuid")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "vehicle_geofence_zones",
            joinColumns = @JoinColumn(name = "vehicle_id"),
            inverseJoinColumns = @JoinColumn(name = "geofence_zone_id")
    )
    private Set<GeofenceZone> geofenceZones = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "vehicle_assigned_routes",
            joinColumns = @JoinColumn(name = "vehicle_id"),
            inverseJoinColumns = @JoinColumn(name = "route_id")
    )
    private Set<Route> assignedRoutes = new HashSet<>();

    private String imageUrl;

}