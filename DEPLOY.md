# Guide de Déploiement Geofence Platform

## 🔧 En Local (Développement)

### 1. Nettoyer l'environnement existant

```bash
cd geofence-reactive-final-version
docker compose down -v
docker system prune -f
```

### 2. Lancer les services

```bash
docker compose up -d --build
```

### 3. Vérifier les logs

```bash
# Logs PostgreSQL (vérifier création users)
docker logs geofence-postgres

# Logs application (vérifier Liquibase)
docker logs -f geofence-api
```

✅ **Attendu** : Liquibase crée toutes les tables automatiquement au démarrage

---

## 🚀 Sur le Serveur (Production)

### Prérequis côté professeur

Le prof doit exécuter **UNE SEULE FOIS** dans sa base de données :

```bash
# Se connecter à PostgreSQL
psql -U postgres -d geofence_db

# Copier-coller le contenu de scripts/init-db-production.sql
\i /path/to/init-db-production.sql
```

⚠️ **Avant d'exécuter**, remplacer dans le fichier :
- `APP_PASSWORD_TO_CHANGE` → mot de passe réel (ex: `SecureApp2024!`)
- `LIQUIBASE_PASSWORD_TO_CHANGE` → mot de passe réel (ex: `SecureLB2024!`)

### Variables d'environnement à configurer (GitHub Secrets)

Dans `projet-synthese-gi26` (repo principal), configurer :

```bash
# Base de données
DB_URL=jdbc:postgresql://host:5432/geofence_db
DB_USERNAME=geofence_app
DB_PASSWORD=<mot_de_passe_app>
DB_LIQUIBASE_USERNAME=geofence_liquibase
DB_LIQUIBASE_PASSWORD=<mot_de_passe_liquibase>

# Redis
REDIS_HOST=redis-host-ou-ip
REDIS_PORT=6379

# Application
JWT_ENCRYPTION_KEY=<clé_256_bits_en_hex>
FRONTEND_URL=https://votre-frontend.com
SERVER_PORT=8080
```

### Pipeline GitHub Actions

Le pipeline `.github/workflows/ci-cd.yml` se déclenche automatiquement sur :
- `push` sur `main`
- `pull_request` sur `main`

**Étapes automatisées** :
1. Build Maven (tests inclus)
2. Build Docker image
3. Push vers registry
4. Deploy sur Coolify via webhook

---

## 🔍 Vérifications

### En local

```bash
# Health check
curl http://localhost:8080/actuator/health

# Vérifier tables PostGIS
docker exec -it geofence-postgres psql -U postgres -d geofence_db -c "\dt"

# Vérifier extensions
docker exec -it geofence-postgres psql -U postgres -d geofence_db -c "\dx"
```

### Sur le serveur

```bash
# Logs du service
docker logs geofence-service -f --tail=100

# Vérifier users PostgreSQL
docker exec postgres psql -U postgres -d geofence_db -c "SELECT rolname, rolcanlogin FROM pg_roles WHERE rolname LIKE 'geofence%';"
```

---

## 🐛 Troubleshooting

### Erreur : "relation already exists"

**Cause** : Tables créées manuellement, Liquibase ne le sait pas

**Solution** :
```sql
-- Option 1 : Fresh start (DROP SCHEMA)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO geofence_app;
GRANT ALL ON SCHEMA public TO geofence_liquibase;

-- Option 2 : Synchroniser Liquibase
-- Voir scripts/liquibase-sync.sql
```

### Erreur de connexion PostgreSQL

Vérifier que :
- Les users `geofence_app` et `geofence_liquibase` existent
- Les mots de passe correspondent aux variables d'environnement
- Les permissions sont correctes (voir `scripts/init-db.sql`)

---

## 📁 Fichiers importants

```
geofence-reactive-final-version/
├── scripts/
│   ├── init-db.sql                    # Init local (docker-compose)
│   └── init-db-production.sql         # Script à donner au prof
├── src/main/resources/
│   ├── application.properties         # Config dev
│   ├── application-docker.properties  # Config Docker/prod
│   └── db/changelog/
│       ├── db.changelog-master.xml
│       └── changes/
│           └── 001-initial-schema.xml
├── .github/workflows/
│   └── ci-cd.yml                      # Pipeline CI/CD
├── Dockerfile                         # Multi-stage build
├── docker-compose.yml                 # Dev local
└── docker-compose.prod.yml            # Prod (sans PostgreSQL)
```
