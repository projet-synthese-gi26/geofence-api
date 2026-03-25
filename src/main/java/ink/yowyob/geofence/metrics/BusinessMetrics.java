package ink.yowyob.geofence.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Métriques métier personnalisées pour GeoFence
 * Exportées vers Prometheus pour monitoring dans Grafana
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    // Compteurs
    private final Counter geofenceCreatedCounter;
    private final Counter geofenceDeletedCounter;
    private final Counter vehicleCreatedCounter;
    private final Counter vehicleDeletedCounter;
    private final Counter alertCreatedCounter;
    private final Counter routeDeviationCounter;
    private final Counter zoneViolationCounter;

    // Jauges (valeurs instantanées)
    private final AtomicInteger activeGeofencesCount = new AtomicInteger(0);
    private final AtomicInteger activeVehiclesCount = new AtomicInteger(0);
    private final AtomicInteger unreadAlertsCount = new AtomicInteger(0);

    // Timers
    private final Timer geofenceCheckTimer;
    private final Timer routeDeviationCheckTimer;
    private final Timer databaseQueryTimer;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialisation des compteurs
        this.geofenceCreatedCounter = Counter.builder("geofence.created.total")
                .description("Total number of geofences created")
                .register(meterRegistry);

        this.geofenceDeletedCounter = Counter.builder("geofence.deleted.total")
                .description("Total number of geofences deleted")
                .register(meterRegistry);

        this.vehicleCreatedCounter = Counter.builder("vehicle.created.total")
                .description("Total number of vehicles created")
                .register(meterRegistry);

        this.vehicleDeletedCounter = Counter.builder("vehicle.deleted.total")
                .description("Total number of vehicles deleted")
                .register(meterRegistry);

        this.alertCreatedCounter = Counter.builder("alert.created.total")
                .description("Total number of alerts created")
                .tag("type", "all")
                .register(meterRegistry);

        this.routeDeviationCounter = Counter.builder("route.deviation.total")
                .description("Total number of route deviations detected")
                .register(meterRegistry);

        this.zoneViolationCounter = Counter.builder("zone.violation.total")
                .description("Total number of zone violations")
                .register(meterRegistry);

        // Initialisation des jauges
        Gauge.builder("geofence.active.count", activeGeofencesCount, AtomicInteger::get)
                .description("Current number of active geofences")
                .register(meterRegistry);

        Gauge.builder("vehicle.active.count", activeVehiclesCount, AtomicInteger::get)
                .description("Current number of active vehicles")
                .register(meterRegistry);

        Gauge.builder("alert.unread.count", unreadAlertsCount, AtomicInteger::get)
                .description("Current number of unread alerts")
                .register(meterRegistry);

        // Initialisation des timers
        this.geofenceCheckTimer = Timer.builder("geofence.check.duration")
                .description("Time taken to check geofence boundaries")
                .register(meterRegistry);

        this.routeDeviationCheckTimer = Timer.builder("route.deviation.check.duration")
                .description("Time taken to check route deviations")
                .register(meterRegistry);

        this.databaseQueryTimer = Timer.builder("database.query.duration")
                .description("Time taken for database queries")
                .register(meterRegistry);
    }

    // === Méthodes pour incrémenter les compteurs ===

    public void incrementGeofenceCreated() {
        geofenceCreatedCounter.increment();
        log.debug("Metric: Geofence created counter incremented");
    }

    public void incrementGeofenceDeleted() {
        geofenceDeletedCounter.increment();
        log.debug("Metric: Geofence deleted counter incremented");
    }

    public void incrementVehicleCreated() {
        vehicleCreatedCounter.increment();
        log.debug("Metric: Vehicle created counter incremented");
    }

    public void incrementVehicleDeleted() {
        vehicleDeletedCounter.increment();
        log.debug("Metric: Vehicle deleted counter incremented");
    }

    public void incrementAlertCreated(String alertType) {
        Counter.builder("alert.created.total")
                .description("Total number of alerts created by type")
                .tag("type", alertType)
                .register(meterRegistry)
                .increment();
        alertCreatedCounter.increment();
        log.debug("Metric: Alert created - type: {}", alertType);
    }

    public void incrementRouteDeviation() {
        routeDeviationCounter.increment();
        log.debug("Metric: Route deviation detected");
    }

    public void incrementZoneViolation(String violationType) {
        Counter.builder("zone.violation.total")
                .description("Total number of zone violations by type")
                .tag("violation_type", violationType)
                .register(meterRegistry)
                .increment();
        zoneViolationCounter.increment();
        log.debug("Metric: Zone violation - type: {}", violationType);
    }

    // === Méthodes pour mettre à jour les jauges ===

    public void setActiveGeofencesCount(int count) {
        activeGeofencesCount.set(count);
        log.debug("Metric: Active geofences count set to {}", count);
    }

    public void setActiveVehiclesCount(int count) {
        activeVehiclesCount.set(count);
        log.debug("Metric: Active vehicles count set to {}", count);
    }

    public void setUnreadAlertsCount(int count) {
        unreadAlertsCount.set(count);
        log.debug("Metric: Unread alerts count set to {}", count);
    }

    // === Méthodes pour les timers ===

    public Timer.Sample startGeofenceCheckTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopGeofenceCheckTimer(Timer.Sample sample) {
        sample.stop(geofenceCheckTimer);
    }

    public Timer.Sample startRouteDeviationCheckTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopRouteDeviationCheckTimer(Timer.Sample sample) {
        sample.stop(routeDeviationCheckTimer);
    }

    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopDatabaseQueryTimer(Timer.Sample sample) {
        sample.stop(databaseQueryTimer);
    }

    // === Métriques de cache Redis ===

    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hit.total")
                .description("Total cache hits")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.miss.total")
                .description("Total cache misses")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }
}
