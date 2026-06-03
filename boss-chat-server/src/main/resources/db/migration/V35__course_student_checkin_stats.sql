ALTER TABLE course_student
    ADD COLUMN check_in_count INT NOT NULL DEFAULT 0 AFTER is_new_student,
    ADD COLUMN last_check_in_time DATETIME NULL AFTER check_in_count;

