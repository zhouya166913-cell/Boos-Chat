CREATE TABLE IF NOT EXISTS course_phase (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phase_code VARCHAR(64) NOT NULL UNIQUE,
    phase_name VARCHAR(120) NOT NULL,
    course_name VARCHAR(120) NOT NULL DEFAULT 'AI Operations Bootcamp',
    survey_path VARCHAR(255) NOT NULL DEFAULT '/survey/enterprise-diagnosis.html',
    qr_image_url VARCHAR(500) NOT NULL DEFAULT '',
    enabled TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_phase_enabled_time (enabled, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course_student (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phase_id BIGINT NOT NULL,
    student_name VARCHAR(80) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    id_card VARCHAR(32) NOT NULL DEFAULT '',
    is_new_student TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student_phase_phone (phase_id, phone),
    INDEX idx_course_student_phase (phase_id),
    INDEX idx_course_student_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE survey_record
    ADD COLUMN phase_id BIGINT NULL AFTER id,
    ADD COLUMN student_id BIGINT NULL AFTER phase_id;

CREATE INDEX idx_survey_record_phase_time ON survey_record (phase_id, create_time);
CREATE INDEX idx_survey_record_student ON survey_record (student_id);
