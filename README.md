# PetGo v2

REST API сервиса выгула питомцев для КР №3. Backend использует Java 21, Spring Boot, PostgreSQL 17, Flyway и Spring Security.

## Запуск через Docker Compose

Скопируйте шаблон окружения и сгенерируйте случайный ключ для JWT.

```powershell
Copy-Item .env.example .env
$key = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($key)
$rng.Dispose()
[Convert]::ToBase64String($key)
```

В `.env` укажите результат генерации в `JWT_SECRET_BASE64`, свой пароль в `POSTGRES_PASSWORD` и временный пароль администратора в `BOOTSTRAP_ADMIN_PASSWORD`. Пароли пользователей должны содержать не менее 12 символов и занимать не более 72 байт UTF-8. Начальная учётная запись создаётся при первом запуске, если указаны `BOOTSTRAP_ADMIN_EMAIL` и `BOOTSTRAP_ADMIN_PASSWORD` и такого email ещё нет.

```powershell
docker compose --env-file .env up --build --wait
```

По умолчанию API доступен на `http://localhost:8080/api`, проверка состояния на `/actuator/health`, PostgreSQL на локальном порту `5434`. Swagger UI находится на `/swagger-ui.html` и открывается в браузере после входа с постоянным паролем администратора. Запрос входа должен быть отправлен из того же браузера, чтобы установить cookie Swagger. Для вызова защищённых методов из Swagger укажите JWT через кнопку **Authorize**. OpenAPI JSON на `/v3/api-docs` также доступен администратору.

`docker compose down` останавливает сервисы и сохраняет данные в именованном томе. Для запуска приложения из Maven поднимите сервис `postgres` и задайте `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` и переменные JWT и администратора в окружении перед `mvn spring-boot:run`. Для клиента на другом origin задайте список адресов через запятую в `CORS_ALLOWED_ORIGINS`.

## Запуск React

Запустите API на порту 8080, затем из корня проекта выполните:

```powershell
npm --prefix frontend ci
npm --prefix frontend run dev
```

Интерфейс доступен на `http://localhost:3000`. Vite пересылает запросы `/api` на backend. Для проверки сборки выполните `npm --prefix frontend run build`.

## Авторизация

`OWNER` и `WALKER` определяют, может ли учётная запись выступать владельцем питомца или назначаться выгульщиком. Отдельное поле `accessLevel` определяет права доступа к API.

| Уровень | Права |
| --- | --- |
| `VIEWER` | Просмотр данных и экспорт |
| `EDITOR` | Просмотр, изменение заявок и тегов, массовые операции, предварительная проверка и применение CSV |
| `ADMIN` | Права редактора, управление питомцами и заявками, пользователями и справочником, просмотр аудита и Swagger. Окончательное удаление заявок доступно при включённой настройке `app.requests.permanent-delete-enabled` |

- `OWNER` управляет своими питомцами и создаёт заявки от своего имени. При создании заявки интерфейс запрашивает список его питомцев.
- `WALKER` может начать и завершить назначенную ему прогулку, в том числе с уровнем `VIEWER`.
- `ADMIN` может создавать заявку за владельца, назначать выгульщика и выполнять переходы статуса для любой назначенной прогулки.

- `POST /api/auth/login` принимает `email` и `password`, возвращает JWT и refresh-токен. Передавайте JWT в заголовке `Authorization: Bearer <accessToken>`.
- `GET /api/auth/me` возвращает данные текущей учётной записи.
- `POST /api/auth/change-password` принимает `currentPassword` и `newPassword`. При временном пароле доступны просмотр своей учётной записи и смена пароля. После смены выполните вход заново.
- `POST /api/auth/refresh` принимает `refreshToken` и заменяет его новым. `POST /api/auth/logout` с таким же телом отзывает сессию.

## Администрирование

| Маршрут | Назначение |
| --- | --- |
| `GET /api/admin/users`, `GET /api/admin/users/{id}` | Список с пагинацией и карточка пользователя |
| `POST /api/admin/users` | Создание. Поля `username`, `email`, `role`, `accessLevel`, `temporaryPassword` |
| `PUT /api/admin/users/{id}` | Редактирование. Поля `username`, `email`, `role`, `accessLevel`, `enabled`, `version` |
| `POST /api/admin/users/{id}/block`, `/unblock` | Блокировка и разблокировка. Блокировка отзывает действующие сессии |
| `POST /api/admin/users/{id}/reset-password` | Назначение `temporaryPassword` и отзыв действующих сессий |
| `GET /api/districts?enabled=true` | Список районов. Параметр `enabled` необязательный |
| `POST /api/districts`, `PUT /api/districts/{id}` | Создание и переименование района. Поле `name` |
| `POST /api/districts/{id}/enable`, `/disable` | Включение и отключение района |
| `GET /api/admin/audit` | Журнал аудита с фильтрами `q`, `entityType` и `actorId` |

Для постраничных списков используются `page` (начиная с 0), `size` (25 по умолчанию, до 100) и `sort` (например, `occurredAt,desc` для аудита). Аудит хранит действие, автора, тип и UUID объекта, время и краткое описание. `X-Correlation-ID` возвращается в ответе и связывает события одного запроса. Для поиска по журналу его можно передать в `q`.

## Питомцы и заявки

Питомцы доступны через `/api/pets`. Владелец создаёт питомца с полем `name`, администратор может указать `ownerId` нужного владельца. Для отбора питомцев до пагинации список принимает параметр `ownerId`. Через `/api/walk-requests` можно создать заявку с `petId`, `ownerId`, необязательными `walkerId` и `districtId`, а также `scheduledAt`, `durationMinutes`, `address` и `price`. Отключённый район сохраняется в существующих заявках. Для новых назначений выбирайте включённый район.

Списки и CSV-экспорт принимают одинаковые фильтры, например `search`, `archived`, `status`, `priority`, `districtId`, `tagId`, `priceFrom`, `priceTo`, `scheduledFrom` и `scheduledTo`. Приоритет рассчитывается по длительности. До 30 минут это `LOW`, от 30 до 59 минут `NORMAL`, с 60 минут `HIGH`.

## Сборка и тесты

```powershell
mvn clean verify
```

Для проверки запуска Spring и миграций укажите URL выделенной тестовой PostgreSQL в `PETGO_TEST_DB_URL`, при необходимости задайте `PETGO_TEST_DB_USER` и `PETGO_TEST_DB_PASSWORD`. Затем выполните `mvn -Dtest=PostgresSmokeTest test`.

Схемой управляют миграции в `src/main/resources/db/migration`. Hibernate проверяет схему при старте. Код распределён по пакетам `controller`, `service`, `repository`, `domain`, `dto`, `security`, `filter`, `error` и `config` внутри `ru.mirea.petgo`.
