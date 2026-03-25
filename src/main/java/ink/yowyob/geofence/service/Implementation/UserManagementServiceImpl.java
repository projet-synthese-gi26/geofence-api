package ink.yowyob.geofence.service.Implementation;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.request.UpdateUserRoleRequest;
import ink.yowyob.geofence.dto.response.UserDTO;
import ink.yowyob.geofence.dto.response.UserListResponse;
import ink.yowyob.geofence.exception.BadCredentialsException;
import ink.yowyob.geofence.mapper.UserDTOMapper;
import ink.yowyob.geofence.model.Role;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.RoleRepository;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.UserManagementService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDTOMapper userDTOMapper;

    @Override
    public UserListResponse getAllUsers(Integer page, Integer size, User currentUser) {
        // Check if current user is admin

        if (!Objects.equals(currentUser.getRole().getName(), UserRole.ADMIN)) {
            throw new BadCredentialsException("Only admin users can view all users");
        }

        List<User> users = userRepository.findByOrganization_Id(currentUser.getOrganization().getId());
        List<UserDTO> userDTOs = users.stream()
                .map(userDTOMapper)
                .collect(Collectors.toList());

        return new UserListResponse(userDTOs, userDTOs.size());
    }

    @Override
    public UserListResponse getUsersForSharing(User currentUser) {
        // Cette méthode est accessible à tous les utilisateurs authentifiés;

        // Récupérer tous les utilisateurs sauf l'utilisateur actuel
        List<User> users = userRepository.findByOrganization_Id(currentUser.getOrganization().getId()).stream()
                .filter(user -> !Objects.equals(user.getUuid(), currentUser.getUuid()))
                .collect(Collectors.toList());

        List<UserDTO> userDTOs = users.stream()
                .map(userDTOMapper)
                .collect(Collectors.toList());

        return new UserListResponse(userDTOs, userDTOs.size());
    }

    @Override
    public UserDTO getUserById(UUID id, User admin) {
        // Check if current user is admin

        if (!Objects.equals(admin.getRole().getName(), UserRole.ADMIN)) {
            throw new BadCredentialsException("Only admin users can view user details");
        }

        User user = userRepository.findByUuidAndOrganization_Id(id, admin.getOrganization().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User with id " + id + " not found"));

        return userDTOMapper.apply(user);
    }

    @Override
    public UserDTO updateUserRole(UUID id, UpdateUserRoleRequest request, User admin) {
        // Check if current user is admin

        if (!Objects.equals(admin.getRole().getName(), UserRole.ADMIN)) {
            throw new BadCredentialsException("Only admin users can update user roles");
        }

        User user = userRepository.findByUuidAndOrganization_Id(id, admin.getOrganization().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User with id " + id + " not found"));

        // Get the role from the repository
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + request.role()));

        // Update the user's role
        user.setRole(role);
        User updatedUser = userRepository.save(user);

        return userDTOMapper.apply(updatedUser);
    }

    @Override
    public void deleteUser(UUID id, User admin) {

        if (!Objects.equals(admin.getRole().getName(), UserRole.ADMIN)) {
            throw new BadCredentialsException("Only admin users can delete users");
        }

        // Check if the user exists
        User user = userRepository.findByUuidAndOrganization_Id(id, admin.getOrganization().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User with id " + id + " not found"));

        // Delete the user
        userRepository.delete(user);
    }
}