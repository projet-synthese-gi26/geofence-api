# Guide du Monitoring - GeoFence Backend

## 📋 Table des matières
- [Introduction](#introduction)
- [Stack de monitoring](#stack-de-monitoring)
- [Configuration](#configuration)
- [Accès aux interfaces](#accès-aux-interfaces)
- [Métriques disponibles](#métriques-disponibles)
- [Dashboards Grafana](#dashboards-grafana)
- [Logs structurés](#logs-structurés)
- [Correlation IDs](#correlation-ids)
- [Alerting](#alerting)
- [Troubleshooting](#troubleshooting)

---

## Introduction

Le système de monitoring de GeoFence permet de :
- **Observer** les performances en temps réel
- **Détecter** les problèmes rapidement
- **Analyser** les tendances et comportements
- **Optimiser** les ressources et performances
- **Tracer** les requêtes avec des Correlation IDs

---

## Stack de monitoring

```
┌───────────────────────────────────────────────────────┐
│                    GeoFence API                       │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │  Spring Boot │  │   Actuator   │  │  Logback   │ │
│  │   Metrics    │─▶│  /actuator/* │◀─│  Logs JSON │ │
│  └──────────────┘  └──────────────┘  └────────────┘ │
└───────────────┬────────────────────────────┬──────────┘
                │                            │
                ▼                            ▼
       ┌─────────────────┐         ┌──────────────┐
       │   Prometheus    │         │  logs/*.json │
       │ (Collecte 15s)  │         │  (Fichiers)  │
       └────────┬────────┘         └──────────────┘
                │
                ▼
       ┌─────────────────┐
       │     Grafana     │
       │  (Visualisation)│
       └─────────────────┘
```

---

## Configuration

### 1. Actuator (Spring Boot)

**Dépendances Maven :**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Configuration (`application.properties`) :**
```properties
# Endpoints exposés
management.endpoints.web.exposure.include=health,info,metrics,prometheus,caches

# Health check détaillé
management.endpoint.health.show-details=always

# Métriques Prometheus
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true

# Tags globaux
management.metrics.tags.application=geofence
management.metrics.tags.environment=docker
```

### 2. Prometheus

**Configuration (`monitoring/prometheus/prometheus.yml`) :**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'geofence-api'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['api:8080']
```

**Scrape des métriques toutes les 10 secondes** depuis `/actuator/prometheus`.

### 3. Grafana

**Provisioning automatique** :
- Datasource Prometheus configurée
- Dashboard "GeoFence - Overview" pré-chargé
- Plugins installés automatiquement

---

## Accès aux interfaces

| Service | URL | Credentials | Description |
|---------|-----|-------------|-------------|
| **API Actuator** | http://localhost:8080/actuator | - | Endpoints Spring Boot |
| **Prometheus** | http://localhost:9090 | - | Collecte de métriques |
| **Grafana** | http://localhost:3001 | admin / admin123 | Dashboards visualisation |
| **API Health** | http://localhost:8080/actuator/health | - | Health check détaillé |

### Démarrage

```bash
cd /path/to/geofence-reactive-final-version

# Démarrer tous les services
docker-compose up -d

# Vérifier que tout tourne
docker-compose ps

# Voir les logs
docker-compose logs -f api
docker-compose logs -f prometheus
docker-compose logs -f grafana
```

**Accès Grafana :**
1. Ouvrir http://localhost:3001
2. Login : `admin` / `admin123`
3. Dashboard : "GeoFence - Overview" déjà configuré

---

## Métriques disponibles

### 1. Métriques standard Spring Boot

| Métrique | Description |
|----------|-------------|
| `jvm_memory_used_bytes` | Mémoire JVM utilisée |
| `jvm_memory_max_bytes` | Mémoire JVM maximale |
| `jvm_threads_live_threads` | Nombre de threads actifs |
| `jvm_gc_pause_seconds` | Durée des Garbage Collections |
| `process_cpu_usage` | CPU utilisé par le processus |
| `system_cpu_usage` | CPU système total |

### 2. Métriques HTTP

| Métrique | Description |
|----------|-------------|
| `http_server_requests_seconds_count` | Nombre total de requêtes |
| `http_server_requests_seconds_sum` | Temps total des requêtes |
| `http_server_requests_seconds_max` | Temps maximal d'une requête |

**Tags disponibles :**
- `uri` : Endpoint appelé
- `method` : Méthode HTTP (GET, POST, etc.)
- `status` : Code de réponse (200, 404, 500, etc.)
- `outcome` : Résultat (SUCCESS, CLIENT_ERROR, SERVER_ERROR)

**Exemples de requêtes Prometheus :**
```promql
# Requêtes par seconde
rate(http_server_requests_seconds_count{application="geofence"}[1m])

# Temps de réponse 95e percentile
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))

# Taux d'erreur 5xx
rate(http_server_requests_seconds_count{status=~"5.."}[1m]) /
rate(http_server_requests_seconds_count[1m])
```

### 3. Métriques métier custom

Créées via `BusinessMetrics.java` :

| Métrique | Type | Description |
|----------|------|-------------|
| `geofence_created_total` | Counter | Geofences créées |
| `geofence_deleted_total` | Counter | Geofences supprimées |
| `geofence_active_count` | Gauge | Geofences actives actuellement |
| `vehicle_created_total` | Counter | Véhicules créés |
| `vehicle_deleted_total` | Counter | Véhicules supprimés |
| `vehicle_active_count` | Gauge | Véhicules actifs |
| `alert_created_total` | Counter | Alertes créées (par type) |
| `alert_unread_count` | Gauge | Alertes non lues |
| `route_deviation_total` | Counter | Déviations de route détectées |
| `zone_violation_total` | Counter | Violations de zone (par type) |
| `geofence_check_duration_seconds` | Timer | Temps de vérification geofence |
| `route_deviation_check_duration_seconds` | Timer | Temps de vérification déviation |
| `cache_hit_total` | Counter | Cache hits (par cache) |
| `cache_miss_total` | Counter | Cache misses (par cache) |

**Utilisation dans le code :**
```java
@Service
public class GeofenceServiceImpl {
    private final BusinessMetrics metrics;

    public GeofenceDTO createGeofence(CreateGeofenceDTO dto) {
        // Création de la geofence
        GeofenceDTO result = ...;

        // Incrémenter la métrique
        metrics.incrementGeofenceCreated();

        return result;
    }
}
```

### 4. Métriques Redis (cache)

| Métrique | Description |
|----------|-------------|
| `cache_gets_total` | Nombre de lectures cache |
| `cache_puts_total` | Nombre d'écritures cache |
| `cache_evictions_total` | Évictions de cache |
| `cache_size` | Taille des caches |

---

## Dashboards Grafana

### Dashboard "GeoFence - Overview"

**Panels disponibles :**

1. **Active Geofences** (Stat)
   - Nombre de geofences actives
   - Couleur : Vert

2. **Active Vehicles** (Stat)
   - Nombre de véhicules actifs
   - Couleur : Bleu

3. **Unread Alerts** (Stat)
   - Alertes non lues
   - Couleur : Rouge

4. **Requests per Second** (Graph)
   - Trafic HTTP en temps réel
   - Par endpoint

5. **Response Time (95th percentile)** (Graph)
   - Latence des endpoints
   - Seuils : < 200ms (vert), 200-500ms (jaune), > 500ms (rouge)

6. **Business Events** (Bars)
   - Création de geofences, véhicules, alertes
   - Taux sur 5 minutes

7. **JVM Memory Usage** (Graph)
   - Heap utilisé vs max
   - Détection de fuites mémoire

**Rafraîchissement : Automatique toutes les 10 secondes**

### Créer un dashboard personnalisé

1. Aller dans Grafana → "+" → "Dashboard"
2. Ajouter un panel
3. Sélectionner "Prometheus" comme datasource
4. Entrer une requête PromQL :
   ```promql
   rate(alert_created_total{type="ZONE_EXIT"}[5m])
   ```
5. Configurer la visualisation (Graph, Stat, Gauge, etc.)
6. Sauvegarder

---

## Logs structurés

### Configuration Logback

**Fichier : `src/main/resources/logback-spring.xml`**

**3 appenders configurés :**

1. **CONSOLE** : Logs colorés pour développement
2. **JSON_FILE** : Logs JSON structurés (`logs/application.json`)
3. **FILE** : Logs texte standard (`logs/application.log`)

**Rotation automatique** :
- Quotidienne (nouveau fichier chaque jour)
- Compression gzip des anciens logs
- Rétention : 30 jours
- Taille max : 1 GB total

### Format des logs JSON

```json
{
  "@timestamp": "2025-02-03T14:30:45.123Z",
  "level": "INFO",
  "thread": "reactor-http-nio-2",
  "logger": "ink.yowyob.geofence.service.GeofenceServiceImpl",
  "message": "Cache MISS: Fetching geofence 123e4567",
  "correlationId": "a7b3c5d9-e1f2-4567-8901-234567890abc",
  "userId": "user-uuid",
  "username": "john.doe",
  "application": "geofence"
}
```

### Niveaux de log

| Niveau | Usage | Environnement |
|--------|-------|---------------|
| `TRACE` | Détails très verbeux (SQL bindings) | Dev uniquement |
| `DEBUG` | Informations de débogage | Dev, Staging |
| `INFO` | Événements importants | Tous |
| `WARN` | Avertissements (non bloquant) | Tous |
| `ERROR` | Erreurs (nécessitent attention) | Tous |

**Configuration par profil :**
```xml
<!-- Local/Dev : Console + fichier texte -->
<springProfile name="local,dev">
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</springProfile>

<!-- Docker/Prod : Console + JSON + fichier texte -->
<springProfile name="docker,prod">
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_JSON"/>
        <appender-ref ref="ASYNC_FILE"/>
    </root>
</springProfile>
```

### Lire les logs

```bash
# Logs en temps réel
docker-compose logs -f api

# Logs JSON structurés
docker exec -it geofence-api cat /app/logs/application.json | jq

# Logs avec correlation ID
docker exec -it geofence-api grep "correlationId" /app/logs/application.json | jq

# Logs d'erreurs uniquement
docker exec -it geofence-api grep "ERROR" /app/logs/application.log
```

---

## Correlation IDs

### Qu'est-ce qu'un Correlation ID ?

Un **ID unique** attribué à chaque requête pour **tracer son parcours** à travers les logs.

### Comment ça fonctionne ?

1. **Requête arrive** → `CorrelationIdFilter` génère/récupère un UUID
2. **Header ajouté** : `X-Correlation-ID: a7b3c5d9-...`
3. **MDC (Mapped Diagnostic Context)** : Stocké dans Logback
4. **Tous les logs** de cette requête contiennent le même ID
5. **Réponse inclut** le header `X-Correlation-ID`

### Utilisation

**Côté client (Frontend/Postman) :**
```bash
curl -H "X-Correlation-ID: my-custom-id-123" http://localhost:8080/api/geofences
```

Si pas fourni, un UUID est généré automatiquement.

**Tracer une requête dans les logs :**
```bash
# Chercher tous les logs d'une requête spécifique
grep "a7b3c5d9-e1f2-4567-8901-234567890abc" logs/application.log

# Format JSON
cat logs/application.json | jq 'select(.correlationId=="a7b3c5d9-...")'
```

**Exemple de logs tracés :**
```
[a7b3c5d9-...] GeofenceServiceImpl: Cache MISS: Fetching geofence 123
[a7b3c5d9-...] GeofenceRepository: Executing query: SELECT * FROM geofence
[a7b3c5d9-...] GeofenceServiceImpl: Geofence found, returning DTO
[a7b3c5d9-...] CorrelationIdFilter: Request completed in 45ms
```

---

## Alerting

### Alertes Prometheus (à configurer)

Créer `monitoring/prometheus/alerts.yml` :

```yaml
groups:
  - name: geofence_alerts
    interval: 30s
    rules:
      # Alerte : Taux d'erreur > 5%
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[1m]) /
          rate(http_server_requests_seconds_count[1m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Taux d'erreur élevé ({{ $value }}%)"
          description: "Plus de 5% de requêtes en erreur 5xx"

      # Alerte : Latence p95 > 1s
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            rate(http_server_requests_seconds_bucket[1m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Latence élevée ({{ $value }}s)"
          description: "Temps de réponse p95 supérieur à 1 seconde"

      # Alerte : Mémoire JVM > 90%
      - alert: HighMemoryUsage
        expr: |
          jvm_memory_used_bytes{area="heap"} /
          jvm_memory_max_bytes{area="heap"} > 0.9
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Mémoire JVM élevée ({{ $value }}%)"
          description: "Heap memory utilisée à plus de 90%"

      # Alerte : Aucune requête depuis 5 min
      - alert: NoTraffic
        expr: |
          rate(http_server_requests_seconds_count[5m]) == 0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Aucun trafic détecté"
          description: "Aucune requête reçue depuis 5 minutes"
```

### Intégration Alertmanager (optionnel)

Ajouter au `docker-compose.yml` :
```yaml
alertmanager:
  image: prom/alertmanager:latest
  ports:
    - "9093:9093"
  volumes:
    - ./monitoring/alertmanager/config.yml:/etc/alertmanager/config.yml
```

**Notifications** : Email, Slack, PagerDuty, etc.

---

## Troubleshooting

### Problème : Prometheus ne scrape pas les métriques

**Vérifications :**
1. API est accessible ?
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Prometheus voit la target ?
   → http://localhost:9090/targets
   → "geofence-api" doit être "UP"

3. Firewall/réseau Docker ?
   ```bash
   docker exec -it geofence-prometheus wget -O- http://api:8080/actuator/prometheus
   ```

### Problème : Grafana ne montre pas de données

**Vérifications :**
1. Datasource Prometheus configurée ?
   → Grafana → Configuration → Data Sources → "Prometheus"
   → Test : Devrait être "OK"

2. Données présentes dans Prometheus ?
   → http://localhost:9090/graph
   → Query : `up{job="geofence-api"}`
   → Devrait retourner `1`

3. Dashboard bien configuré ?
   → Vérifier les requêtes PromQL
   → Time range correct ? (dernière heure)

### Problème : Logs ne sont pas écrits

**Vérifications :**
1. Dossier `logs/` existe ?
   ```bash
   ls -la logs/
   ```

2. Permissions d'écriture ?
   ```bash
   chmod -R 755 logs/
   ```

3. Profil Spring actif ?
   ```bash
   docker exec -it geofence-api env | grep SPRING_PROFILES_ACTIVE
   ```

### Problème : Correlation ID ne s'affiche pas

**Vérifications :**
1. `CorrelationIdFilter` est chargé ?
   ```bash
   docker logs geofence-api | grep CorrelationIdFilter
   ```

2. Requête passe par le filtre ?
   → Toutes les requêtes HTTP doivent passer par le WebFilter

---

## Best Practices

### 1. Métriques

✅ **DO:**
- Incrémenter les compteurs métier (geofences créées, etc.)
- Utiliser des timers pour les opérations critiques
- Ajouter des tags pertinents

❌ **DON'T:**
- Créer trop de métriques (explosion cardinale)
- Oublier de mettre à jour les gauges

### 2. Logs

✅ **DO:**
- Logger les événements importants (INFO)
- Logger les erreurs avec stack trace (ERROR)
- Inclure le contexte (userId, geofenceId, etc.)
- Utiliser des correlation IDs

❌ **DON'T:**
- Logger des données sensibles (mots de passe, tokens)
- Logger en DEBUG en production sans raison
- Oublier de logger les exceptions

### 3. Dashboards

✅ **DO:**
- Créer des dashboards par domaine (API, Business, Infra)
- Utiliser des seuils de couleur (rouge/jaune/vert)
- Ajouter des descriptions aux panels

❌ **DON'T:**
- Surcharger un dashboard (max 10-12 panels)
- Oublier de documenter les requêtes PromQL

---

## Conclusion

Le monitoring de GeoFence vous permet de :
- 👀 **Observer** : Dashboards temps réel
- 🔍 **Tracer** : Correlation IDs dans les logs
- 📊 **Analyser** : Métriques métier + infra
- 🚨 **Alerter** : Prometheus alerts (à configurer)

**Prochaines étapes :**
1. Personnaliser les dashboards Grafana
2. Configurer les alertes Prometheus
3. Intégrer Alertmanager pour notifications
4. Ajouter des exporters (Redis, PostgreSQL)

**Ressources :**
- Prometheus : http://localhost:9090
- Grafana : http://localhost:3001
- Actuator : http://localhost:8080/actuator

Happy monitoring! 🚀
