package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.TenantDTORequest;
import ink.yowyob.geofence.dto.response.TenantDTOResponse;
import ink.yowyob.geofence.dto.response.TenantListResponse;

import java.util.UUID;

/**
 * Service pour la gestion des tenants/organisations
 * Seuls les utilisateurs ADMIN peuvent accéder à ces fonctionnalités
 */
public interface OrganizationService {

    /**
     * Récupère tous les tenants/organisations
     */
    TenantListResponse getAllTenants();

    /**
     * Récupère un tenant par son ID
     */
    TenantDTOResponse getTenantById(UUID tenantId);

    /**
     * Crée un nouveau tenant/organisation
     */
    TenantDTOResponse createTenant(TenantDTORequest request);

    /**
     * Met à jour un tenant existant
     */
    TenantDTOResponse updateTenant(UUID tenantId, TenantDTORequest request);

    /**
     * Désactive un tenant (soft delete)
     */
    void deactivateTenant(UUID tenantId);

    /**
     * Active un tenant
     */
    TenantDTOResponse activateTenant(UUID tenantId);

    /**
     * Supprime définitivement un tenant
     * ATTENTION: Cette opération est irréversible
     */
    void deleteTenant(UUID tenantId);
}
