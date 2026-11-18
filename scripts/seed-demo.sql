-- Демонстрационные данные для проверки этапов 1 и 2.
-- Скрипт идемпотентен: повторный запуск не создаёт дубликаты.
--
-- Маршрут 321, направление 0:
--   1. Невский проспект (12345) — отправление
--   2. Университет (67890)      — назначение
--
-- Статистика за 30 дней подобрана так, чтобы один и тот же маршрут
-- и остановка назначения давали разные уровни риска в зависимости
-- от periodDays:
--   periodDays = 7   -> low
--   periodDays = 20  -> medium
--   periodDays = 30  -> high

BEGIN;

INSERT INTO routes (external_id, name, type)
VALUES ('321', 'Трамвай 21', 'TRAM')
ON CONFLICT (external_id) DO UPDATE
SET name = EXCLUDED.name,
    type = EXCLUDED.type;

INSERT INTO stops (external_id, name, latitude, longitude)
VALUES
    ('12345', 'Невский проспект', 59.934280, 30.335098),
    ('67890', 'Университет', 59.943170, 30.298611)
ON CONFLICT (external_id) DO UPDATE
SET name = EXCLUDED.name,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude;

INSERT INTO route_stops (route_id, direction_id, sequence_number, stop_id)
SELECT r.id, 0, v.sequence_number, s.id
FROM routes r
JOIN (VALUES (1, '12345'), (2, '67890')) AS v(sequence_number, external_id)
    ON TRUE
JOIN stops s ON s.external_id = v.external_id
WHERE r.external_id = '321'
ON CONFLICT (route_id, direction_id, sequence_number) DO UPDATE
SET stop_id = EXCLUDED.stop_id;

-- 30 суточных записей:
--   дни 0..6   (7 дней)  — низкий риск;
--   дни 7..19  (13 дней) — средний риск;
--   дни 20..29 (10 дней) — высокая задержка и отмены.
-- Суммарно за 30 дней доля опозданий превышает 25 %, поэтому риск high.
INSERT INTO historical_statistics (
    route_id, stop_id, stat_date,
    trips_count, delayed_trips, cancelled_trips,
    average_delay, median_delay, max_delay
)
SELECT
    r.id,
    s.id,
    current_date - g,
    20,
    CASE WHEN g <= 6 THEN 1 WHEN g <= 19 THEN 4 ELSE 10 END,
    CASE WHEN g <= 6 THEN 0 WHEN g <= 19 THEN 0 ELSE 2 END,
    CASE WHEN g <= 6 THEN 2.00 WHEN g <= 19 THEN 5.00 ELSE 12.00 END,
    CASE WHEN g <= 6 THEN 2.00 WHEN g <= 19 THEN 4.00 ELSE 11.00 END,
    CASE WHEN g <= 6 THEN 5.00 WHEN g <= 19 THEN 9.00 ELSE 20.00 END
FROM generate_series(0, 29) AS g
JOIN routes r ON r.external_id = '321'
JOIN stops s ON s.external_id = '67890'
ON CONFLICT (route_id, stop_id, stat_date) DO UPDATE
SET trips_count = EXCLUDED.trips_count,
    delayed_trips = EXCLUDED.delayed_trips,
    cancelled_trips = EXCLUDED.cancelled_trips,
    average_delay = EXCLUDED.average_delay,
    median_delay = EXCLUDED.median_delay,
    max_delay = EXCLUDED.max_delay;

INSERT INTO saved_trips (
    user_id, route_id, direction_id,
    from_stop_id, to_stop_id, name, arrival_time
)
SELECT
    u.id,
    r.id,
    0,
    from_stop.id,
    to_stop.id,
    'Дом — Университет (тест)',
    TIME '08:40'
FROM users u
JOIN routes r ON r.external_id = '321'
JOIN stops from_stop ON from_stop.external_id = '12345'
JOIN stops to_stop ON to_stop.external_id = '67890'
WHERE u.email = 'demo@spb.local'
  AND NOT EXISTS (
      SELECT 1
      FROM saved_trips st
      WHERE st.user_id = u.id
        AND st.name = 'Дом — Университет (тест)'
  );

COMMIT;
