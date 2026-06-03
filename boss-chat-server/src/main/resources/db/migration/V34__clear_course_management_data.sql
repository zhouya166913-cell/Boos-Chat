-- Clear course operation data only.
-- Keep system users, permissions, model providers, models, API keys, agents,
-- image storage and other management configuration unchanged.
--
-- This also clears old survey records created before the course-management merge,
-- because those records live in survey_record with no phase ownership.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM course_analysis_history;
DELETE FROM survey_record;
DELETE FROM course_student;
DELETE FROM course_phase;

ALTER TABLE course_analysis_history AUTO_INCREMENT = 1;
ALTER TABLE survey_record AUTO_INCREMENT = 1;
ALTER TABLE course_student AUTO_INCREMENT = 1;
ALTER TABLE course_phase AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
