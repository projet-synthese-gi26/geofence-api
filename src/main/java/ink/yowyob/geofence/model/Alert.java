package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    private AlertType type;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
    
    private String message;
    
    @OneToOne
    private Location location;

    @ManyToOne
    private Vehicle vehicle;

    @ManyToOne
    private GeofenceZone geofenceZone;
    
    // Données additionnelles pour géofence intelligente
    @Column(name = "speed")
    private Double speed; // Vitesse au moment de l'alerte
    
    @Column(name = "dwell_time_minutes")
    private Integer dwellTimeMinutes; // Temps de séjour en minutes
    
    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private Boolean isRead = false; // Statut de lecture de l'alerte
    
    @Column(name = "severity", columnDefinition = "varchar(20) default 'INFO'")
    private String severity = "INFO"; // CRITICAL, WARNING, INFO
    
    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData; // JSON pour données supplémentaires
}
