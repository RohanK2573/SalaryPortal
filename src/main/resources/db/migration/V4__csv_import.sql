CREATE TABLE import_batch (
    id UUID PRIMARY KEY,
    owner_email VARCHAR(254) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PREVIEWED', 'COMMITTED', 'EXPIRED')),
    file_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    committed_at TIMESTAMPTZ,
    total_row_count INTEGER NOT NULL CHECK (total_row_count BETWEEN 1 AND 10000),
    version BIGINT NOT NULL DEFAULT 0,
    create_count INTEGER NOT NULL DEFAULT 0 CHECK (create_count >= 0),
    update_count INTEGER NOT NULL DEFAULT 0 CHECK (update_count >= 0),
    no_op_count INTEGER NOT NULL DEFAULT 0 CHECK (no_op_count >= 0),
    invalid_count INTEGER NOT NULL DEFAULT 0 CHECK (invalid_count >= 0)
);

CREATE INDEX idx_import_batch_expiry ON import_batch (expires_at) WHERE status = 'PREVIEWED';

CREATE TABLE import_row (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES import_batch(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL CHECK (row_number >= 2),
    employee_id UUID REFERENCES employee(id),
    employee_version BIGINT,
    latest_salary_record_id UUID REFERENCES salary_record(id),
    latest_salary_version BIGINT,
    employee_code TEXT NOT NULL,
    full_name TEXT,
    country_code TEXT,
    department TEXT,
    job_title TEXT,
    employee_status TEXT,
    annual_base_amount TEXT,
    currency_code TEXT,
    effective_date TEXT,
    action VARCHAR(16) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'NO_OP', 'INVALID')),
    errors_json TEXT NOT NULL DEFAULT '[]',
    profile_changed BOOLEAN NOT NULL DEFAULT FALSE,
    salary_record_proposed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_import_row_batch_number UNIQUE (batch_id, row_number)
);

CREATE INDEX idx_import_row_batch_page ON import_row (batch_id, row_number);

ALTER TABLE audit_event ADD COLUMN import_batch_id UUID REFERENCES import_batch(id);
CREATE INDEX idx_audit_import_batch ON audit_event (import_batch_id) WHERE import_batch_id IS NOT NULL;
