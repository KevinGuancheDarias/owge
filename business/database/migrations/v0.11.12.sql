-- Fix stale mission_id references in obtained_units that point at already-deleted missions rows.
-- These were left behind because no foreign key existed on obtained_units.mission_id; the clean-up
-- nullifies the dangling references and then adds the FK so future deletions are handled atomically.

UPDATE obtained_units ou LEFT JOIN missions m ON m.id = ou.mission_id
SET ou.mission_id = NULL, ou.target_planet = NULL
WHERE ou.mission_id IS NOT NULL AND m.id IS NULL;

ALTER TABLE obtained_units ADD CONSTRAINT obtained_units_mission_fk
  FOREIGN KEY (mission_id) REFERENCES missions (id) ON DELETE SET NULL;
