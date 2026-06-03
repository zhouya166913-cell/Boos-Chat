ALTER TABLE survey_record
    ADD COLUMN id_card VARCHAR(32) NOT NULL DEFAULT '' AFTER phone;

CREATE INDEX idx_survey_record_phase_name ON survey_record (phase_id, customer_name);
