package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
   Optional <User> findByUsername(String username);
   
   @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
   Optional<User> findByUsernameWithRole(@Param("username") String username);

   Optional<User> findByUsernameAndOrganization_ApiKey(String username, String tenantId);
   Optional<User> findByEmailAndOrganization_ApiKey(String email, String tenantId);
   Optional<User> findByPhoneNumberAndOrganization_ApiKey(String phoneNumber, String tenantId);
   List<User> findByOrganization_Id(UUID organizationId);
   long countByOrganization_Id(UUID organizationId);
   Optional<User> findByUuidAndOrganization_Id(UUID uuid, UUID organizationId);

   @Query("SELECT u FROM User u WHERE u.organization.apiKey = :tenantId AND (u.email = :email OR u.phoneNumber = :phoneNumber OR u.username = :username)")
   Optional<User> findByTenantAndIdentifiers(@Param("tenantId") String tenantId,
                                             @Param("email") String email,
                                             @Param("phoneNumber") String phoneNumber,
                                             @Param("username") String username);
   
   Optional <User> findByEmail(String email);
   Optional <User> findByPhoneNumber(String phoneNumber);
   Optional <User> findByEmailOrPhoneNumberOrUsername(String email, String phoneNumber, String username);
}
