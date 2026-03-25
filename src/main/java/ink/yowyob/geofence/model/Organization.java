package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String domain; // easy-rental.com

    @Column(unique = true, nullable = false)
    private String apiKey;

    private String contactEmail;
    private String webhookUrl;

    private boolean isActive = true;
    private boolean isInternal = false; // true pour votre frontend original

    // Subscription & Quotas
    private String subscriptionPlan = "FREE";
    private int maxUsers = 10;
    private int maxVehicles = 50;
    private int maxGeofences = 20;
    private int rateLimitPerHour = 1000;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastApiCall;

    // Méthodes utilitaires
    public boolean canCreateVehicle(long currentCount) {
        return currentCount < maxVehicles;
    }

    public boolean canCreateGeofence(long currentCount) {
        return currentCount < maxGeofences;
    }
}
