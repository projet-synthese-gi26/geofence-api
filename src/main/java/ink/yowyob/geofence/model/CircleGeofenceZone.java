package ink.yowyob.geofence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Entity
@Table
@Getter
@Setter
public class CircleGeofenceZone extends GeofenceZone {
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point center;

    private Double radius;
}
