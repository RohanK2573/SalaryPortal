CREATE TABLE employee (
    id UUID PRIMARY KEY,
    employee_code VARCHAR(64) NOT NULL UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    department VARCHAR(120) NOT NULL,
    job_title VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_employee_name_lower ON employee (lower(full_name));
CREATE INDEX idx_employee_department_lower ON employee (lower(department));
CREATE INDEX idx_employee_country_status ON employee (country_code, status);

CREATE TABLE salary_record (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id),
    annual_base_amount NUMERIC(19,4) NOT NULL CHECK (annual_base_amount > 0),
    currency_code VARCHAR(3) NOT NULL,
    effective_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_salary_employee_effective_date UNIQUE (employee_id, effective_date)
);

CREATE INDEX idx_salary_employee_effective_desc ON salary_record (employee_id, effective_date DESC);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id),
    salary_record_id UUID NOT NULL REFERENCES salary_record(id),
    actor_email VARCHAR(254) NOT NULL,
    event_type VARCHAR(32) NOT NULL CHECK (event_type IN ('SALARY_CREATED', 'SALARY_CORRECTED')),
    source VARCHAR(32) NOT NULL CHECK (source IN ('MANUAL', 'CSV_IMPORT')),
    reason VARCHAR(500),
    before_amount NUMERIC(19,4),
    before_currency VARCHAR(3),
    before_effective_date DATE,
    after_amount NUMERIC(19,4) NOT NULL,
    after_currency VARCHAR(3) NOT NULL,
    after_effective_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_audit_before_values CHECK (
        (event_type = 'SALARY_CREATED' AND before_amount IS NULL AND before_currency IS NULL AND before_effective_date IS NULL)
        OR (event_type = 'SALARY_CORRECTED' AND before_amount IS NOT NULL AND before_currency IS NOT NULL AND before_effective_date IS NOT NULL)
    )
);

CREATE INDEX idx_audit_employee_created_desc ON audit_event (employee_id, created_at DESC);
