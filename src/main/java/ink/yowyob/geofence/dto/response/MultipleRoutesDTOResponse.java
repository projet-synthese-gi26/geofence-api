package ink.yowyob.geofence.dto.response;

import java.util.List;

public record MultipleRoutesDTOResponse(
    List<RouteDTOResponse> routes
) {}