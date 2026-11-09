-- Индексы под основные операции чтения.
-- Составные первичные ключи уже создают индексы и здесь не дублируются.

CREATE UNIQUE INDEX ux_users_email_lower
    ON users (lower(email));

CREATE UNIQUE INDEX ux_routes_external_id
    ON routes (external_id);

CREATE UNIQUE INDEX ux_stops_external_id
    ON stops (external_id);

CREATE INDEX ix_stops_name_trgm
    ON stops USING GIN (name gin_trgm_ops);

CREATE INDEX ix_saved_trips_user_created
    ON saved_trips (user_id, created_at DESC);

CREATE INDEX ix_saved_trips_route
    ON saved_trips (route_id);

CREATE INDEX ix_saved_trips_from_stop
    ON saved_trips (from_stop_id);

CREATE INDEX ix_saved_trips_to_stop
    ON saved_trips (to_stop_id);

CREATE INDEX ix_route_stops_stop
    ON route_stops (stop_id);

CREATE UNIQUE INDEX ux_historical_statistics_route_stop_date
    ON historical_statistics (route_id, stop_id, stat_date);

CREATE INDEX ix_historical_statistics_route_date
    ON historical_statistics (route_id, stat_date);

CREATE INDEX ix_historical_statistics_stop_date
    ON historical_statistics (stop_id, stat_date);

CREATE INDEX ix_historical_statistics_stat_date_brin
    ON historical_statistics USING BRIN (stat_date);

CREATE INDEX ix_transport_updates_route_observed
    ON transport_updates (route_external_id, observed_at DESC);

CREATE INDEX ix_transport_updates_stop_observed
    ON transport_updates (stop_external_id, observed_at DESC);

CREATE INDEX ix_transport_updates_observed_brin
    ON transport_updates USING BRIN (observed_at);
