package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByApiKey(String apiKey);
}
