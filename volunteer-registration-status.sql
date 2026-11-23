ALTER TABLE registration DROP CHECK chk_reg_status;
ALTER TABLE registration ADD CONSTRAINT chk_reg_status CHECK (status IN ('pending','approved','rejected','absent'));
