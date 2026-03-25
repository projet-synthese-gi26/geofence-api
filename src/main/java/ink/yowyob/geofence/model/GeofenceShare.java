package ink.yowyob.geofence.model;

import ink.yowyob.geofence.Enum.ShareStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
public class GeofenceShare {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "geofence_zone_id")
    private GeofenceZone geofenceZone;

    @ManyToOne
    @JoinColumn(name = "shared_by_user_id")
    private User sharedBy;

    @ManyToOne
    @JoinColumn(name = "shared_with_user_id")
    private User sharedWith;

    private LocalDateTime sharedAt;
    private LocalDateTime expiresAt;
    private boolean canEdit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareStatus status = ShareStatus.PENDING;

    private LocalDateTime respondedAt;
}