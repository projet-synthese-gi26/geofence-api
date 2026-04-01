#!/bin/bash
# =============================================================
# Script de redémarrage propre -- Développement local
# =============================================================

set -e  # Arrêter en cas d'erreur

echo "🧹 Nettoyage de l'environnement Docker..."
docker compose down -v 2>/dev/null || true
docker system prune -f

echo ""
echo "🚀 Construction et démarrage des services..."
docker compose up -d --build

echo ""
echo "⏳ Attente du démarrage de PostgreSQL (30s)..."
sleep 30

echo ""
echo "📊 Vérification de l'état des services..."
docker compose ps

echo ""
echo "📝 Logs PostgreSQL (init script):"
docker logs geofence-postgres | tail -20

echo ""
echo "📝 Logs API (Liquibase):"
docker logs geofence-api | tail -50

echo ""
echo "✅ Démarrage terminé !"
echo ""
echo "🔍 Commandes utiles :"
echo "   docker logs -f geofence-api        # Suivre les logs API"
echo "   docker logs -f geofence-postgres   # Suivre les logs PostgreSQL"
echo "   docker compose down -v             # Tout arrêter et supprimer volumes"
echo "   curl http://localhost:8080/actuator/health  # Health check"
