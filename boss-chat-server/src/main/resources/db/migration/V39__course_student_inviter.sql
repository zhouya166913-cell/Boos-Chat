ALTER TABLE course_student
    ADD COLUMN inviter VARCHAR(80) NOT NULL DEFAULT '' AFTER id_card;
