package ink.yowyob.geofence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table
@Getter
@Setter
public class PolygonGeofenceZone extends GeofenceZone {
    @Column(columnDefinition = "geometry(Polygon, 4326)")
    private Polygon polygon;
}
