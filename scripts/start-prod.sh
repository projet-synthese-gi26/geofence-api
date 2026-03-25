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
