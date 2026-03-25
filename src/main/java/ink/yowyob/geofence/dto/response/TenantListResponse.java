package ink.yowyob.geofence.dto.response;

import java.util.List;

/**
 * DTO de réponse pour une liste de tenants/organisations
 */
public record TenantListResponse(
        List<TenantDTOResponse> tenants,
        int totalItems
) {
}
