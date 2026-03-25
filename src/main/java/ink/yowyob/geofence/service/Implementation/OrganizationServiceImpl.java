package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.dto.request.TenantDTORequest;
import ink.yowyob.geofence.dto.response.TenantDTOResponse;
import ink.yowyob.geofence.dto.response.TenantListResponse;
import ink.yowyob.geofence.model.Organization;
import ink.yowyob.geofence.repository.OrganizationRepository;
import ink.yowyob.geofence.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public TenantListResponse getAllTenants() {
        List<Organization> organizations = organizationRepository.findAll();
        List<TenantDTOResponse> tenants = organizations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new TenantListResponse(tenants, tenants.size());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDTOResponse getTenantById(UUID tenantId) {
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trouvé avec l'ID: " + tenantId));
        return mapToResponse(organization);
    }

    @Override
    @Transactional
    public TenantDTOResponse createTenant(TenantDTORequest request) {
        // Générer une clé API unique
        String apiKey = generateApiKey();

        Organization organization = new Organization();
        organization.setName(request.name());
        organization.setDomain(request.domain());
        organization.setApiKey(apiKey);
        organization.setContactEmail(request.contactEmail());
        organization.setWebhookUrl(request.webhookUrl());
        organization.setActive(true);
        organization.setInternal(false); // Les tenants créés via l'API ne sont pas internes
        organization.setSubscriptionPlan(request.subscriptionPlan());
        organization.setMaxUsers(request.maxUsers());
        organization.setMaxVehicles(request.maxVehicles());
        organization.setMaxGeofences(request.maxGeofences());
        organization.setRateLimitPerHour(request.rateLimitPerHour());

        Organization savedOrganization = organizationRepository.save(organization);
        log.info("Created new tenant: {} (ID: {}, API Key: {})", 
                savedOrganization.getName(), savedOrganization.getId(), savedOrganization.getApiKey());

        return mapToResponse(savedOrganization);
    }

    @Override
    @Transactional
    public TenantDTOResponse updateTenant(UUID tenantId, TenantDTORequest request) {
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trouvé avec l'ID: " + tenantId));

        // Ne pas permettre la modification d'un tenant interne
        if (organization.isInternal()) {
            throw new IllegalStateException("Impossible de modifier un tenant interne");
        }

        organization.setName(request.name());
        organization.setDomain(request.domain());
        organization.setContactEmail(request.contactEmail());
        organization.setWebhookUrl(request.webhookUrl());
        organization.setSubscriptionPlan(request.subscriptionPlan());
        organization.setMaxUsers(request.maxUsers());
        organization.setMaxVehicles(request.maxVehicles());
        organization.setMaxGeofences(request.maxGeofences());
        organization.setRateLimitPerHour(request.rateLimitPerHour());

        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("Updated tenant: {} (ID: {})", updatedOrganization.getName(), updatedOrganization.getId());

        return mapToResponse(updatedOrganization);
    }

    @Override
    @Transactional
    public void deactivateTenant(UUID tenantId) {
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trouvé avec l'ID: " + tenantId));

        if (organization.isInternal()) {
            throw new IllegalStateException("Impossible de désactiver un tenant interne");
        }

        organization.setActive(false);
        organizationRepository.save(organization);
        log.info("Deactivated tenant: {} (ID: {})", organization.getName(), organization.getId());
    }

    @Override
    @Transactional
    public TenantDTOResponse activateTenant(UUID tenantId) {
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trouvé avec l'ID: " + tenantId));

        organization.setActive(true);
        Organization activatedOrganization = organizationRepository.save(organization);
        log.info("Activated tenant: {} (ID: {})", activatedOrganization.getName(), activatedOrganization.getId());

        return mapToResponse(activatedOrganization);
    }

    @Override
    @Transactional
    public void deleteTenant(UUID tenantId) {
        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant non trouvé avec l'ID: " + tenantId));

        if (organization.isInternal()) {
            throw new IllegalStateException("Impossible de supprimer un tenant interne");
        }

        organizationRepository.delete(organization);
        log.warn("Permanently deleted tenant: {} (ID: {})", organization.getName(), organization.getId());
    }

    /**
     * Mappe une entité Organization vers un DTO TenantDTOResponse
     */
    private TenantDTOResponse mapToResponse(Organization organization) {
        return new TenantDTOResponse(
                organization.getId(),
                organization.getName(),
                organization.getDomain(),
                organization.getApiKey(),
                organization.getContactEmail(),
                organization.getWebhookUrl(),
                organization.isActive(),
                organization.isInternal(),
                organization.getSubscriptionPlan(),
                organization.getMaxUsers(),
                organization.getMaxVehicles(),
                organization.getMaxGeofences(),
                organization.getRateLimitPerHour(),
                organization.getCreatedAt(),
                organization.getLastApiCall()
        );
    }

    /**
     * Génère une clé API unique pour un tenant
     */
    private String generateApiKey() {
        return "org_" + UUID.randomUUID().toString().replace("-", "");
    }
}
