package ink.yowyob.geofence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "route_segments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteSegment {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    // Géométrie du segment (ligne)
    @Column(name = "path_line", columnDefinition = "geometry(LineString,4326)", nullable = false)
    private LineString pathLine;

    // Ordre du segment dans la route (pour les routes séquentielles)
    @Column(name = "segment_order")
    private Integer segmentOrder;

    // Longueur du segment en mètres
    @Column(name = "segment_length", nullable = false)
    private Double segmentLength;

    // Type de segment
    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false)
    private RouteSegmentType segmentType = RouteSegmentType.MAIN_ROUTE;

    // Priorité du segment (pour les routes alternatives)
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    // Limite de vitesse pour ce segment (optionnel)
    @Column(name = "speed_limit")
    private Double speedLimit;

    // Temps estimé pour parcourir ce segment (en minutes)
    @Column(name = "estimated_time")
    private Integer estimatedTime;

    // Statut du segment
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
        
        // Calculer automatiquement la longueur du segment
        if (pathLine != null && segmentLength == null) {
            segmentLength = calculateSegmentLength();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Recalculer la longueur si le path a changé
        if (pathLine != null) {
            segmentLength = calculateSegmentLength();
        }
    }

    // Énumération pour les types de segments
    public enum RouteSegmentType {
        MAIN_ROUTE("Route principale"),
        ALTERNATIVE_ROUTE("Route alternative"),
        BYPASS_ROUTE("Route de contournement"),
        EMERGENCY_ROUTE("Route d'urgence");

        private final String description;

        RouteSegmentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Méthodes utilitaires

    /**
     * Calcule la longueur du segment en mètres
     */
    private double calculateSegmentLength() {
        if (pathLine == null) return 0.0;
        
        // Utiliser la projection sphéroïdale pour une mesure précise
        // Pour simplifier, on utilise une approximation basée sur les coordonnées
        double totalDistance = 0.0;
        
        for (int i = 0; i < pathLine.getNumPoints() - 1; i++) {
            Point p1 = pathLine.getPointN(i);
            Point p2 = pathLine.getPointN(i + 1);
            totalDistance += calculateDistanceBetweenPoints(p1, p2);
        }
        
        return totalDistance;
    }

    /**
     * Calcule la distance entre deux points en mètres (formule haversine simplifiée)
     */
    private double calculateDistanceBetweenPoints(Point p1, Point p2) {
        double lat1 = Math.toRadians(p1.getY());
        double lon1 = Math.toRadians(p1.getX());
        double lat2 = Math.toRadians(p2.getY());
        double lon2 = Math.toRadians(p2.getX());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double earthRadius = 6371000; // Rayon de la Terre en mètres

        return earthRadius * c;
    }

    /**
     * Vérifie si un point est dans la tolérance d'écart de ce segment
     */
    public boolean isPointWithinTolerance(Point point, double toleranceMeters) {
        if (pathLine == null || point == null) return false;
        
        // Calculer la distance minimale du point à la ligne
        double minDistance = pathLine.distance(point);
        
        // Convertir la tolérance en degrés approximativement
        // 1 degré ≈ 111000 mètres à l'équateur
        double toleranceDegrees = toleranceMeters / 111000.0;
        
        return minDistance <= toleranceDegrees;
    }

    /**
     * Obtient le point le plus proche sur ce segment pour un point donné
     */
    public Point getClosestPointOnSegment(Point point) {
        if (pathLine == null || point == null) return null;
        
        // Trouver le point le plus proche sur la ligne
        double minDistance = Double.MAX_VALUE;
        Point closestPoint = null;
        
        for (int i = 0; i < pathLine.getNumPoints(); i++) {
            Point segmentPoint = pathLine.getPointN(i);
            double distance = point.distance(segmentPoint);
            
            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = segmentPoint;
            }
        }
        
        return closestPoint;
    }

    /**
     * Calcule le pourcentage de progression sur ce segment pour un point donné
     */
    public double getProgressPercentage(Point point) {
        if (pathLine == null || point == null) return 0.0;
        
        Point closestPoint = getClosestPointOnSegment(point);
        if (closestPoint == null) return 0.0;
        
        // Calculer la distance du début du segment au point le plus proche
        Point startPoint = pathLine.getPointN(0);
        double distanceToClosest = calculateDistanceBetweenPoints(startPoint, closestPoint);
        
        if (segmentLength == 0) return 0.0;
        
        return Math.min(100.0, (distanceToClosest / segmentLength) * 100.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteSegment that = (RouteSegment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RouteSegment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", segmentType=" + segmentType +
                ", segmentLength=" + segmentLength +
                ", priority=" + priority +
                ", isActive=" + isActive +
                '}';
    }
}