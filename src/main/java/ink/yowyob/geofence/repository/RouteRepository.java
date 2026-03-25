package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.Route;
import ink.yowyob.geofence.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles " +
           "WHERE r.user = :user")
    List<Route> findByUser(@Param("user") User user);
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles " +
           "WHERE r.user = :user AND r.isActive = :isActive")
    List<Route> findByUserAndIsActive(@Param("user") User user, @Param("isActive") Boolean isActive);
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles " +
           "WHERE r.isActive = :isActive")
    List<Route> findByIsActive(@Param("isActive") Boolean isActive);
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles " +
           "WHERE r.user = :user AND r.isActive = true")
    List<Route> findActiveRoutesByUser(@Param("user") User user);
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "JOIN FETCH r.assignedVehicles v " +
           "WHERE v.id = :vehicleId AND r.isActive = true")
    List<Route> findActiveRoutesByVehicleId(@Param("vehicleId") UUID vehicleId);
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles " +
           "WHERE r.user = :user AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.startAddress) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.endAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Route> searchUserRoutes(@Param("user") User user, @Param("keyword") String keyword);
    
    @Query("SELECT COUNT(r) FROM Route r WHERE r.user = :user AND r.isActive = true")
    Long countActiveRoutesByUser(@Param("user") User user);
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles " +
           "WHERE r.id = :id")
    Optional<Route> findByIdWithUserAndRole(@Param("id") UUID id);
    
    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles")
    List<Route> findAllWithUserAndRole();

    @Query("SELECT r FROM Route r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH u.role " +
           "LEFT JOIN FETCH r.activeDays " +
           "LEFT JOIN FETCH r.authorizedSegments " +
           "LEFT JOIN FETCH r.assignedVehicles " +
           "WHERE u.organization.id = :organizationId")
    List<Route> findByUser_Organization_IdWithUserAndRole(@Param("organizationId") UUID organizationId);
}