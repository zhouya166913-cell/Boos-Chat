ALTER TABLE course_student
    MODIFY phone VARCHAR(20) NULL DEFAULT NULL;

UPDATE course_student
SET phone = NULL
WHERE phone = '';
