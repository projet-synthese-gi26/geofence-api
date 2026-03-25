#!/bin/bash

# Script pour créer automatiquement tous les fichiers Docker pour Geofence API
# Utilisation: ./setup-docker.sh

set -e

echo "🐳 Configuration Docker pour Geofence API"
echo "=========================================="

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Vérifier si on est dans un projet Maven
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Erreur: Ce script doit être exécuté à la racine d'un projet Maven (pom.xml non trouvé)${NC}"
    exit 1
fi

print_info "Création de la structure des dossiers..."

# Créer les dossiers nécessaires
mkdir -p scripts
mkdir -p nginx
mkdir -p backups
mkdir -p logs
mkdir -p src/main/resources

print_status "Dossiers créés"

# 1. Créer le Dockerfile
print_info "Création du Dockerfile..."
cat > Dockerfile << 'EOF'
# Dockerfile
FROM openjdk:21-jdk-slim

# Métadonnées
LABEL maintainer="votre-email@example.com"
LABEL description="Geofence API - Spring Boot WebFlux Application"

# Variables d'environnement
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SPRING_PROFILES_ACTIVE=docker

# Installer curl pour les health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Créer un utilisateur non-root pour la sécurité
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Répertoire de travail
WORKDIR /app

# Copier les dépendances locales
COPY lib/ /app/lib/

# Copier le JAR de l'application
COPY target/geofence-*.jar /app/app.jar

# Créer le dossier uploads
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app

# Changer vers l'utilisateur non-root
USER appuser

# Port exposé
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Point d'entrée
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
EOF

print_status "Dockerfile créé"

# 2. Créer docker-compose.yml (développement)
print_info "Création de docker-compose.yml..."
cat > docker-compose.yml << 'EOF'
# docker-compose.yml
version: '3.8'

services:
  # Base de données PostgreSQL + PostGIS
  postgres:
    image: postgis/postgis:15-3.4
    container_name: geofence-postgres
    environment:
      POSTGRES_DB: geofence_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C"
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/01-init.sql
    networks:
      - geofence-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d geofence_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Application Spring Boot
  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: geofence-api
    environment:
      # Base de données
      DB_URL: jdbc:postgresql://postgres:5432/geofence_db
      DB_USERNAME: postgres
      DB_PASSWORD: postgres123

      # JWT
      JWT_ENCRYPTION_KEY: ${JWT_ENCRYPTION_KEY:-608f36e92dc66d97d5933f0e6371893cb4fc05b3aa8f8de64014732472303a7c}

      # Mail
      MAIL_HOST: ${MAIL_HOST:-smtp.gmail.com}
      MAIL_PORT: ${MAIL_PORT:-587}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}

      # Application
      FRONTEND_URL: ${FRONTEND_URL:-http://localhost:3000}
      UPLOAD_DIR: /app/uploads
      SERVER_PORT: 8080

      # Spring profiles
      SPRING_PROFILES_ACTIVE: docker
    ports:
      - "8080:8080"
    volumes:
      - uploads_data:/app/uploads
    networks:
      - geofence-network
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # Interface d'administration PostgreSQL (optionnel)
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: geofence-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@geofence.com
      PGADMIN_DEFAULT_PASSWORD: admin123
      PGADMIN_CONFIG_SERVER_MODE: 'False'
    ports:
      - "5050:80"
    networks:
      - geofence-network
    depends_on:
      - postgres
    restart: unless-stopped
    profiles:
      - tools

volumes:
  postgres_data:
    driver: local
  uploads_data:
    driver: local

networks:
  geofence-network:
    driver: bridge
EOF

print_status "docker-compose.yml créé"

# 3. Créer docker-compose.prod.yml (production)
print_info "Création de docker-compose.prod.yml..."
cat > docker-compose.prod.yml << 'EOF'
# docker-compose.prod.yml
version: '3.8'

services:
  postgres:
    image: postgis/postgis:15-3.4
    container_name: geofence-postgres-prod
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_prod_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/01-init.sql
      - ./backups:/backups
    networks:
      - geofence-network
    restart: always
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}"]
      interval: 30s
      timeout: 10s
      retries: 5

  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: geofence-api-prod
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_ENCRYPTION_KEY: ${JWT_ENCRYPTION_KEY}
      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      FRONTEND_URL: ${FRONTEND_URL}
      UPLOAD_DIR: /app/uploads
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: prod
      JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC"
    ports:
      - "8080:8080"
    volumes:
      - uploads_prod_data:/app/uploads
      - ./logs:/app/logs
    networks:
      - geofence-network
    depends_on:
      postgres:
        condition: service_healthy
    restart: always

  # Reverse proxy Nginx (optionnel)
  nginx:
    image: nginx:alpine
    container_name: geofence-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
    networks:
      - geofence-network
    depends_on:
      - api
    restart: always
    profiles:
      - nginx

volumes:
  postgres_prod_data:
  uploads_prod_data:

networks:
  geofence-network:
    driver: bridge
EOF

print_status "docker-compose.prod.yml créé"

# 4. Créer .dockerignore
print_info "Création de .dockerignore..."
cat > .dockerignore << 'EOF'
# .dockerignore

# Git
.git
.gitignore

# Documentation
README.md
*.md

# IDE
.idea/
.vscode/
*.iml

# Maven
.mvn/
mvnw
mvnw.cmd

# Logs
logs/
*.log

# OS
.DS_Store
Thumbs.db

# Docker
Dockerfile*
docker-compose*.yml

# Environment (sera copié explicitement si nécessaire)
.env*
!.env.docker

# Tests
src/test/

# Temporary files
tmp/
temp/

# Node modules (si présent)
node_modules/

# Backup files
*.bak
*.backup
EOF

print_status ".dockerignore créé"

# 5. Créer le script d'initialisation de la base de données
print_info "Création de scripts/init-db.sql..."
cat > scripts/init-db.sql << 'EOF'
-- scripts/init-db.sql

-- Activer PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Vérifier l'installation
SELECT PostGIS_Full_Version();

-- Créer des index spatiaux si nécessaire
-- (Hibernate les créera automatiquement, mais on peut les optimiser)

-- Log de confirmation
\echo 'PostGIS extensions installed successfully!';
EOF

print_status "scripts/init-db.sql créé"

# 6. Créer .env.docker
print_info "Création de .env.docker..."
cat > .env.docker << 'EOF'
# .env.docker - Configuration pour Docker

# Base de données
DB_NAME=geofence_db
DB_USERNAME=postgres
DB_PASSWORD=postgres123

# JWT (CHANGEZ CETTE CLÉ EN PRODUCTION !)
JWT_ENCRYPTION_KEY=608f36e92dc66d97d5933f0e6371893cb4fc05b3aa8f8de64014732472303a7c

# Mail (remplacez par vos vraies valeurs)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_app_password

# Application
FRONTEND_URL=http://localhost:3000
SERVER_PORT=8080
EOF

print_status ".env.docker créé"

# 7. Créer application-docker.properties
print_info "Création de application-docker.properties..."
cat > src/main/resources/application-docker.properties << 'EOF'
# application-docker.properties

# Base de données (surchargées par variables d'environnement)
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.database-platform=org.hibernate.spatial.dialect.postgis.PostgisDialect

# Logs optimisés pour Docker
logging.level.root=INFO
logging.level.com.reseau.geofence=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Actuator pour health checks
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=always

# File upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload-dir=${UPLOAD_DIR:/app/uploads}

# Application
app.frontend.url=${FRONTEND_URL}

# Mail
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# JWT
jwt.encryption.key=${JWT_ENCRYPTION_KEY}

# Security
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
spring.main.web-application-type=reactive

# Optimisations pour conteneur
server.netty.connection-timeout=20s
server.netty.idle-timeout=60s
EOF

print_status "application-docker.properties créé"

# 8. Créer le script de démarrage développement
print_info "Création de scripts/start.sh..."
cat > scripts/start.sh << 'EOF'
#!/bin/bash
# scripts/start.sh

set -e

echo "🐳 Starting Geofence API with Docker..."

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Vérifier si Docker est installé
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed!${NC}"
    echo "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# Vérifier si Docker Compose est installé
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed!${NC}"
    echo "Please install Docker Compose: https://docs.docker.com/compose/install/"
    exit 1
fi

# Vérifier si le fichier .env.docker existe
if [ ! -f ".env.docker" ]; then
    echo -e "${YELLOW}⚠️  .env.docker file not found. Creating from template...${NC}"
    cp .env.docker.example .env.docker 2>/dev/null || echo "Please create .env.docker file with your configuration"
fi

# Builder l'application
echo "📦 Building application..."
if ! mvn clean package -DskipTests; then
    echo -e "${RED}❌ Maven build failed!${NC}"
    exit 1
fi

# Vérifier que le JAR existe
if [ ! -f target/geofence-*.jar ]; then
    echo -e "${RED}❌ JAR file not found in target/ directory!${NC}"
    exit 1
fi

# Arrêter les services existants
echo "🛑 Stopping existing services..."
docker-compose down --remove-orphans 2>/dev/null || true

# Démarrer les services
echo "🚀 Starting services..."
if docker-compose --env-file .env.docker up --build; then
    echo -e "${GREEN}✅ Application started successfully!${NC}"
    echo -e "${GREEN}🌐 API: http://localhost:8080${NC}"
    echo -e "${GREEN}📚 Docs: http://localhost:8080/api/v1/docs/index.html${NC}"
    echo -e "${GREEN}🗄️  PgAdmin: http://localhost:5050 (admin@geofence.com / admin123)${NC}"
else
    echo -e "${RED}❌ Failed to start services!${NC}"
    echo "Check logs with: docker-compose logs"
    exit 1
fi
EOF

# 9. Créer le script de démarrage production
print_info "Création de scripts/start-prod.sh..."
cat > scripts/start-prod.sh << 'EOF'
#!/bin/bash
# scripts/start-prod.sh

set -e

echo "🚀 Starting Geofence API in PRODUCTION mode..."

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Charger les variables d'environnement si le fichier existe
if [ -f ".env" ]; then
    source .env
fi

# Vérifier les variables d'environnement critiques
if [ -z "$JWT_ENCRYPTION_KEY" ]; then
    echo -e "${RED}❌ JWT_ENCRYPTION_KEY is required!${NC}"
    echo "Set it in .env file or as environment variable"
    exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
    echo -e "${RED}❌ DB_PASSWORD is required!${NC}"
    echo "Set it in .env file or as environment variable"
    exit 1
fi

# Avertissement de sécurité
echo -e "${YELLOW}⚠️  PRODUCTION MODE - Make sure you have:${NC}"
echo "   - Changed default passwords"
echo "   - Set secure JWT_ENCRYPTION_KEY"
echo "   - Configured proper mail settings"
echo "   - Set up SSL certificates (if using nginx)"
echo ""
read -p "Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

# Builder l'application pour la production
echo "📦 Building application for production..."
if ! mvn clean package -Pprod -DskipTests; then
    echo -e "${RED}❌ Maven build failed!${NC}"
    exit 1
fi

# Créer les dossiers de production si nécessaire
mkdir -p logs backups

# Arrêter les services existants
echo "🛑 Stopping existing services..."
docker-compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true

# Démarrer en mode production
echo "🚀 Starting production services..."
if docker-compose -f docker-compose.prod.yml up -d; then
    echo -e "${GREEN}✅ Production deployment started!${NC}"
    echo -e "${GREEN}🌐 API: http://localhost:8080${NC}"
    echo ""
    echo "📊 Monitor with:"
    echo "   docker-compose -f docker-compose.prod.yml logs -f"
    echo "   docker-compose -f docker-compose.prod.yml ps"
else
    echo -e "${RED}❌ Failed to start production services!${NC}"
    exit 1
fi
EOF

# 10. Créer le script d'arrêt
print_info "Création de scripts/stop.sh..."
cat > scripts/stop.sh << 'EOF'
#!/bin/bash
# scripts/stop.sh

echo "🛑 Stopping Geofence API services..."

# Arrêter développement
if [ -f "docker-compose.yml" ]; then
    echo "Stopping development services..."
    docker-compose down
fi

# Arrêter production
if [ -f "docker-compose.prod.yml" ]; then
    echo "Stopping production services..."
    docker-compose -f docker-compose.prod.yml down
fi

echo "✅ All services stopped"
EOF

# 11. Créer le script de nettoyage
print_info "Création de scripts/clean.sh..."
cat > scripts/clean.sh << 'EOF'
#!/bin/bash
# scripts/clean.sh

echo "🧹 Cleaning Docker resources for Geofence API..."

# Arrêter tous les services
./scripts/stop.sh

# Supprimer les conteneurs
echo "Removing containers..."
docker-compose down --remove-orphans 2>/dev/null || true
docker-compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true

# Supprimer les images
echo "Removing images..."
docker rmi geofence-api 2>/dev/null || true
docker rmi geofence-api-prod 2>/dev/null || true

# Supprimer les volumes (ATTENTION: ceci supprime les données!)
read -p "❌ Remove volumes (THIS WILL DELETE ALL DATA)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Removing volumes..."
    docker volume rm geofence_postgres_data 2>/dev/null || true
    docker volume rm geofence_uploads_data 2>/dev/null || true
    docker volume rm geofence_postgres_prod_data 2>/dev/null || true
    docker volume rm geofence_uploads_prod_data 2>/dev/null || true
fi

# Nettoyer le système Docker
echo "Cleaning Docker system..."
docker system prune -f

echo "✅ Cleanup completed"
EOF

# 12. Créer un exemple de configuration nginx
print_info "Création de nginx/nginx.conf..."
cat > nginx/nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream api {
        server api:8080;
    }

    server {
        listen 80;
        server_name localhost;

        location / {
            proxy_pass http://api;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
EOF

# 13. Créer .env.docker.example
print_info "Création de .env.docker.example..."
cat > .env.docker.example << 'EOF'
# .env.docker.example - Template de configuration Docker

# Base de données
DB_NAME=geofence_db
DB_USERNAME=postgres
DB_PASSWORD=changez_ce_mot_de_passe

# JWT (OBLIGATOIRE: générez une clé sécurisée !)
JWT_ENCRYPTION_KEY=changez_cette_cle_jwt_par_une_cle_securisee_de_64_caracteres

# Mail (configurez avec vos vraies valeurs)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_app_password_gmail

# Application
FRONTEND_URL=http://localhost:3000
SERVER_PORT=8080
EOF

# Rendre les scripts exécutables
print_info "Rendre les scripts exécutables..."
chmod +x scripts/*.sh

print_status "Tous les scripts sont maintenant exécutables"

# Créer un README spécifique Docker
print_info "Création de DOCKER.md..."
cat > DOCKER.md << 'EOF'
# 🐳 Docker Setup pour Geofence API

## Quick Start

1. **Configuration**:
   ```bash
   cp .env.docker.example .env.docker
   # Éditez .env.docker avec vos paramètres
   ```

2. **Démarrage développement**:
   ```bash
   ./scripts/start.sh
   ```

3. **Accès**:
   - API: http://localhost:8080
   - Docs: http://localhost:8080/api/v1/docs/index.html
   - PgAdmin: http://localhost:5050

## Scripts disponibles

- `./scripts/start.sh` - Démarrer en développement
- `./scripts/start-prod.sh` - Démarrer en production
- `./scripts/stop.sh` - Arrêter tous les services
- `./scripts/clean.sh` - Nettoyer complètement

## Commandes utiles

```bash
# Voir les logs
docker-compose logs -f api

# Entrer dans le conteneur
docker-compose exec api bash

# Backup de la base
docker-compose exec postgres pg_dump -U postgres geofence_db > backup.sql

# Monitoring
docker stats
```

## Production

1. Configurez les variables d'environnement
2. Changez les mots de passe par défaut
3. Lancez: `./scripts/start-prod.sh`

⚠️ **Important**: Changez TOUJOURS `JWT_ENCRYPTION_KEY` et `DB_PASSWORD` en production !
EOF

print_status "DOCKER.md créé"

echo ""
echo "=========================================="
echo -e "${GREEN}🎉 Configuration Docker terminée !${NC}"
echo "=========================================="
echo ""
echo "📁 Fichiers créés:"
echo "   ├── Dockerfile"
echo "   ├── docker-compose.yml"
echo "   ├── docker-compose.prod.yml"
echo "   ├── .dockerignore"
echo "   ├── .env.docker"
echo "   ├── .env.docker.example"
echo "   ├── DOCKER.md"
echo "   ├── scripts/"
echo "   │   ├── init-db.sql"
echo "   │   ├── start.sh"
echo "   │   ├── start-prod.sh"
echo "   │   ├── stop.sh"
echo "   │   └── clean.sh"
echo "   ├── nginx/"
echo "   │   └── nginx.conf"
echo "   └── src/main/resources/"
echo "       └── application-docker.properties"
echo ""
echo "🚀 Prochaines étapes:"
echo "   1. Éditez .env.docker avec vos paramètres"
echo "   2. Lancez: ./scripts/start.sh"
echo "   3. Accédez à: http://localhost:8080"
echo ""
print_warning "N'oubliez pas de modifier JWT_ENCRYPTION_KEY et les mots de passe !"
echo ""
EOF