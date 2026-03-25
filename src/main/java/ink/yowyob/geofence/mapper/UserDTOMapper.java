package ink.yowyob.geofence.mapper;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.dto.response.UserDTO;
import ink.yowyob.geofence.model.User;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class UserDTOMapper implements Function <User, UserDTO> {
    @Override
    public UserDTO apply(User user) {
        // Ensure the role is loaded to avoid LazyInitializationException
        UserRole roleName = null;
        if (user.getRole() != null && Hibernate.isInitialized(user.getRole())) {
            roleName = user.getRole().getName();
        } else if (user.getRole() != null) {
            // Force initialization if in active transaction
            try {
                roleName = user.getRole().getName();
            } catch (Exception e) {
                // If lazy loading fails, set a default or handle gracefully
                roleName = UserRole.USER;
            }
        }
        
        return new UserDTO(
                user.getUuid(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getDOB(),
                roleName
        );
    }
}
