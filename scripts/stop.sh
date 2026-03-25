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
