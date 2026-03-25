package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geofence_forks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceFork {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_geofence_id", nullable = false)
    private GeofenceZone originalGeofence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_geofence_id", nullable = false)
    private GeofenceZone forkedGeofence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_by_user_id", nullable = false)
    private User forkedBy;

    @Column(nullable = false)
    private LocalDateTime forkedAt;

    @Column(length = 500)
    private String forkReason; // Raison optionnelle du fork

    @PrePersist
    protected void onCreate() {
        if (forkedAt == null) {
            forkedAt = LocalDateTime.now();
        }
    }

    // Constructeur utilitaire
    public GeofenceFork(GeofenceZone originalGeofence, GeofenceZone forkedGeofence,
                        User forkedBy, String forkReason) {
        this.originalGeofence = originalGeofence;
        this.forkedGeofence = forkedGeofence;
        this.forkedBy = forkedBy;
        this.forkReason = forkReason;
        this.forkedAt = LocalDateTime.now();
    }
}