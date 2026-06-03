CREATE TABLE IF NOT EXISTS course_group (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phase_id BIGINT NOT NULL,
    group_name VARCHAR(120) NOT NULL,
    leader_name VARCHAR(80) NOT NULL DEFAULT '',
    remark VARCHAR(500) NOT NULL DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_group_phase_name (phase_id, group_name),
    INDEX idx_course_group_phase_sort (phase_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE course_student
    ADD COLUMN group_id BIGINT NULL AFTER phase_id,
    ADD COLUMN student_no VARCHAR(64) NOT NULL DEFAULT '' AFTER group_id;

CREATE INDEX idx_course_student_group ON course_student (group_id);
CREATE INDEX idx_course_student_phase_no ON course_student (phase_id, student_no);
