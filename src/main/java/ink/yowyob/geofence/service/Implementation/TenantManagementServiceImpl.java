package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.request.CreateTenantRequest;
import ink.yowyob.geofence.dto.request.UpdateTenantRequest;
import ink.yowyob.geofence.dto.response.TenantDTOResponse;
import ink.yowyob.geofence.dto.response.TenantListResponse;
import ink.yowyob.geofence.dto.response.TenantResponse;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.model.Organization;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.OrganizationRepository;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.TenantManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantManagementServiceImpl implements TenantManagementService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Value("${app.multi-tenant.default-tenant-id:frontend-app}")
    private String frontendTenantId;

    @Override
    public TenantListResponse getAllTenants(User actor) {
        assertAdmin(actor);
        List<TenantDTOResponse> tenants = organizationRepository.findAll()
                .stream()
                .map(this::toTenantDTOResponse)
                .toList();

        return new TenantListResponse(tenants, tenants.size());
    }

    @Override
    public TenantResponse getTenantByApiKey(String apiKey, User actor) {
        assertAdmin(actor);
        Organization organization = findOrganizationOrThrow(apiKey);
        return toTenantResponse(organization);
    }

    @Override
    public TenantResponse createTenant(CreateTenantRequest request, User actor) {
        assertAdmin(actor);

        String apiKey = normalizeRequired(request.apiKey(), "apiKey");
        String name = normalizeRequired(request.name(), "name");

        if (organizationRepository.findByApiKey(apiKey).isPresent()) {
            throw new BadCredentialsException("Un tenant avec cet apiKey existe déjà.");
        }

        Organization organization = new Organization();
        organization.setApiKey(apiKey);
        organization.setName(name);
        organization.setDomain(normalizeOptional(request.domain()));
        organization.setContactEmail(normalizeOptional(request.contactEmail()));
        organization.setWebhookUrl(normalizeOptional(request.webhookUrl()));

        if (request.subscriptionPlan() != null && !request.subscriptionPlan().isBlank()) {
            organization.setSubscriptionPlan(request.subscriptionPlan().trim());
        }
        if (request.maxUsers() != null) {
            organization.setMaxUsers(request.maxUsers());
        }
        if (request.maxVehicles() != null) {
            organization.setMaxVehicles(request.maxVehicles());
        }
        if (request.maxGeofences() != null) {
            organization.setMaxGeofences(request.maxGeofences());
        }
        if (request.rateLimitPerHour() != null) {
            organization.setRateLimitPerHour(request.rateLimitPerHour());
        }

        return toTenantResponse(organizationRepository.save(organization));
    }

    @Override
    public TenantResponse updateTenant(String apiKey, UpdateTenantRequest request, User actor) {
        assertAdmin(actor);

        Organization organization = findOrganizationOrThrow(apiKey);

        if (request.name() != null && !request.name().isBlank()) {
            organization.setName(request.name().trim());
        }
        if (request.domain() != null) {
            organization.setDomain(normalizeOptional(request.domain()));
        }
        if (request.contactEmail() != null) {
            organization.setContactEmail(normalizeOptional(request.contactEmail()));
        }
        if (request.webhookUrl() != null) {
            organization.setWebhookUrl(normalizeOptional(request.webhookUrl()));
        }
        if (request.active() != null) {
            organization.setActive(request.active());
        }
        if (request.subscriptionPlan() != null && !request.subscriptionPlan().isBlank()) {
            organization.setSubscriptionPlan(request.subscriptionPlan().trim());
        }
        if (request.maxUsers() != null) {
            organization.setMaxUsers(request.maxUsers());
        }
        if (request.maxVehicles() != null) {
            organization.setMaxVehicles(request.maxVehicles());
        }
        if (request.maxGeofences() != null) {
            organization.setMaxGeofences(request.maxGeofences());
        }
        if (request.rateLimitPerHour() != null) {
            organization.setRateLimitPerHour(request.rateLimitPerHour());
        }

        return toTenantResponse(organizationRepository.save(organization));
    }

    @Override
    public void deleteTenant(String apiKey, User actor) {
        assertAdmin(actor);

        Organization organization = findOrganizationOrThrow(apiKey);
        if (Objects.equals(organization.getApiKey(), frontendTenantId)) {
            throw new BadCredentialsException("Le tenant frontend par défaut ne peut pas être supprimé.");
        }

        long usersCount = userRepository.countByOrganization_Id(organization.getId());
        if (usersCount > 0) {
            throw new BadCredentialsException("Impossible de supprimer ce tenant: des utilisateurs y sont rattachés.");
        }

        organizationRepository.delete(organization);
    }

    private void assertAdmin(User actor) {
        if (actor.getRole() == null || actor.getRole().getName() != UserRole.ADMIN) {
            throw new BadCredentialsException("Seuls les ADMIN peuvent gérer les tenants.");
        }
    }

    private Organization findOrganizationOrThrow(String apiKey) {
        return organizationRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new UsernameNotFoundException("Tenant introuvable: " + apiKey));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadCredentialsException("Le champ " + fieldName + " est obligatoire.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TenantResponse toTenantResponse(Organization organization) {
        return new TenantResponse(
                organization.getId(),
                organization.getApiKey(),
                organization.getName(),
                organization.getDomain(),
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

    private TenantDTOResponse toTenantDTOResponse(Organization organization) {
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
}
