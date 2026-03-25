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
