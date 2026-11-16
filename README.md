# PetGo v2

Каркас REST API на Spring Boot 3.5.16, Java 17, PostgreSQL и Flyway.

## Запуск

Нужны Java 17+, Maven и Docker Compose.

```powershell
docker compose up -d postgres
mvn clean package
mvn spring-boot:run
```

Для запуска API и базы в контейнерах: `docker compose up --build`.
PostgreSQL доступен на `localhost:5434`, API — на `localhost:8080`.
Порты задаются через `POSTGRES_PORT` и `APP_PORT`, пароль базы — через
`POSTGRES_PASSWORD`. Значения по умолчанию предназначены для локальной разработки.
Пока не реализована собственная конфигурация безопасности, HTTP-маршруты
защищает стандартная конфигурация Spring Security.

## Структура исходников

Все пакеты находятся под `src/main/java/ru/mirea/petgo/`. Сейчас в них нет
реализации; `PetGoApplication.java` — единственный Java-класс.

| Папка | Назначение |
| --- | --- |
| `common/` | Общие DTO, ошибки и конфигурация |
| `auth/` | Вход, JWT, refresh, logout, пароль (разработчик 2) |
| `user/` | Учётные записи и уровни доступа (разработчики 1 и 2) |
| `pet/` | Питомцы (разработчик 2) |
| `request/` | Заявки, статусы, поиск, архив (разработчик 3) |
| `note/` | Заметки (разработчик 3) |
| `history/` | История прогулок и отзывы (разработчик 3) |
| `tag/` | Теги (разработчик 4) |
| `bulk/` | Массовые операции (разработчик 4) |
| `catalog/` | Справочники (разработчик 1) |
| `audit/` | Журнал аудита (разработчик 1) |
| `csv/` | Импорт и экспорт CSV (разработчик 4) |
| `analytics/` | SQL-агрегаты (разработчик 4) |

Миграции схемы размещаются в `src/main/resources/db/migration/` в формате
`V1__description.sql`. Hibernate проверяет схему (`ddl-auto: validate`),
таблицы создаёт Flyway. Тесты размещаются в соответствующих пакетах
`src/test/java/ru/mirea/petgo/`.
