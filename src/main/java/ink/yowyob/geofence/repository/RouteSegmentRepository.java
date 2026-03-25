package ink.yowyob.geofence.repository;

import ink.yowyob.geofence.model.Route;
import ink.yowyob.geofence.model.RouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RouteSegmentRepository extends JpaRepository<RouteSegment, UUID> {
    
    List<RouteSegment> findByRoute(Route route);
    
    List<RouteSegment> findByRouteAndIsActive(Route route, Boolean isActive);
    
    @Query("SELECT rs FROM RouteSegment rs WHERE rs.route = :route AND rs.isActive = true ORDER BY rs.segmentOrder ASC, rs.priority DESC")
    List<RouteSegment> findActiveSegmentsByRouteOrderedByOrderAndPriority(@Param("route") Route route);
    
    @Query("SELECT rs FROM RouteSegment rs WHERE rs.route = :route AND rs.segmentType = :segmentType AND rs.isActive = true")
    List<RouteSegment> findActiveSegmentsByRouteAndType(@Param("route") Route route, @Param("segmentType") RouteSegment.RouteSegmentType segmentType);
    
    @Query("SELECT rs FROM RouteSegment rs WHERE rs.route.id = :routeId AND rs.isActive = true ORDER BY rs.priority DESC")
    List<RouteSegment> findActiveSegmentsByRouteIdOrderedByPriority(@Param("routeId") UUID routeId);
    
    @Query("SELECT SUM(rs.segmentLength) FROM RouteSegment rs WHERE rs.route = :route AND rs.isActive = true")
    Double getTotalLengthByRoute(@Param("route") Route route);
    
    @Query("SELECT SUM(rs.estimatedTime) FROM RouteSegment rs WHERE rs.route = :route AND rs.isActive = true")
    Integer getTotalEstimatedTimeByRoute(@Param("route") Route route);
}