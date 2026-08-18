CREATE TABLE app_users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(50) NOT NULL
);

CREATE TABLE persons (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    phone       VARCHAR(50)
);

CREATE TABLE boats (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    registration_number VARCHAR(100),
    length_m            NUMERIC(6,2) NOT NULL,
    width_m             NUMERIC(6,2) NOT NULL,
    draft_m             NUMERIC(6,2),
    owner_id            BIGINT NOT NULL REFERENCES persons(id)
);

CREATE TABLE docks (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE slips (
    id          BIGSERIAL PRIMARY KEY,
    slip_number VARCHAR(20) NOT NULL,
    max_length_m NUMERIC(6,2) NOT NULL,
    max_width_m  NUMERIC(6,2) NOT NULL,
    max_draft_m  NUMERIC(6,2),
    dock_id     BIGINT NOT NULL REFERENCES docks(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    UNIQUE (dock_id, slip_number)
);

CREATE TABLE assignments (
    id              BIGSERIAL PRIMARY KEY,
    boat_id         BIGINT NOT NULL REFERENCES boats(id),
    slip_id         BIGINT NOT NULL REFERENCES slips(id),
    assigned_date   DATE NOT NULL,
    end_date        DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE queue_entries (
    id              BIGSERIAL PRIMARY KEY,
    person_id       BIGINT NOT NULL REFERENCES persons(id),
    boat_id         BIGINT NOT NULL REFERENCES boats(id),
    requested_date  TIMESTAMP NOT NULL DEFAULT NOW(),
    notes           TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING'
);

CREATE TABLE mail_threads (
    id                  BIGSERIAL PRIMARY KEY,
    gmail_thread_id     VARCHAR(255) NOT NULL UNIQUE,
    subject             VARCHAR(500),
    from_email          VARCHAR(255),
    received_at         TIMESTAMP NOT NULL,
    raw_content         TEXT,
    ai_suggested_reply  TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'NEW',
    linked_person_id    BIGINT REFERENCES persons(id)
);
