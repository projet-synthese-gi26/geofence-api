
package ink.yowyob.geofence.config;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.model.Organization;
import ink.yowyob.geofence.model.Role;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.OrganizationRepository;
import ink.yowyob.geofence.repository.RoleRepository;
import ink.yowyob.geofence.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapDataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.multi-tenant.default-tenant-id:frontend-app}")
    private String frontendTenantId;

    @Value("${app.multi-tenant.default-tenant-name:Frontend Application}")
    private String frontendTenantName;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.email:admin@frontend.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.phone:+237600000000}")
    private String adminPhone;

    @Value("${app.bootstrap.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.firstname:Frontend}")
    private String adminFirstname;

    @Value("${app.bootstrap.admin.lastname:Admin}")
    private String adminLastname;

    @Value("${app.bootstrap.admin.dob:1990-01-01}")
    private String adminDob;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🚀 Démarrage du bootstrap de l'application...");

        ensureRoleExists(UserRole.USER);
        ensureRoleExists(UserRole.MANAGER);
        Role adminRole = ensureRoleExists(UserRole.ADMIN);

        Organization frontendTenant = ensureFrontendTenant();
        ensureDefaultAdmin(frontendTenant, adminRole);

        log.info("✅ Bootstrap terminé avec succès");
    }

    private Role ensureRoleExists(UserRole roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    log.info("✓ Création du rôle: {}", roleName);
                    return roleRepository.save(role);
                });
    }

    private Organization ensureFrontendTenant() {
        return organizationRepository.findByApiKey(frontendTenantId)
                .orElseGet(() -> {
                    Organization organization = new Organization();
                    organization.setApiKey(frontendTenantId);
                    organization.setName(frontendTenantName);
                    organization.setDomain("localhost");
                    organization.setContactEmail(adminEmail);
                    organization.setActive(true);
                    organization.setInternal(true);
                    organization.setSubscriptionPlan("UNLIMITED");
                    organization.setMaxUsers(Integer.MAX_VALUE);
                    organization.setMaxVehicles(Integer.MAX_VALUE);
                    organization.setMaxGeofences(Integer.MAX_VALUE);
                    organization.setRateLimitPerHour(Integer.MAX_VALUE);
                    log.info("🚀 Création du tenant frontend: {} (API Key: {})", frontendTenantName, frontendTenantId);
                    return organizationRepository.save(organization);
                });
    }

    private void ensureDefaultAdmin(Organization organization, Role adminRole) {
        User adminUser = userRepository
                .findByUsernameAndOrganization_ApiKey(adminUsername, organization.getApiKey())
                .orElseGet(() -> createAdminUser(organization, adminRole));

        boolean updated = false;
        if (adminUser.getRole() == null || adminUser.getRole().getName() != UserRole.ADMIN) {
            adminUser.setRole(adminRole);
            updated = true;
        }
        if (!adminUser.isEnabled()) {
            adminUser.setEnabled(true);
            updated = true;
        }
        if (adminUser.getOrganization() == null) {
            adminUser.setOrganization(organization);
            updated = true;
        }

        if (updated) {
            userRepository.save(adminUser);
        }
    }

    private User createAdminUser(Organization organization, Role adminRole) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            throw new IllegalStateException("Impossible de créer l'admin par défaut: email déjà utilisé (" + adminEmail + ")");
        }
        if (userRepository.findByPhoneNumber(adminPhone).isPresent()) {
            throw new IllegalStateException("Impossible de créer l'admin par défaut: téléphone déjà utilisé (" + adminPhone + ")");
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPhoneNumber(adminPhone);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setDOB(LocalDate.parse(adminDob));
        admin.setFirstname(adminFirstname);
        admin.setLastname(adminLastname);
        admin.setOrganization(organization);
        admin.setRole(adminRole);
        admin.setEnabled(true);
        admin.setAccountNonExpired(true);
        admin.setAccountNonLocked(true);
        admin.setCredentialsNonExpired(true);

        log.info("✓ Création de l'administrateur par défaut: {} (email: {})", adminUsername, adminEmail);
        log.info("  → Mot de passe par défaut: {}", adminPassword);
        log.warn("⚠️  IMPORTANT: Veuillez changer le mot de passe admin après la première connexion!");
        return userRepository.save(admin);
    }
}
