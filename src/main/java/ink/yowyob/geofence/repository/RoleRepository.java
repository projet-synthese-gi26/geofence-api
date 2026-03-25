package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.Enum.UserRole;
import ink.yowyob.geofence.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(UserRole name);
}
