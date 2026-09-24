-- v1.0.0: Quartz removed — TIME_SPECIAL_EFFECT_END / TIME_SPECIAL_IS_READY /
-- UNIT_EXPIRED now ride db-scheduler's `scheduled_tasks` table like
-- `mission-run` (task_instance = the domain entity id, task_data NULL).
--
-- Rebuild any in-flight events from the domain tables (the fire times live
-- there, no qrtz blob parsing needed). A past-due execution_time simply fires
-- on the next backend startup, which is the desired behavior.

INSERT IGNORE INTO scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, version)
SELECT 'TIME_SPECIAL_EFFECT_END', ats.id, NULL, ats.expiring_date, 0, 1
FROM active_time_specials ats
WHERE ats.state = 'ACTIVE';

INSERT IGNORE INTO scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, version)
SELECT 'TIME_SPECIAL_IS_READY', ats.id, NULL, ats.ready_date, 0, 1
FROM active_time_specials ats
WHERE ats.state = 'RECHARGE' AND ats.ready_date IS NOT NULL;

INSERT IGNORE INTO scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, version)
SELECT 'UNIT_EXPIRED', outi.id, NULL, outi.expiration, 0, 1
FROM obtained_unit_temporal_information outi;

-- Drop the Quartz job store (children of qrtz_triggers first, then
-- qrtz_triggers, then qrtz_job_details; the rest are standalone).
-- Live universes have the tables in UPPERCASE; on a case-sensitive server
-- (lower_case_table_names=0, e.g. dc12/dc14's) the lowercase names silently
-- match nothing, so drop both spellings.
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
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;
DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;

-- websocket_events_information.last_sent gains millisecond precision.
ALTER TABLE `websocket_events_information` MODIFY COLUMN `last_sent` DATETIME(3) NOT NULL;
