CREATE TABLE winter_seasons (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    year       INT          NOT NULL,
    name       VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date   DATE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PLANNING'
);

CREATE TABLE haul_out_slots (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    season_id  BIGINT NOT NULL REFERENCES winter_seasons(id),
    slot_date  DATE   NOT NULL,
    start_time TIME   NOT NULL,
    end_time   TIME   NOT NULL,
    capacity   INT    NOT NULL DEFAULT 1
);

CREATE TABLE haul_out_bookings (
    id           BIGINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slot_id      BIGINT    NOT NULL REFERENCES haul_out_slots(id),
    boat_id      BIGINT    NOT NULL REFERENCES boats(id),
    person_id    BIGINT    NOT NULL REFERENCES persons(id),
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status       VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
);

CREATE TABLE storage_yards (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    season_id            BIGINT       NOT NULL REFERENCES winter_seasons(id),
    name                 VARCHAR(255) NOT NULL,
    background_image_url VARCHAR(1024),
    origin_pixel_x       DOUBLE PRECISION,
    origin_pixel_y       DOUBLE PRECISION,
    pixels_per_meter     DOUBLE PRECISION,
    lane_margin_m        DECIMAL(5,2) DEFAULT 0.50
);

CREATE TABLE storage_yard_boundary_points (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    yard_id     BIGINT NOT NULL REFERENCES storage_yards(id),
    point_order INT    NOT NULL,
    x_meters    DECIMAL(10,3) NOT NULL,
    y_meters    DECIMAL(10,3) NOT NULL
);

CREATE TABLE storage_packing_groups (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    yard_id        BIGINT       NOT NULL REFERENCES storage_yards(id),
    name           VARCHAR(255) NOT NULL,
    retrieval_note VARCHAR(1000)
);

CREATE TABLE storage_placements (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    yard_id          BIGINT NOT NULL REFERENCES storage_yards(id),
    booking_id       BIGINT NOT NULL REFERENCES haul_out_bookings(id),
    x_meters         DECIMAL(10,3) NOT NULL DEFAULT 0,
    y_meters         DECIMAL(10,3) NOT NULL DEFAULT 0,
    rotation_deg     DECIMAL(7,3)  NOT NULL DEFAULT 0,
    width_meters     DECIMAL(6,2)  NOT NULL,
    length_meters    DECIMAL(6,2)  NOT NULL,
    packing_group_id BIGINT REFERENCES storage_packing_groups(id),
    order_in_group   INT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PLANNED'
);

CREATE TABLE pricing_rules (
    id                           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    season_id                    BIGINT        NOT NULL REFERENCES winter_seasons(id),
    price_per_sqm                DECIMAL(10,2) NOT NULL,
    extra_width_threshold_m      DECIMAL(6,2),
    extra_width_surcharge_per_dm DECIMAL(10,2),
    extra_length_threshold_m     DECIMAL(6,2),
    extra_length_surcharge_per_dm DECIMAL(10,2),
    min_price                    DECIMAL(10,2)
);
