-- Базовые справочники и пользовательские данные.
-- Миграция создаёт только структуру; данные загружаются агрегатором или тестами.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    display_name  VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_email_not_blank CHECK (btrim(email) <> '')
);

COMMENT ON TABLE users IS 'Пользователи сервиса';
COMMENT ON COLUMN users.email IS 'Email; уникальность проверяется без учёта регистра';
COMMENT ON COLUMN users.password_hash IS 'Хеш пароля; NULL, пока пользователь создан без локального пароля';

CREATE TABLE routes (
    id          BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_routes_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_routes_type CHECK (type IN ('BUS', 'TRAM', 'TROLLEYBUS', 'METRO', 'SUBURBAN_RAIL', 'OTHER'))
);

COMMENT ON TABLE routes IS 'Маршруты общественного транспорта';
COMMENT ON COLUMN routes.external_id IS 'Идентификатор маршрута во внешнем источнике (GTFS route_id или аналог)';

CREATE TABLE stops (
    id          BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    latitude    NUMERIC(9, 6) NOT NULL,
    longitude   NUMERIC(9, 6) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_stops_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_stops_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_stops_longitude CHECK (longitude BETWEEN -180 AND 180)
);

COMMENT ON TABLE stops IS 'Остановочные пункты';
COMMENT ON COLUMN stops.external_id IS 'Идентификатор остановки во внешнем источнике (GTFS stop_id или аналог)';

CREATE TABLE route_stops (
    route_id        BIGINT  NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    direction_id    SMALLINT NOT NULL DEFAULT 0,
    sequence_number INTEGER NOT NULL,
    stop_id         BIGINT  NOT NULL REFERENCES stops (id) ON DELETE RESTRICT,
    PRIMARY KEY (route_id, direction_id, sequence_number),
    CONSTRAINT ck_route_stops_direction CHECK (direction_id >= 0),
    CONSTRAINT ck_route_stops_sequence CHECK (sequence_number >= 0)
);

COMMENT ON TABLE route_stops IS 'Упорядоченные остановки маршрута по направлениям';
COMMENT ON COLUMN route_stops.direction_id IS 'Направление движения: 0 или 1, как в GTFS direction_id';

CREATE TABLE saved_trips (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    route_id       BIGINT      NOT NULL REFERENCES routes (id) ON DELETE RESTRICT,
    direction_id   SMALLINT    NOT NULL DEFAULT 0,
    from_stop_id   BIGINT      NOT NULL REFERENCES stops (id) ON DELETE RESTRICT,
    to_stop_id     BIGINT      NOT NULL REFERENCES stops (id) ON DELETE RESTRICT,
    name           VARCHAR(255) NOT NULL,
    arrival_time   TIME        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_saved_trips_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_saved_trips_different_stops CHECK (from_stop_id <> to_stop_id),
    CONSTRAINT ck_saved_trips_direction CHECK (direction_id >= 0)
);

COMMENT ON TABLE saved_trips IS 'Поездки, сохранённые пользователем';
COMMENT ON COLUMN saved_trips.arrival_time IS 'Требуемое локальное время прибытия (Europe/Moscow)';

CREATE TABLE historical_statistics (
    id               BIGSERIAL PRIMARY KEY,
    route_id         BIGINT      NOT NULL REFERENCES routes (id) ON DELETE RESTRICT,
    stop_id          BIGINT      NOT NULL REFERENCES stops (id) ON DELETE RESTRICT,
    stat_date        DATE        NOT NULL,
    trips_count      INTEGER     NOT NULL DEFAULT 0,
    delayed_trips    INTEGER     NOT NULL DEFAULT 0,
    cancelled_trips  INTEGER     NOT NULL DEFAULT 0,
    average_delay    NUMERIC(8, 2),
    median_delay     NUMERIC(8, 2),
    max_delay        NUMERIC(8, 2),
    computed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_historical_trips_count CHECK (trips_count >= 0),
    CONSTRAINT ck_historical_delayed_trips CHECK (delayed_trips >= 0 AND delayed_trips <= trips_count),
    CONSTRAINT ck_historical_cancelled_trips CHECK (cancelled_trips >= 0 AND cancelled_trips <= trips_count),
    CONSTRAINT ck_historical_status_sum CHECK (delayed_trips + cancelled_trips <= trips_count)
);

COMMENT ON TABLE historical_statistics IS 'Суточные агрегаты надёжности по маршруту и остановке';

CREATE TABLE transport_updates (
    id                  BIGSERIAL PRIMARY KEY,
    source              VARCHAR(50)  NOT NULL,
    route_external_id   VARCHAR(100),
    stop_external_id    VARCHAR(100),
    trip_external_id    VARCHAR(100),
    vehicle_external_id VARCHAR(100),
    event_type          VARCHAR(30)  NOT NULL,
    observed_at         TIMESTAMPTZ  NOT NULL,
    scheduled_at        TIMESTAMPTZ,
    actual_at           TIMESTAMPTZ,
    delay_seconds       INTEGER,
    status              VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    payload             JSONB        NOT NULL,
    received_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_transport_updates_event_type CHECK (event_type IN ('VEHICLE_POSITION', 'TRIP_UPDATE', 'ALERT')),
    CONSTRAINT ck_transport_updates_status CHECK (status IN ('ON_TIME', 'DELAYED', 'CANCELLED', 'UNKNOWN')),
    CONSTRAINT ck_transport_updates_delay CHECK (delay_seconds IS NULL OR delay_seconds BETWEEN -86400 AND 86400)
);

COMMENT ON TABLE transport_updates IS 'Сырые наблюдения агрегатора; хранятся ограниченное время для расчёта агрегатов и диагностики';
COMMENT ON COLUMN transport_updates.payload IS 'Исходный JSON внешнего API для повторной обработки';
