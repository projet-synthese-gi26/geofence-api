package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.dto.LineStringDTO;
import ink.yowyob.geofence.dto.response.RouteSegmentDTOResponse;
import ink.yowyob.geofence.model.RouteSegment;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Service
public class RouteSegmentDTOMapper implements Function<RouteSegment, RouteSegmentDTOResponse> {

    @Override
    public RouteSegmentDTOResponse apply(RouteSegment segment) {
        return new RouteSegmentDTOResponse(
            segment.getId(),
            segment.getName(),
            segment.getDescription(),
            convertLineStringToDTO(segment.getPathLine()),
            segment.getSegmentOrder(),
            segment.getSegmentLength(),
            segment.getSegmentType(),
            segment.getPriority(),
            segment.getSpeedLimit(),
            segment.getEstimatedTime(),
            segment.getIsActive(),
            segment.getCreatedAt(),
            segment.getUpdatedAt()
        );
    }

    private LineStringDTO convertLineStringToDTO(LineString lineString) {
        if (lineString == null) return null;
        
        List<List<Double>> coordinates = new ArrayList<>();
        
        for (Coordinate coord : lineString.getCoordinates()) {
            coordinates.add(Arrays.asList(coord.x, coord.y));
        }
        
        return new LineStringDTO(coordinates);
    }
}