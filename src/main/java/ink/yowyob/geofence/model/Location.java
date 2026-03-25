// main/java/com/reseau/geofence/model/Location.java
package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point position; // Position géographique

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp; // Horodatage de la position

    @Column
    private Double speed; // Vitesse en km/h (optionnelle)

    @Column
    private Double heading; // Direction en degrés (0-360, optionnelle)

    @Column
    private Double altitude; // Altitude en mètres (optionnelle)

    @Column
    private Double accuracy; // Précision en mètres (optionnelle)

    @Column(length = 500)
    private String source; // Source de la position (GPS, NETWORK, etc.)

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}