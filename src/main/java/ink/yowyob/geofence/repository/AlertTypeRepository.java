package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.Enum.AlertTypeEnum;
import ink.yowyob.geofence.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertTypeRepository extends JpaRepository <AlertType, Long> {
    Optional<AlertType> findByType(AlertTypeEnum type);
}