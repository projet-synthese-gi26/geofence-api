-- =============================================================
-- Script de synchronisation Liquibase
-- Utiliser SI les tables existent déjà et Liquibase échoue
-- =============================================================
-- Ce script marque tous les changesets comme "déjà exécutés"
-- sans recréer les tables existantes
-- =============================================================

-- Créer les tables de tracking Liquibase si elles n'existent pas
CREATE TABLE IF NOT EXISTS databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS databasechangeloglock (
    id INTEGER NOT NULL,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP,
    lockedby VARCHAR(255),
    CONSTRAINT pk_databasechangeloglock PRIMARY KEY (id)
);

-- Initialiser le lock
INSERT INTO databasechangeloglock (id, locked)
VALUES (1, FALSE)
ON CONFLICT (id) DO NOTHING;

-- Vider le changelog si existe déjà (pour recommencer proprement)
TRUNCATE TABLE databasechangelog;

-- Marquer tous les changesets comme exécutés
INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase)
VALUES
('001-create-organizations', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 1, 'EXECUTED', NULL, 'createTable tableName=organizations', '', NULL, '4.9'),
('001-create-role', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 2, 'EXECUTED', NULL, 'createTable tableName=role', '', NULL, '4.9'),
('001-create-alert-type', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 3, 'EXECUTED', NULL, 'createTable tableName=alert_type', '', NULL, '4.9'),
('001-create-users', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 4, 'EXECUTED', NULL, 'createTable tableName=users', '', NULL, '4.9'),
('001-create-organization-users', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 5, 'EXECUTED', NULL, 'createTable tableName=organization_users', '', NULL, '4.9'),
('001-create-vehicle', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 6, 'EXECUTED', NULL, 'createTable tableName=vehicle', '', NULL, '4.9'),
('001-create-vehicle-api-keys', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 7, 'EXECUTED', NULL, 'createTable tableName=vehicle_api_keys', '', NULL, '4.9'),
('001-create-circle-geofence-zone', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 8, 'EXECUTED', NULL, 'createTable tableName=circle_geofence_zone', '', NULL, '4.9'),
('001-create-polygon-geofence-zone', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 9, 'EXECUTED', NULL, 'createTable tableName=polygon_geofence_zone', '', NULL, '4.9'),
('001-add-geometry-columns', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 10, 'EXECUTED', NULL, 'sql', '', NULL, '4.9'),
('001-create-geofence-active-days', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 11, 'EXECUTED', NULL, 'createTable tableName=geofence_active_days', '', NULL, '4.9'),
('001-create-vehicle-geofence-zones', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 12, 'EXECUTED', NULL, 'createTable tableName=vehicle_geofence_zones', '', NULL, '4.9'),
('001-create-locations', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 13, 'EXECUTED', NULL, 'createTable tableName=locations', '', NULL, '4.9'),
('001-create-alert', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 14, 'EXECUTED', NULL, 'createTable tableName=alert', '', NULL, '4.9'),
('001-create-routes', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 15, 'EXECUTED', NULL, 'createTable tableName=routes', '', NULL, '4.9'),
('001-create-route-active-days', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 16, 'EXECUTED', NULL, 'createTable tableName=route_active_days', '', NULL, '4.9'),
('001-create-route-segments', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 17, 'EXECUTED', NULL, 'createTable tableName=route_segments', '', NULL, '4.9'),
('001-create-vehicle-assigned-routes', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 18, 'EXECUTED', NULL, 'createTable tableName=vehicle_assigned_routes', '', NULL, '4.9'),
('001-create-geofence-forks', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 19, 'EXECUTED', NULL, 'createTable tableName=geofence_forks', '', NULL, '4.9'),
('001-create-geofence-share', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 20, 'EXECUTED', NULL, 'createTable tableName=geofence_share', '', NULL, '4.9'),
('001-create-geofence-invite-links', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 21, 'EXECUTED', NULL, 'createTable tableName=geofence_invite_links', '', NULL, '4.9'),
('001-create-validation', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 22, 'EXECUTED', NULL, 'createTable tableName=validation', '', NULL, '4.9'),
('001-create-indexes', 'geofence', 'db/changelog/changes/001-initial-schema.xml', NOW(), 23, 'EXECUTED', NULL, 'sql', '', NULL, '4.9');

-- Vérification
\echo '✅ Synchronisation Liquibase terminée !'
\echo '📊 Changesets enregistrés :'
SELECT COUNT(*) as total_changesets FROM databasechangelog;

\echo ''
\echo '🎯 Vous pouvez maintenant redémarrer l''application'
\echo '   Liquibase ignorera ces changesets car marqués comme EXECUTED'
