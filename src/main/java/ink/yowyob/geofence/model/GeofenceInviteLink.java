package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geofence_invite_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceInviteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 12)
    private String inviteCode; // Code unique (12 caractères)

    @Column(nullable = false)
    private UUID geofenceId;

    @Column(nullable = false, length = 1)
    private String geofenceType; // 'c' ou 'p'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true) // null = durée infinie
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean canEdit;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int maxUses; // -1 = illimité

    @Column(nullable = false)
    private int currentUses;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Méthodes utilitaires
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsageLimitReached() {
        return maxUses > 0 && currentUses >= maxUses;
    }

    public boolean canBeUsed() {
        return isActive && !isExpired() && !isUsageLimitReached();
    }
}
