-- The rentals module (REN-1 to REN-14). All three tables are created here; the entities for
-- bookings and fault reports arrive with the features that own them.

CREATE TABLE rental_objects (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    description        CLOB,
    bookable           BOOLEAN      NOT NULL DEFAULT TRUE,
    max_duration_hours INTEGER      NOT NULL DEFAULT 24,
    requires_approval  BOOLEAN      NOT NULL DEFAULT FALSE,
    max_days_ahead     INTEGER      NOT NULL DEFAULT 30,
    image_file_id      BIGINT       REFERENCES stored_files (id)
);

-- A booking is the half-open interval [start_at, end_at) in wall-clock time, as haul_out uses.
CREATE TABLE rental_bookings (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    object_id   BIGINT      NOT NULL REFERENCES rental_objects (id),
    person_id   BIGINT      NOT NULL REFERENCES persons (id),
    start_at    TIMESTAMP   NOT NULL,
    end_at      TIMESTAMP   NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    returned_at TIMESTAMP
);

CREATE INDEX idx_rental_bookings_object_start ON rental_bookings (object_id, start_at);

CREATE TABLE rental_fault_reports (
    id          BIGINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id  BIGINT    NOT NULL REFERENCES rental_bookings (id),
    description CLOB      NOT NULL,
    reported_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100)
);
