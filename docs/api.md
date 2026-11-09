# REST API

Сервис предоставляет REST API для управления сохранёнными поездками, получения
текущего статуса поездки и оценки надёжности маршрута по исторической
статистике.

Базовый адрес локального запуска: `http://localhost:8080`.

## Аутентификация

Все методы `/api/**` требуют аутентификации по схеме **HTTP Basic**. Учётные
данные проверяются по таблице `users`, пароли хранятся в виде BCrypt-хеша.

Для локального запуска при первом старте создаётся демонстрационный
пользователь:

| Поле | Значение |
|---|---|
| E-mail | `demo@spb.local` |
| Пароль | `demo` |

Демонстрационного пользователя можно переименовать или отключить переменными
окружения `APP_DEMO_USER_EMAIL`, `APP_DEMO_USER_PASSWORD` и
`APP_DEMO_USER_ENABLED`. **В production демо-пользователь должен быть
отключён:** `APP_DEMO_USER_ENABLED=false`.

Без заголовка `Authorization` или с неверными учётными данными API возвращает
`401 Unauthorized`. Сессия не создаётся: каждый запрос должен содержать
заголовок Basic Auth.

```http
Authorization: Basic ZGVtb0BzcGIubG9jYWw6ZGVtbw==
```

Публично доступны только служебные endpoints `/actuator/health` и
`/actuator/info`.

## Вызовы из браузера (CORS)

API не зависит от фронтенд-фреймворка: подойдёт любой современный JavaScript
(`fetch`, `XMLHttpRequest`). Учётные данные передаются в заголовке
`Authorization`:

```js
const authorization = "Basic " + btoa("demo@spb.local:demo");

const response = await fetch("http://localhost:8080/api/trips", {
  headers: { Authorization: authorization }
});

if (response.status === 401) {
  // пользователь не авторизован — показать форму входа
}

const data = await response.json();
```

Если страница обслуживается с другого origin (например, VS Code Live Server на
`http://localhost:5500`), браузер выполнит preflight-запрос `OPTIONS`. Сервер
разрешает его без аутентификации и возвращает заголовки CORS:

- разрешённые методы: `GET`, `POST`, `DELETE`, `OPTIONS`;
- разрешённые заголовки: `Authorization`, `Content-Type`;
- список origin задаётся переменной `APP_CORS_ALLOWED_ORIGINS` (значения через
  запятую).

По умолчанию разрешены типовые локальные адреса: `http://localhost:5500`,
`http://127.0.0.1:5500`, `http://localhost:8000`, `http://localhost:3000`,
`http://localhost:5173`. В production укажите точный адрес фронтенда. Значение
`*` допустимо только для локальной отладки.

Не открывайте страницу через `file://`: браузер отправит `Origin: null`, и
такой запрос не пройдёт CORS. Запустите локальный HTTP-сервер либо разместите
статику в `backend/src/main/resources/static` — тогда фронтенд и API будут на
одном origin, и CORS не понадобится.

При пароле с символами вне ASCII кодируйте строку в UTF-8 перед `btoa`,
например через `TextEncoder`.

## Идентификаторы

API работает с внешними идентификаторами:

- `routeId` — значение `routes.external_id`, например `"321"`;
- `fromStopId` / `toStopId` — значения `stops.external_id`, например `"12345"`.

Внутренние первичные ключи базы данных наружу не передаются. Исключение —
`id` сохранённой поездки, который используется в пути запроса.

## Сводная таблица методов

| Метод | Endpoint | Назначение | Успешный ответ |
|---|---|---|---|
| `GET` | `/api/trips` | Список поездок текущего пользователя | `200 OK` |
| `POST` | `/api/trips` | Создание сохранённой поездки | `201 Created` |
| `GET` | `/api/trips/{id}/status` | Текущий статус и прогноз прибытия | `200 OK` |
| `GET` | `/api/trips/{id}/reliability` | Оценка надёжности маршрута | `200 OK` |
| `DELETE` | `/api/trips/{id}` | Удаление поездки | `204 No Content` |

Поездка доступна только владельцу. Обращение к чужой или несуществующей
поездке возвращает `404 Not Found`, а не `403 Forbidden` — так API не
раскрывает существование чужих записей.

## Получение списка поездок

```http
GET /api/trips
Authorization: Basic ZGVtb0BzcGIubG9jYWw6ZGVtbw==
```

```json
{
  "trips": [
    {
      "id": 1,
      "name": "Дом — Университет",
      "routeId": "321",
      "fromStopId": "12345",
      "toStopId": "67890",
      "requiredArrivalTime": "08:40"
    }
  ]
}
```

Поездки возвращаются в порядке убывания даты создания.

## Создание поездки

```http
POST /api/trips
Authorization: Basic ZGVtb0BzcGIubG9jYWw6ZGVtbw==
Content-Type: application/json
```

```json
{
  "name": "Дом — Университет",
  "routeId": "321",
  "fromStopId": "12345",
  "toStopId": "67890",
  "requiredArrivalTime": "08:40"
}
```

Ответ `201 Created` повторяет сохранённую поездку и содержит её `id`:

```json
{
  "id": 1,
  "name": "Дом — Университет",
  "routeId": "321",
  "fromStopId": "12345",
  "toStopId": "67890",
  "requiredArrivalTime": "08:40"
}
```

Правила валидации:

- `name` — обязательное, непустое, не длиннее 255 символов;
- `routeId`, `fromStopId`, `toStopId` — обязательные строки до 100 символов,
  маршрут и остановки должны существовать в базе;
- `fromStopId` и `toStopId` должны различаться;
- `requiredArrivalTime` — обязательное время в формате `HH:mm` (например,
  `08:40`).

При нарушении возвращается `400 Bad Request`.

## Статус поездки

```http
GET /api/trips/1/status
Authorization: Basic ZGVtb0BzcGIubG9jYWw6ZGVtbw==
```

```json
{
  "tripId": 1,
  "route": "321",
  "nextStop": "Университет",
  "etaMinutes": 7,
  "arrivalTime": "08:34",
  "requiredArrivalTime": "08:40",
  "delayMinutes": 3,
  "willArriveOnTime": true,
  "dataStatus": "current",
  "dataUpdatedAt": "2026-10-12T08:20:00"
}
```

### Выбор исходных данных

Сервис использует последнее доступное транспортное обновление:

1. сначала ищется последнее обновление по маршруту и **остановке назначения**;
2. если его нет — последнее обновление по маршруту в целом;
3. если обновлений нет — возвращается `dataStatus = "unavailable"`.

Актуальность определяется по времени `observedAt`:

| `dataStatus` | Условие |
|---|---|
| `current` | обновление не старше `STATUS_CURRENT_WINDOW` (по умолчанию 5 минут) |
| `stale` | обновление старше 5 минут, но не старше `STATUS_STALE_WINDOW` (по умолчанию 60 минут) |
| `unavailable` | обновлений нет или последнее старше 60 минут |

При `unavailable` прогноз не строится: `etaMinutes`, `arrivalTime`,
`delayMinutes` и `willArriveOnTime` возвращаются как `null`. Сервис не
подставляет вымышленные значения.

### Расчёт прогноза

- `arrivalTime` рассчитывается по `actual_at`, если он известен; иначе по
  `scheduled_at + delay_seconds`;
- `etaMinutes` — целое число минут от текущего момента до прогнозируемого
  прибытия (не меньше нуля);
- `delayMinutes` — задержка из `delay_seconds`, либо разница между
  `actual_at` и `scheduled_at`;
- `willArriveOnTime = true`, если прогнозируемое прибытие не позже требуемого
  времени; иначе `false`;
- для отменённой поездки прогноз прибытия не строится, а
  `willArriveOnTime = false`;
- если прогноз построить нельзя, `willArriveOnTime = null`;
- `dataUpdatedAt` — время последнего использованного обновления в часовом
  поясе `Europe/Moscow` (без смещения), либо `null`.

`nextStop` — следующая остановка маршрута после остановки из обновления. Если
её определить нельзя, возвращается остановка назначения поездки.

## Оценка надёжности

```http
GET /api/trips/1/reliability?periodDays=30
Authorization: Basic ZGVtb0BzcGIubG9jYWw6ZGVtbw==
```

Параметр `periodDays` необязателен: по умолчанию `30`, допустимый диапазон —
от `1` до `365` дней. Некорректное значение возвращает `400 Bad Request`.

```json
{
  "tripId": 1,
  "periodDays": 30,
  "averageDelayMinutes": 4.20,
  "delayedTripsPercent": 12.50,
  "cancelledTripsPercent": 1.20,
  "tripsCount": 850,
  "riskLevel": "medium",
  "dataSufficient": true
}
```

Ответ расширяет пример из проектной документации полями
`cancelledTripsPercent`, `tripsCount` и `dataSufficient`, чтобы клиент мог
отличить обоснованную оценку от отсутствия данных.

### Алгоритм

Расчёт выполняется по суточным агрегатам `historical_statistics` для пары
«маршрут + остановка назначения» за последние `periodDays` дней. Учитывается
именно остановка назначения, а не все остановки маршрута, чтобы не
дублировать одни и те же рейсы.

- `averageDelayMinutes` — средняя задержка, взвешенная по числу поездок
  (`trips_count`) каждого дня;
- `delayedTripsPercent` — доля поездок с опозданием от общего числа поездок;
- `cancelledTripsPercent` — доля отменённых поездок;
- проценты возвращаются в диапазоне `0..100` с точностью до двух знаков;
- при отсутствии поездок проценты и средняя задержка равны `null`.

### Уровни риска

Оценка считается достаточной, если набрано не менее **10 поездок** и известна
средняя задержка. Иначе возвращается `riskLevel = "unknown"` и
`dataSufficient = false`.

| Уровень | Правило |
|---|---|
| `high` | средняя задержка ≥ 8 мин, или доля опозданий ≥ 25 %, или доля отмен ≥ 5 % |
| `medium` | средняя задержка ≥ 4 мин, или доля опозданий ≥ 12 % |
| `low` | остальные случаи при достаточных данных |
| `unknown` | данных недостаточно |

Пороговые значения заданы в `ReliabilityCalculator` и могут быть уточнены
после анализа реальной статистики.

## Удаление поездки

```http
DELETE /api/trips/1
Authorization: Basic ZGVtb0BzcGIubG9jYWw6ZGVtbw==
```

Успешный ответ — `204 No Content` без тела. Чужая или несуществующая поездка
даёт `404 Not Found`.

## Формат ошибок

Ошибки уровня приложения возвращаются в едином JSON-формате:

```json
{
  "timestamp": "2026-11-07T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Некорректные данные запроса",
  "path": "/api/trips",
  "fieldErrors": {
    "requiredArrivalTime": "Требуемое время прибытия обязательно"
  }
}
```

Поле `fieldErrors` присутствует только при ошибках валидации полей.

| Код | Когда возвращается |
|---|---|
| `400 Bad Request` | некорректный JSON, ошибка валидации, неизвестный маршрут или остановка |
| `401 Unauthorized` | нет заголовка `Authorization` или неверные учётные данные |
| `404 Not Found` | поездка не существует или принадлежит другому пользователю |
| `500 Internal Server Error` | непредвиденная ошибка сервера |

Ответ `401 Unauthorized` формируется Spring Security и дополнительно содержит
заголовок `WWW-Authenticate: Basic`.

## Конфигурация

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `APP_DEMO_USER_ENABLED` | `true` | Создавать демонстрационного пользователя при старте |
| `APP_DEMO_USER_EMAIL` | `demo@spb.local` | E-mail демо-пользователя |
| `APP_DEMO_USER_PASSWORD` | `demo` | Пароль демо-пользователя |
| `STATUS_CURRENT_WINDOW` | `PT5M` | Максимальный возраст данных для `current` |
| `STATUS_STALE_WINDOW` | `PT60M` | Максимальный возраст данных для `stale` |
| `APP_CORS_ALLOWED_ORIGINS` | локальные адреса | Origin фронтенда через запятую |

Значения окон задаются в формате ISO-8601 Duration (`PT5M` — 5 минут,
`PT1H` — 1 час). Подробнее о схеме данных — в
[database.md](database.md), о запуске — в корневом `README.md`.
