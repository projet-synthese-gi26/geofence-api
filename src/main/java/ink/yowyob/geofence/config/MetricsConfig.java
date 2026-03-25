package ink.yowyob.geofence.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration des métriques custom pour Prometheus/Grafana
 */
@Configuration
public class MetricsConfig {

    /**
     * Configuration globale des tags pour toutes les métriques
     */
    @Bean
    public MeterBinder commonTags() {
        return (registry) -> registry.config().commonTags(
                Arrays.asList(
                        Tag.of("application", "geofence"),
                        Tag.of("instance", getHostname())
                )
        );
    }

    /**
     * Récupérer le hostname pour identifier l'instance
     */
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
