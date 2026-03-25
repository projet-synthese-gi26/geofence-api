package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.CreateTenantRequest;
import ink.yowyob.geofence.dto.request.UpdateTenantRequest;
import ink.yowyob.geofence.dto.response.TenantListResponse;
import ink.yowyob.geofence.dto.response.TenantResponse;
import ink.yowyob.geofence.model.User;

public interface TenantManagementService {
    TenantListResponse getAllTenants(User actor);

    TenantResponse getTenantByApiKey(String apiKey, User actor);

    TenantResponse createTenant(CreateTenantRequest request, User actor);

    TenantResponse updateTenant(String apiKey, UpdateTenantRequest request, User actor);

    void deleteTenant(String apiKey, User actor);
}
