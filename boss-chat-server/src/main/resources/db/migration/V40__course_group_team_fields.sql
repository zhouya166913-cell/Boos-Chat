ALTER TABLE course_group
    ADD COLUMN team_name VARCHAR(120) NOT NULL DEFAULT '' AFTER leader_name,
    ADD COLUMN team_slogan VARCHAR(200) NOT NULL DEFAULT '' AFTER team_name;
