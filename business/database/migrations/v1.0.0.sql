-- v1.0.0: Quartz removed — TIME_SPECIAL_EFFECT_END / TIME_SPECIAL_IS_READY /
-- UNIT_EXPIRED now ride db-scheduler's `scheduled_tasks` table like
-- `mission-run` (task_instance = the domain entity id, task_data NULL).
--
-- Rebuild any in-flight events from the domain tables (the fire times live
-- there, no qrtz blob parsing needed). A past-due execution_time simply fires
-- on the next backend startup, which is the desired behavior.

INSERT INTO scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, version)
SELECT 'TIME_SPECIAL_EFFECT_END', ats.id, NULL, ats.expiring_date, 0, 1
FROM active_time_specials ats
WHERE ats.state = 'ACTIVE';

INSERT INTO scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, version)
SELECT 'TIME_SPECIAL_IS_READY', ats.id, NULL, ats.ready_date, 0, 1
FROM active_time_specials ats
WHERE ats.state = 'RECHARGE' AND ats.ready_date IS NOT NULL;

INSERT INTO scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, version)
SELECT 'UNIT_EXPIRED', outi.id, NULL, outi.expiration, 0, 1
FROM obtained_unit_temporal_information outi;

-- Drop the Quartz job store (children of qrtz_triggers first, then
-- qrtz_triggers, then qrtz_job_details; the rest are standalone).
DROP TABLE IF EXISTS qrtz_blob_triggers;
DROP TABLE IF EXISTS qrtz_cron_triggers;
DROP TABLE IF EXISTS qrtz_simple_triggers;
DROP TABLE IF EXISTS qrtz_simprop_triggers;
DROP TABLE IF EXISTS qrtz_triggers;
DROP TABLE IF EXISTS qrtz_job_details;
DROP TABLE IF EXISTS qrtz_calendars;
DROP TABLE IF EXISTS qrtz_fired_triggers;
DROP TABLE IF EXISTS qrtz_locks;
DROP TABLE IF EXISTS qrtz_paused_trigger_grps;
DROP TABLE IF EXISTS qrtz_scheduler_state;
