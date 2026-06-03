DELETE FROM course_analysis_history;
DELETE FROM survey_record;

DROP INDEX idx_course_student_phase_no ON course_student;

ALTER TABLE course_student
    CHANGE COLUMN student_no seat_no VARCHAR(64) NOT NULL DEFAULT '';

CREATE INDEX idx_course_student_phase_seat ON course_student (phase_id, seat_no);
