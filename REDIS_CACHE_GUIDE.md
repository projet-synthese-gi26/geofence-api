# Guide du Cache Redis - GeoFence Backend

## 📋 Table des matières
- [Introduction](#introduction)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Stratégies de cache](#stratégies-de-cache)
- [Utilisation](#utilisation)
- [Annotations disponibles](#annotations-disponibles)
- [Monitoring](#monitoring)
- [Best Practices](#best-practices)

---

## Introduction

Le cache Redis a été intégré pour améliorer drastiquement les performances de l'API GeoFence en :
- **Réduisant la charge sur PostgreSQL** (moins de requêtes DB)
- **Accélérant les réponses** (5-10x plus rapide pour données en cache)
- **Scalant horizontalement** (Redis distribué)

---

## Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────────┐
│   Client    │─────▶│  Spring Boot │─────▶│   PostgreSQL    │
│  (Frontend) │      │  + Redis     │      │   + PostGIS     │
└─────────────┘      └──────────────┘      └─────────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │    Redis     │
                     │   (Cache)    │
                     └──────────────┘
```

**Flux de données :**
1. Requête arrive → Spring vérifie Redis
2. Si **Cache HIT** → Retour immédiat depuis Redis (rapide ⚡)
3. Si **Cache MISS** → Requête DB → Stockage dans Redis → Retour

---

## Configuration

### Docker Compose

Redis est configuré dans `docker-compose.yml` :

```yaml
redis:
  image: redis:7-alpine
  container_name: geofence-redis
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
  command: redis-server --appendonly yes  # Persistence AOF
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
```

### Application Properties

Configuration Spring dans `application.properties` :

```properties
# Redis Connection
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.timeout=2000ms

# Connection Pool (Lettuce)
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000  # 10 minutes par défaut
spring.cache.redis.cache-null-values=false
```

---

## Stratégies de cache

Chaque type de données a un **TTL (Time To Live)** adapté :

| Cache | TTL | Justification |
|-------|-----|---------------|
| **geofences** | 5 min | Données modifiées occasionnellement |
| **geofenceDetail** | 10 min | Détails rarement modifiés |
| **sharedGeofences** | 3 min | Partages peuvent changer |
| **vehicles** | 5 min | Flotte stable |
| **vehicleDetail** | 10 min | Détails rarement modifiés |
| **vehicleLocations** | 1 min | Données temps réel |
| **routes** | 5 min | Routes stables |
| **alerts** | 30 sec | Données temps réel |
| **unreadAlerts** | 30 sec | Compteurs temps réel |
| **userStatistics** | 15 min | Calculs coûteux, peu fréquents |
| **vehicleStatistics** | 15 min | Calculs coûteux |
| **systemStatistics** | 15 min | Admin, peu fréquent |
| **dashboardStats** | 2 min | Vue d'ensemble |
| **users** | 10 min | Liste utilisateurs |

---

## Utilisation

### 1. Annotation @Cacheable (Lecture)

Cache le résultat d'une méthode lors du premier appel :

```java
@Cacheable(value = "geofenceDetail", key = "#zoneId + '_' + #type")
public GeofenceZoneDTOResponse getGeofenceZone(UUID zoneId, String type) {
    log.debug("Cache MISS: Fetching geofence {} type {}", zoneId, type);
    // Requête DB coûteuse
    return geofenceFromDatabase;
}
```

**Comportement :**
- 1er appel : DB query + mise en cache
- Appels suivants (< 10 min) : Retour depuis Redis (ultra rapide)

### 2. Annotation @CacheEvict (Invalidation)

Supprime les entrées du cache lors de modifications :

```java
@CacheEvict(value = {"geofences", "geofenceDetail"}, allEntries = true)
public GeofenceZoneDTOResponse createGeofenceZone(...) {
    log.debug("Cache EVICT: Creating new geofence, clearing caches");
    // Création en DB
    return newGeofence;
}
```

**Comportement :**
- Vide complètement les caches `geofences` et `geofenceDetail`
- Les prochains appels feront un Cache MISS et refetcheront depuis DB

### 3. Annotation @CachePut (Mise à jour)

Met à jour une entrée spécifique du cache :

```java
@CachePut(value = "vehicleDetail", key = "#id")
public VehicleDTO updateVehicle(UUID id, UpdateVehicleDTO data) {
    // Update en DB
    VehicleDTO updated = vehicleRepository.save(...);
    // Le résultat remplace l'entrée en cache
    return updated;
}
```

---

## Annotations disponibles

### @Cacheable
```java
@Cacheable(
    value = "nomDuCache",              // Nom du cache
    key = "#parametreId",              // Clé unique
    condition = "#parametreId != null", // Condition pour cacher
    unless = "#result == null"         // Ne pas cacher si résultat null
)
```

### @CacheEvict
```java
@CacheEvict(
    value = {"cache1", "cache2"},  // Plusieurs caches
    key = "#id",                   // Clé spécifique
    allEntries = true,             // Vider tout le cache
    beforeInvocation = false       // Évict après succès de la méthode
)
```

### @CachePut
```java
@CachePut(
    value = "nomDuCache",
    key = "#id"
)
```

### @Caching (Multiple opérations)
```java
@Caching(
    evict = {
        @CacheEvict(value = "cache1", allEntries = true),
        @CacheEvict(value = "cache2", key = "#id")
    }
)
```

---

## Monitoring

### 1. Logs de cache

Les logs Spring montrent les opérations de cache :

```
DEBUG Cache MISS: Fetching geofence 123e4567-e89b-12d3-a456-426614174000 type c
DEBUG Cache HIT: Returning cached geofence
DEBUG Cache EVICT: Creating new geofence, clearing caches
```

### 2. Redis CLI

Connectez-vous à Redis pour inspecter :

```bash
# Connexion au container Redis
docker exec -it geofence-redis redis-cli

# Lister toutes les clés
KEYS *

# Voir une clé spécifique
GET "geofences::all"

# Voir le TTL restant (en secondes)
TTL "geofences::all"

# Nombre de clés en cache
DBSIZE

# Statistiques mémoire
INFO memory

# Vider tout le cache (ATTENTION !)
FLUSHALL
```

### 3. Spring Boot Actuator (à venir Phase 6)

Endpoints de monitoring :
- `/actuator/caches` - Liste des caches
- `/actuator/metrics/cache.*` - Métriques de cache (hit rate, size)

---

## Best Practices

### ✅ DO (À faire)

1. **Cacher les lectures fréquentes** :
   ```java
   @Cacheable(value = "users", key = "#userId")
   public User getUserById(UUID userId) { ... }
   ```

2. **Invalider lors des modifications** :
   ```java
   @CacheEvict(value = "users", allEntries = true)
   public User updateUser(UUID id, UserDTO data) { ... }
   ```

3. **Utiliser des clés uniques** :
   ```java
   @Cacheable(value = "vehicleLocations", key = "#vehicleId + '_' + #startDate + '_' + #endDate")
   ```

4. **TTL adapté aux données** :
   - Données temps réel → 30s-1min
   - Données stables → 5-15min
   - Calculs coûteux → 15-30min

5. **Logger les opérations** :
   ```java
   log.debug("Cache MISS: Fetching data for {}", id);
   ```

### ❌ DON'T (À éviter)

1. **Ne pas cacher les données sensibles non chiffrées** :
   ```java
   // ❌ Mauvais
   @Cacheable("passwords")
   public String getPassword(UUID userId) { ... }
   ```

2. **Ne pas cacher les données très volatiles** :
   ```java
   // ❌ Mauvais - change toutes les secondes
   @Cacheable("currentTimestamp")
   public LocalDateTime getNow() { return LocalDateTime.now(); }
   ```

3. **Ne pas oublier l'invalidation** :
   ```java
   // ❌ Mauvais - pas de @CacheEvict
   public void deleteUser(UUID id) {
       userRepository.deleteById(id);
       // Le cache contient toujours l'ancien utilisateur !
   }
   ```

4. **Ne pas cacher des objets trop gros** :
   ```java
   // ❌ Mauvais - 100 MB de données
   @Cacheable("hugeDataset")
   public List<BigObject> getHugeDataset() { ... }
   ```

5. **Ne pas mettre de TTL trop long sur données critiques** :
   ```java
   // ❌ Mauvais - 1 heure pour des alertes
   @Cacheable(value = "alerts", ttl = 3600000)
   ```

---

## Exemples complets

### Exemple 1 : Service Vehicle avec cache

```java
@Service
@AllArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private VehicleRepository vehicleRepository;

    // Liste des véhicules d'un utilisateur (cache 5 min)
    @Cacheable(value = "vehicles", key = "'user_' + #user.id")
    public List<VehicleDTO> getMyVehicles(User user) {
        log.debug("Cache MISS: Fetching vehicles for user {}", user.getId());
        return vehicleRepository.findByOwner(user)
                .stream()
                .map(vehicleMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Détail d'un véhicule (cache 10 min)
    @Cacheable(value = "vehicleDetail", key = "#id")
    public VehicleDTO getVehicleById(UUID id) {
        log.debug("Cache MISS: Fetching vehicle {}", id);
        return vehicleRepository.findById(id)
                .map(vehicleMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));
    }

    // Création : invalide le cache de la liste
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleDTO createVehicle(CreateVehicleDTO dto, User user) {
        log.debug("Cache EVICT: Creating vehicle, clearing vehicles cache");
        Vehicle vehicle = vehicleMapper.toEntity(dto);
        vehicle.setOwner(user);
        return vehicleMapper.toDTO(vehicleRepository.save(vehicle));
    }

    // Mise à jour : invalide détail + liste
    @Caching(evict = {
        @CacheEvict(value = "vehicleDetail", key = "#id"),
        @CacheEvict(value = "vehicles", allEntries = true)
    })
    public VehicleDTO updateVehicle(UUID id, UpdateVehicleDTO dto) {
        log.debug("Cache EVICT: Updating vehicle {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));
        vehicleMapper.updateEntity(vehicle, dto);
        return vehicleMapper.toDTO(vehicleRepository.save(vehicle));
    }

    // Suppression : invalide tout
    @CacheEvict(value = {"vehicles", "vehicleDetail"}, allEntries = true)
    public void deleteVehicle(UUID id) {
        log.debug("Cache EVICT: Deleting vehicle {}", id);
        vehicleRepository.deleteById(id);
    }
}
```

---

## Troubleshooting

### Problème : Cache ne fonctionne pas

**Vérifications :**
1. Redis est-il démarré ?
   ```bash
   docker ps | grep redis
   ```

2. Configuration correcte ?
   ```bash
   docker exec -it geofence-redis redis-cli ping
   # Doit retourner PONG
   ```

3. Annotation `@EnableCaching` présente ?
   ```java
   @Configuration
   @EnableCaching  // ← Vérifier ceci
   public class RedisConfig { ... }
   ```

### Problème : Données obsolètes en cache

**Solutions :**
1. Vider manuellement :
   ```bash
   docker exec -it geofence-redis redis-cli FLUSHALL
   ```

2. Ajouter `@CacheEvict` sur les méthodes de modification

3. Réduire le TTL dans `RedisConfig.java`

### Problème : Redis OOM (Out of Memory)

**Solutions :**
1. Configurer une politique d'éviction dans `docker-compose.yml` :
   ```yaml
   redis:
     command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
   ```

2. Augmenter la RAM allouée

---

## Performance attendues

| Opération | Sans cache | Avec cache | Gain |
|-----------|-----------|------------|------|
| GET /api/geofences | ~150ms | ~15ms | **10x** |
| GET /api/vehicles/{id} | ~80ms | ~8ms | **10x** |
| GET /api/statistics/user | ~500ms | ~50ms | **10x** |
| GET /api/dashboard/stats | ~300ms | ~30ms | **10x** |

**Taux de hit attendu** : 70-90% après warm-up

---

## Conclusion

Le cache Redis améliore drastiquement les performances de l'API GeoFence. En suivant ce guide et les best practices, vous obtiendrez :
- ⚡ Réponses 5-10x plus rapides
- 📉 Charge DB réduite de 60-80%
- 🚀 Meilleure scalabilité
- 💰 Coûts infrastructure réduits

**Prochaine étape** : Ajoutez Prometheus + Grafana (Phase 6) pour monitorer le cache en temps réel !
