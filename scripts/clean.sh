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
