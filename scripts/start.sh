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
