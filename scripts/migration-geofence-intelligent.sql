-- Migration pour ajouter les fonctionnalités de géofence intelligente
-- Date: 2025-01-29

-- Ajouter les nouvelles colonnes aux tables circle_geofence_zone et polygon_geofence_zone

-- Pour circle_geofence_zone
ALTER TABLE circle_geofence_zone 
ADD COLUMN IF NOT EXISTS is_temporal_enabled BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS start_time TIME,
ADD COLUMN IF NOT EXISTS end_time TIME,
ADD COLUMN IF NOT EXISTS is_conditional_enabled BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS max_speed DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS max_dwell_time INTEGER,
ADD COLUMN IF NOT EXISTS min_dwell_time INTEGER,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- Pour polygon_geofence_zone
ALTER TABLE polygon_geofence_zone 
ADD COLUMN IF NOT EXISTS is_temporal_enabled BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS start_time TIME,
ADD COLUMN IF NOT EXISTS end_time TIME,
ADD COLUMN IF NOT EXISTS is_conditional_enabled BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS max_speed DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS max_dwell_time INTEGER,
ADD COLUMN IF NOT EXISTS min_dwell_time INTEGER,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- Créer la table pour les jours actifs (pour chaque zone)
CREATE TABLE IF NOT EXISTS geofence_active_days (
    geofence_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    FOREIGN KEY (geofence_id) REFERENCES circle_geofence_zone(id) ON DELETE CASCADE
);

-- Ajouter la contrainte pour polygon_geofence_zone aussi
-- Note: Dans une vraie migration, on utiliserait une approche différente pour la FK polymorphe

-- Ajouter de nouvelles colonnes à la table Alert pour les données enrichies
ALTER TABLE alert 
ADD COLUMN IF NOT EXISTS speed DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS dwell_time_minutes INTEGER,
ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS severity VARCHAR(20) DEFAULT 'INFO',
ADD COLUMN IF NOT EXISTS additional_data TEXT;

-- Mettre à jour les données existantes pour avoir des valeurs par défaut cohérentes
UPDATE circle_geofence_zone 
SET is_active = true, 
    is_temporal_enabled = false, 
    is_conditional_enabled = false,
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE is_active IS NULL;

UPDATE polygon_geofence_zone 
SET is_active = true, 
    is_temporal_enabled = false, 
    is_conditional_enabled = false,
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE is_active IS NULL;

-- Insérer les jours par défaut (tous les jours de la semaine) pour les zones existantes
INSERT INTO geofence_active_days (geofence_id, day_of_week)
SELECT id, 'MONDAY' FROM circle_geofence_zone WHERE id NOT IN (SELECT geofence_id FROM geofence_active_days);

INSERT INTO geofence_active_days (geofence_id, day_of_week)
SELECT id, 'TUESDAY' FROM circle_geofence_zone WHERE id NOT IN (SELECT geofence_id FROM geofence_active_days WHERE day_of_week = 'TUESDAY');

INSERT INTO geofence_active_days (geofence_id, day_of_week)
SELECT id, 'WEDNESDAY' FROM circle_geofence_zone WHERE id NOT IN (SELECT geofence_id FROM geofence_active_days WHERE day_of_week = 'WEDNESDAY');

INSERT INTO geofence_active_days (geofence_id, day_of_week)
SELECT id, 'THURSDAY' FROM circle_geofence_zone WHERE id NOT IN (SELECT geofence_id FROM geofence_active_days WHERE day_of_week = 'THURSDAY');

INSERT INTO geofence_active_days (geofence_id, day_of_week)
SELECT id, 'FRIDAY' FROM circle_geofence_zone WHERE id NOT IN (SELECT geofence_id FROM geofence_active_days WHERE day_of_week = 'FRIDAY');

INSERT INTO geofence_active_days (geofence_id, day_of_week)
SELECT id, 'SATURDAY' FROM circle_geofence_zone WHERE id NOT IN (SELECT geofence_id FROM geofence_active_days WHERE day_of_week = 'SATURDAY');

INSERT INTO geofence_active_days (geofence_id, day_of_week)
SELECT id, 'SUNDAY' FROM circle_geofence_zone WHERE id NOT IN (SELECT geofence_id FROM geofence_active_days WHERE day_of_week = 'SUNDAY');

-- Log de confirmation
SELECT 'Migration des géofences intelligentes terminée!' AS status;