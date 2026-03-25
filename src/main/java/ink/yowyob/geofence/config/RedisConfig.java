package ink.yowyob.geofence.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Configuration du CacheManager avec Redis
     * Définit les stratégies de cache et TTL par type de données
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configuration par défaut
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // TTL par défaut: 10 minutes
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer())
                )
                .disableCachingNullValues(); // Ne pas cacher les valeurs nulles

        // Configurations spécifiques par cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Geofences: cache 5 minutes
        cacheConfigurations.put("geofences", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("geofenceDetail", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("sharedGeofences", defaultConfig.entryTtl(Duration.ofMinutes(3)));

        // Vehicles: cache 5 minutes
        cacheConfigurations.put("vehicles", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("vehicleDetail", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Routes: cache 5 minutes
        cacheConfigurations.put("routes", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("routeDetail", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Alerts: cache court (30 secondes) - données temps réel
        cacheConfigurations.put("alerts", defaultConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigurations.put("unreadAlerts", defaultConfig.entryTtl(Duration.ofSeconds(30)));

        // Statistics: cache long (15 minutes) - calculs coûteux
        cacheConfigurations.put("userStatistics", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("vehicleStatistics", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("systemStatistics", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Dashboard: cache 2 minutes
        cacheConfigurations.put("dashboardStats", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Users: cache 10 minutes
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("userDetail", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Locations: cache 1 minute (données fréquemment mises à jour)
        cacheConfigurations.put("vehicleLocations", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * RedisTemplate pour opérations Redis personnalisées
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Sérialisation des clés en String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Sérialisation des valeurs en JSON
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Créer un sérialiseur JSON pour Redis avec support des types
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Support des dates Java 8+
        objectMapper.registerModule(new JavaTimeModule());

        // Activer le type polymorphique pour gérer l'héritage (GeofenceZone, etc.)
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
