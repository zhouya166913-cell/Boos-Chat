ALTER TABLE survey_record
    ADD COLUMN is_new_student TINYINT NULL AFTER id_card;

UPDATE survey_record sr
LEFT JOIN course_student cs ON cs.id = sr.student_id
SET sr.is_new_student = cs.is_new_student
WHERE sr.is_new_student IS NULL
  AND cs.id IS NOT NULL;

CREATE TABLE IF NOT EXISTS course_analysis_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phase_id BIGINT NOT NULL,
    agent_id BIGINT NULL,
    agent_name VARCHAR(120) NOT NULL DEFAULT '',
    content MEDIUMTEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course_analysis_phase_time (phase_id, create_time),
    INDEX idx_course_analysis_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
