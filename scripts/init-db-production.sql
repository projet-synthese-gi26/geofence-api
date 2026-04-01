-- =============================================================
-- Script d'initialisation PostgreSQL -- Geofence Platform
-- À exécuter UNE SEULE FOIS sur le serveur par l'administrateur
-- =============================================================
-- ⚠️ IMPORTANT : Remplacer les mots de passe avant exécution !
-- =============================================================

-- Extensions PostGIS (si pas encore présentes)
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================
-- Création des utilisateurs
-- ⚠️ REMPLACER LES MOTS DE PASSE CI-DESSOUS !
-- =============================================================

-- Utilisateur applicatif (lecture/écriture DML uniquement)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'geofence_app') THEN
        CREATE USER geofence_app WITH PASSWORD 'APP_PASSWORD_TO_CHANGE';
    END IF;
END $$;

-- Utilisateur Liquibase (DDL : création/modification de tables)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'geofence_liquibase') THEN
        CREATE USER geofence_liquibase WITH PASSWORD 'LIQUIBASE_PASSWORD_TO_CHANGE';
    END IF;
END $$;

-- =============================================================
-- Droits de connexion
-- =============================================================

GRANT CONNECT ON DATABASE geofence_db TO geofence_app;
GRANT CONNECT ON DATABASE geofence_db TO geofence_liquibase;

-- Usage du schéma public
GRANT USAGE ON SCHEMA public TO geofence_app;
GRANT USAGE, CREATE ON SCHEMA public TO geofence_liquibase;

-- =============================================================
-- Droits Liquibase (DDL complet)
-- =============================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO geofence_liquibase;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO geofence_liquibase;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO geofence_liquibase;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES TO geofence_liquibase;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON SEQUENCES TO geofence_liquibase;

-- =============================================================
-- Droits applicatifs (DML uniquement)
-- =============================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO geofence_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO geofence_app;

ALTER DEFAULT PRIVILEGES FOR ROLE geofence_liquibase IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO geofence_app;
ALTER DEFAULT PRIVILEGES FOR ROLE geofence_liquibase IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO geofence_app;

-- =============================================================
-- Vérification
-- =============================================================
\echo '✅ Vérification des utilisateurs créés :'
SELECT rolname, rolcanlogin FROM pg_roles WHERE rolname IN ('geofence_app', 'geofence_liquibase');

\echo ''
\echo '✅ Extensions installées :'
SELECT extname FROM pg_extension WHERE extname IN ('postgis', 'postgis_topology', 'uuid-ossp', 'pgcrypto');

\echo ''
\echo '🎯 Configuration terminée !'
\echo '📝 N''oubliez pas de configurer les variables d''environnement :'
\echo '   - DB_USERNAME=geofence_app'
\echo '   - DB_PASSWORD=<mot_de_passe_app_choisi>'
\echo '   - DB_LIQUIBASE_USERNAME=geofence_liquibase'
\echo '   - DB_LIQUIBASE_PASSWORD=<mot_de_passe_liquibase_choisi>'
