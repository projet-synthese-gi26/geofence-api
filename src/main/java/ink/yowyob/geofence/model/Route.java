package ink.yowyob.geofence.model;

import ink.yowyob.geofence.Enum.DayOfWeek;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Entity
@Table(name = "routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    // Point de départ
    @Column(name = "start_point", columnDefinition = "geometry(Point,4326)")
    private Point startPoint;

    @Column(name = "start_address", length = 255)
    private String startAddress;

    // Point d'arrivée
    @Column(name = "end_point", columnDefinition = "geometry(Point,4326)")
    private Point endPoint;

    @Column(name = "end_address", length = 255)
    private String endAddress;

    // Distance totale estimée en mètres
    @Column(name = "estimated_distance")
    private Double estimatedDistance;

    // Durée estimée en minutes
    @Column(name = "estimated_duration")
    private Integer estimatedDuration;

    // Tolérance d'écart en mètres (distance maximale autorisée par rapport aux segments)
    @Column(name = "deviation_tolerance", nullable = false)
    private Double deviationTolerance = 100.0;

    // Configuration temporelle
    @Column(name = "is_temporal_enabled", nullable = false, columnDefinition = "boolean default false")
    private Boolean isTemporalEnabled = false;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @ElementCollection(targetClass = DayOfWeek.class)
    @CollectionTable(name = "route_active_days", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> activeDays = EnumSet.allOf(DayOfWeek.class);

    // Segments de route autorisés (routes alternatives)
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RouteSegment> authorizedSegments = new ArrayList<>();

    // Véhicules assignés à cette route
    @ManyToMany(mappedBy = "assignedRoutes", fetch = FetchType.LAZY)
    private Set<Vehicle> assignedVehicles = new HashSet<>();

    // Statut de la route
    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private Boolean isActive = true;

    // Métadonnées
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Méthodes utilitaires

    /**
     * Vérifie si la route est active à un moment donné
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        if (!isActive) {
            return false;
        }

        if (!isTemporalEnabled) {
            return true;
        }

        DayOfWeek dayOfWeek = DayOfWeek.from(dateTime.getDayOfWeek());
        LocalTime timeOfDay = dateTime.toLocalTime();

        // Vérifier le jour de la semaine
        if (!activeDays.contains(dayOfWeek)) {
            return false;
        }

        // Vérifier l'heure si configurée
        if (startTime != null && endTime != null) {
            if (startTime.isBefore(endTime)) {
                // Même jour
                return !timeOfDay.isBefore(startTime) && !timeOfDay.isAfter(endTime);
            } else {
                // Chevauchement minuit
                return !timeOfDay.isBefore(startTime) || !timeOfDay.isAfter(endTime);
            }
        }

        return true;
    }

    /**
     * Calcule la distance totale des segments autorisés
     */
    public double getTotalAuthorizedDistance() {
        return authorizedSegments.stream()
                .mapToDouble(RouteSegment::getSegmentLength)
                .sum();
    }

    /**
     * Vérifie si un point est dans la tolérance d'écart d'un des segments
     */
    public boolean isPointWithinTolerance(Point point) {
        return authorizedSegments.stream()
                .anyMatch(segment -> segment.isPointWithinTolerance(point, deviationTolerance));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(id, route.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", startAddress='" + startAddress + '\'' +
                ", endAddress='" + endAddress + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}