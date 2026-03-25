package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
public abstract class GeofenceZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private String description;

    @ManyToOne
    @JoinColumn(name = "user_uuid")
    private User user;

    // Géofence intelligente - Configuration temporelle
    @Column(name = "is_temporal_enabled", nullable = false, columnDefinition = "boolean default false")
    private Boolean isTemporalEnabled = false;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Enumerated(EnumType.STRING)
    @ElementCollection(targetClass = DayOfWeek.class)
    @CollectionTable(name = "geofence_active_days", joinColumns = @JoinColumn(name = "geofence_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> activeDays = EnumSet.allOf(DayOfWeek.class);

    // Géofence intelligente - Configuration conditionnelle
    @Column(name = "is_conditional_enabled", nullable = false, columnDefinition = "boolean default false")
    private Boolean isConditionalEnabled = false;
    
    @Column(name = "max_speed")
    private Double maxSpeed; // Vitesse maximale autorisée en km/h
    
    @Column(name = "max_dwell_time")
    private Integer maxDwellTime; // Temps maximum de séjour en minutes
    
    @Column(name = "min_dwell_time")
    private Integer minDwellTime; // Temps minimum de séjour en minutes

    // Métadonnées
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private Boolean isActive = true;

    // Méthodes utilitaires pour la géofence intelligente
    
    public boolean isActiveAtTime(LocalDateTime dateTime) {
        if (isTemporalEnabled == null || !isTemporalEnabled) {
            return isActive == null ? true : isActive;
        }
        
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        LocalTime timeOfDay = dateTime.toLocalTime();
        
        // Vérifier le jour de la semaine
        if (!activeDays.contains(dayOfWeek)) {
            return false;
        }
        
        // Vérifier l'heure si définie
        if (startTime != null && endTime != null) {
            if (startTime.isBefore(endTime)) {
                // Même jour (ex: 08:00 - 18:00)
                return !timeOfDay.isBefore(startTime) && !timeOfDay.isAfter(endTime);
            } else {
                // Traversée de minuit (ex: 22:00 - 06:00)
                return !timeOfDay.isBefore(startTime) || !timeOfDay.isAfter(endTime);
            }
        }
        
        return isActive == null ? true : isActive;
    }

}

