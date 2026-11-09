# Веб-сервис оценки надёжности маршрутов общественного транспорта Санкт-Петербурга

Сервис собирает данные о движении городского транспорта, рассчитывает показатели
надёжности маршрутов и помогает пользователю выбирать поездки с учётом
исторической статистики задержек и отмен.

> **Текущий статус:** реализованы слой данных (схема PostgreSQL, миграции
> Flyway, JPA-сущности и репозитории), расчёт надёжности и REST API для
> сохранённых поездок, текущего статуса и оценки маршрута. Frontend — следующий
> этап проекта.

## Стек технологий

- **Backend:** Java 21, Spring Boot 3.5, Spring Web, Spring Security,
  Spring Data JPA, Hibernate
- **Database:** PostgreSQL 16, Flyway
- **Тесты:** JUnit 5, AssertJ, MockMvc, встроенный PostgreSQL (без Docker)
- **Frontend:** JavaScript (ES2020+), HTML5, CSS3
- **Инфраструктура:** Docker Compose для локальной БД

## Структура репозитория

```text
backend/                       Spring Boot-приложение
  src/main/java/ru/spb/transit/
    config/                    настройки Spring Security
    domain/                    JPA-сущности
    repository/                Spring Data JPA-репозитории
    reliability/               расчёт показателей надёжности
    security/                  аутентификация и демо-пользователь
    service/                   бизнес-логика поездок, статуса и надёжности
    web/                       REST-контроллеры, DTO и обработка ошибок
  src/main/resources/
    application.yml            конфигурация БД, Flyway, JPA, Basic Auth
    db/migration/              SQL-миграции Flyway
  src/test/java/               unit- и интеграционные тесты
docs/api.md                    описание REST API
docs/database.md               описание схемы, индексов и миграций
docker-compose.yml             PostgreSQL для локального запуска
.env.example                   пример переменных окружения
```

## Быстрый старт

### 1. Поднять PostgreSQL

```bash
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
docker compose up -d postgres
```

Посмотреть БД можно через pgAdmin: он включён в compose как необязательный
сервис.

```bash
docker compose --profile tools up -d
```

Веб-интерфейс — `http://localhost:5050`, логин `admin@example.com`, пароль
`spb_admin`. При добавлении сервера укажите host `postgres`, базу
`spb_transit`, пользователя `spb_app` и пароль `spb_app`.

### 2. Запустить backend

```bash
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Flyway автоматически применит миграции из `backend/src/main/resources/db/migration`.
Приложение будет доступно на `http://localhost:8080`, health-check —
`http://localhost:8080/actuator/health`.

При первом старте создаётся демонстрационный пользователь для Basic Auth:

| E-mail | Пароль |
|---|---|
| `demo@spb.local` | `demo` |

Проверить API можно так:

```bash
curl -u demo@spb.local:demo http://localhost:8080/api/trips
```

```bash
curl -u demo@spb.local:demo \
  -H "Content-Type: application/json" \
  -d '{"name":"Дом — Университет","routeId":"321","fromStopId":"12345","toStopId":"67890","requiredArrivalTime":"08:40"}' \
  http://localhost:8080/api/trips
```

Полное описание методов, форматов запросов и ответов, а также правил расчёта
статуса и надёжности — в [docs/api.md](docs/api.md).

Фронтенд на HTML/CSS/JS обращается к API напрямую через `fetch`. Если он открыт
с другого origin (например, через Live Server на `http://localhost:5500`),
сервер уже разрешает типовые локальные адреса из `APP_CORS_ALLOWED_ORIGINS`.
В production укажите там точный адрес фронтенда. Открывать страницу как
`file://` не рекомендуется — используйте локальный HTTP-сервер или раздавайте
статику из Spring Boot, тогда CORS не понадобится.

### 3. Запустить тесты

```bash
cd backend
./mvnw test                   # Windows: .\mvnw.cmd test
```

Интеграционные тесты запускают встроенный PostgreSQL 16.15, поэтому Docker для
них не нужен.

## Конфигурация

Параметры подключения читаются из переменных окружения. Для локального запуска
удобно скопировать `.env.example` в `.env`: Spring Boot импортирует этот файл,
а Docker Compose использует его для создания БД. Реальные переменные окружения
имеют приоритет над `.env`. Значения по умолчанию перечислены в
`backend/src/main/resources/application.yml`:

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/spb_transit` | JDBC URL |
| `DB_USER` | `spb_app` | Пользователь БД |
| `DB_PASSWORD` | `spb_app` | Пароль БД |
| `DB_POOL_MAX_SIZE` | `10` | Максимум соединений HikariCP |
| `DB_POOL_MIN_IDLE` | `2` | Минимум idle-соединений |
| `SERVER_PORT` | `8080` | Порт приложения |
| `APP_DEMO_USER_ENABLED` | `true` | Создавать демо-пользователя (в production — `false`) |
| `APP_DEMO_USER_EMAIL` | `demo@spb.local` | E-mail демо-пользователя |
| `APP_DEMO_USER_PASSWORD` | `demo` | Пароль демо-пользователя |
| `STATUS_CURRENT_WINDOW` | `PT5M` | Максимальный возраст данных для статуса `current` |
| `STATUS_STALE_WINDOW` | `PT60M` | Максимальный возраст данных для статуса `stale` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5500,...` | Origin фронтенда через запятую; в production — точный адрес |

При изменении `POSTGRES_PORT` или учётных данных в `.env` обновите
соответствующие `DB_*`, чтобы backend подключался к той же БД.

## Схема данных

Реализованы таблицы:

- `users` — пользователи;
- `routes` — маршруты;
- `stops` — остановочные пункты;
- `route_stops` — упорядоченные остановки маршрута по направлениям;
- `saved_trips` — сохранённые пользователем поездки;
- `historical_statistics` — суточные агрегаты надёжности;
- `transport_updates` — staging-таблица сырых событий внешнего агрегатора.

Подробное описание колонок, ограничений, индексов, репозиториев и политики
миграций — в [docs/database.md](docs/database.md).

## Команда проекта

- Крыжановский Никита
- Конев Дмитрий
- Казаков Ярослав
- Образцов Роман
