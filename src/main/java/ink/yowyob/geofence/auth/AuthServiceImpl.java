package ink.yowyob.geofence.auth;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.request.AuthRequest.*;
import ink.yowyob.geofence.dto.response.AuthResponse;
import ink.yowyob.geofence.dto.response.RegisterResponse;
import ink.yowyob.geofence.dto.response.UserDTO;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.exception.PasswordMismatchException;
import ink.yowyob.geofence.exception.UserAlreadyExistsException;
import ink.yowyob.geofence.model.Organization;
import ink.yowyob.geofence.model.Role;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.OrganizationRepository;
import ink.yowyob.geofence.repository.RoleRepository;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;

    @Value("${app.multi-tenant.default-tenant-id:default}")
    private String defaultTenantId;

    @Override
    public RegisterResponse register(RegisterDTO registerDTO, String tenantId) {
        String effectiveTenantId = resolveTenantId(tenantId);
        Organization organization = resolveOrganizationOrThrow(effectiveTenantId);

        Optional<User> user = this.userRepository.findByTenantAndIdentifiers(
                effectiveTenantId,
                registerDTO.email(),
                registerDTO.phoneNumber(),
                registerDTO.username()
        );

        if (user.isPresent()) {
            User existingUser = user.get();
            if (existingUser.getEmail().equals(registerDTO.email())) {
                throw new UserAlreadyExistsException("Un utilisateur avec cet email existe déjà.");
            } else if (existingUser.getPhoneNumber().equals(registerDTO.phoneNumber())) {
                throw new UserAlreadyExistsException("Un utilisateur avec ce numéro de téléphone existe déjà.");
            } else if (existingUser.getUsername().equals(registerDTO.username())) {
                throw new UserAlreadyExistsException("Un utilisateur avec ce nom d'utilisateur existe déjà.");
            }
        }

        if(!Objects.equals(registerDTO.password_confirmation(), registerDTO.password())) {
            throw new PasswordMismatchException();
        }

        User newUser = new User();
        newUser.setUsername(registerDTO.username());
        newUser.setEmail(registerDTO.email());
        newUser.setPhoneNumber(registerDTO.phoneNumber());
        newUser.setPassword(this.passwordEncoder.encode(registerDTO.password()));
        newUser.setDOB(registerDTO.DOB());
        newUser.setFirstname(registerDTO.firstname());
        newUser.setLastname(registerDTO.lastname());
        newUser.setOrganization(organization);

        Role userRole = roleRepository.findByName(UserRole.USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(UserRole.USER);
                    return roleRepository.save(newRole);
                });
        newUser.setRole(userRole);
        newUser.setEnabled(true); // Activé par défaut pour simplifier

        User saveUser = userRepository.save(newUser);
        return new RegisterResponse(
                true,
                "utilisateur enregistrer avec succes",
                new UserDTO(
                        saveUser.getUuid(),
                        saveUser.getUsername(),
                        saveUser.getFirstname(),
                        saveUser.getLastname(),
                        saveUser.getPhoneNumber(),
                        saveUser.getEmail(),
                        saveUser.getDOB(),
                        saveUser.getRole().getName()
                )
        );
    }

    @Override
    public AuthResponse login(LoginDTO loginDTO, String tenantId) {
        String effectiveTenantId = resolveTenantId(tenantId);
        User user;
        switch (loginDTO) {
            case LoginUsernameDTO loginUsernameDTO -> user = loginUsername(loginUsernameDTO, effectiveTenantId);
            case LoginEmailDTO loginEmailDTO -> user = loginEmail(loginEmailDTO, effectiveTenantId);
            case LoginPhoneNumberDTO loginPhoneNumberDTO -> user = loginPhone(loginPhoneNumberDTO, effectiveTenantId);
            case null, default -> throw new BadCredentialsException("the provided information are not good verify it and try again");
        }

        String token = jwtService.generate(user.getUsername()).get("bearer");

        return new AuthResponse(
                new UserDTO(
                        user.getUuid(),
                        user.getUsername(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getPhoneNumber(),
                        user.getEmail(),
                        user.getDOB(),
                        user.getRole().getName()
                ),
                token);
    }

    @Override
    public User loginEmail(LoginEmailDTO loginEmailDTO, String tenantId) {
        Optional<User> user = userRepository.findByEmailAndOrganization_ApiKey(loginEmailDTO.email(), tenantId);

        if (user.isEmpty() || !this.passwordEncoder.matches(loginEmailDTO.password(), user.get().getPassword())) {
            throw new BadCredentialsException("the provided information are not good verify it and try again");
        }
        return ensureUserHasOrganization(user.get(), tenantId);
    }

    @Override
    public User loginUsername(LoginUsernameDTO loginUsernameDTO, String tenantId) {
        Optional<User> user = userRepository.findByUsernameAndOrganization_ApiKey(loginUsernameDTO.username(), tenantId);

        if (user.isEmpty() || !this.passwordEncoder.matches(loginUsernameDTO.password(), user.get().getPassword())) {
            throw new BadCredentialsException("the provided information are not good verify it and try again");
        }
        return ensureUserHasOrganization(user.get(), tenantId);
    }

    @Override
    public User loginPhone(LoginPhoneNumberDTO loginPhoneNumberDTO, String tenantId) {
        Optional<User> user = userRepository.findByPhoneNumberAndOrganization_ApiKey(loginPhoneNumberDTO.phoneNumber(), tenantId);

        if (user.isEmpty() || !this.passwordEncoder.matches(loginPhoneNumberDTO.password(), user.get().getPassword())) {
            throw new BadCredentialsException("the provided information are not good verify it and try again");
        }
        return ensureUserHasOrganization(user.get(), tenantId);
    }

    @Override
    public AuthResponse getCurrentUser(User user) {
        // Récupérer le nom d'utilisateur depuis le contexte de sécurité
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//
//        // Charger l'utilisateur complet depuis la base de données
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // Générer un nouveau token JWT
        String token = jwtService.generate(user.getUsername()).get("bearer");

        // Créer et retourner la réponse contenant les détails utilisateur et le nouveau token
        return new AuthResponse(
                new UserDTO(
                        user.getUuid(),
                        user.getUsername(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getPhoneNumber(),
                        user.getEmail(),
                        user.getDOB(),
                        user.getRole().getName()
                ),
                token
        );
    }

    private String resolveTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return defaultTenantId;
        }
        return tenantId.trim();
    }

    private Organization resolveOrganizationOrThrow(String tenantId) {
        return organizationRepository.findByApiKey(tenantId)
                .orElseThrow(() -> new BadCredentialsException("Tenant introuvable. Contactez un administrateur pour créer ce tenant."));
    }

    private User ensureUserHasOrganization(User user, String tenantId) {
        if (user.getOrganization() != null) {
            return user;
        }
        user.setOrganization(resolveOrganizationOrThrow(tenantId));
        return userRepository.save(user);
    }
}