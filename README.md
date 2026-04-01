# Geofence Platform - Backend API

API Spring Boot réactive avec PostGIS pour la gestion de geofences intelligentes.

## Stack

- Java 21 + Spring Boot 3.4.3 (WebFlux)
- PostgreSQL 16 + PostGIS 3.4
- Redis 7 (cache)
- Liquibase (migrations)
- Docker + GitHub Actions

---

## Développement Local

```bash
# Démarrer
docker compose up -d

# Logs
docker logs -f geofence-api

# Health check
curl http://localhost:8080/actuator/health
```

API : http://localhost:8080
Swagger : http://localhost:8080/swagger-ui.html

---

## Déploiement Production

### 1. Base de données (une fois)

```bash
# Le DBA exécute ce script
psql -U postgres -d geofence_db -f scripts/init-db-production.sql
```

**Important** : Modifier les mots de passe dans le script avant.

### 2. Variables d'environnement (GitHub Secrets)

```bash
DB_URL=jdbc:postgresql://host:5432/geofence_db
DB_USERNAME=geofence_app
DB_PASSWORD=***
DB_LIQUIBASE_USERNAME=geofence_liquibase
DB_LIQUIBASE_PASSWORD=***
REDIS_HOST=redis-host
JWT_ENCRYPTION_KEY=***
FRONTEND_URL=https://frontend.com
```

### 3. Pipeline CI/CD

Le push sur `main` déclenche automatiquement `.github/workflows/ci-cd.yml` :

1. Build Maven + tests
2. Build Docker image
3. Deploy via Coolify webhook

---

## Structure DB

**26 tables** créées par Liquibase :
- `organizations`, `users`, `role`, `vehicle`
- `circle_geofence_zone`, `polygon_geofence_zone`
- `locations`, `routes`, `route_segments`, `alert`

**Types PostGIS** (SRID 4326) :
- `POINT` : positions GPS, centres de cercles
- `POLYGON` : zones complexes
- `LINESTRING` : segments de route

**Users PostgreSQL** :
- `geofence_app` → DML (lecture/écriture)
- `geofence_liquibase` → DDL (migrations)

---

## Commandes utiles

```bash
# Rebuild
docker compose down -v && docker compose up -d --build

# Vérifier tables
docker exec geofence-postgres psql -U geofence_app -d geofence_db -c "\dt"

# Vérifier PostGIS
docker exec geofence-postgres psql -U geofence_app -d geofence_db -c "SELECT f_table_name, f_geometry_column, type FROM geometry_columns;"
```
