# PetGo v2

Каркас REST API для КР №3: Spring Boot 3.5.16, Java 21, PostgreSQL 17,
Flyway и Swagger UI. Названия пакетов соответствуют обязательной структуре
из методички. Функции предметной области пока не реализованы.

## Запуск

Для локальной сборки нужны JDK 21 или новее, Maven 3.9 и Docker Compose.
Создайте `.env` на основе `.env.example` и задайте собственный пароль БД.

```powershell
Copy-Item .env.example .env
docker compose --env-file .env up --build --wait
```

После реализации API: `http://localhost:8080/api`. Swagger UI должен
открываться по адресу `http://localhost:8080/swagger-ui.html`, OpenAPI JSON —
по `/v3/api-docs`, health — по `/actuator/health`. Пока собственная
конфигурация безопасности не написана, Spring Security защищает Swagger и
остальные HTTP-маршруты стандартными правилами. Доступ к Swagger для
администратора настроим вместе с JWT и ролями.

Для сборки вне контейнера: `mvn clean package`. Чтобы запустить приложение
локально, сначала поднимите PostgreSQL (`docker compose --env-file .env up -d
postgres`), затем задайте `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` и выполните
`mvn spring-boot:run`. Compose передаёт эти параметры автоматически.

## Структура backend

Все Java-пакеты находятся в `src/main/java/ru/mirea/petgo/`.

| Пакет | Что размещать |
| --- | --- |
| `controller/` | REST-маршруты для пользователей, питомцев, заявок и других функций |
| `service/` | Бизнес-правила и транзакции |
| `repository/` | Spring Data JPA и запросы к PostgreSQL |
| `domain/` | JPA-сущности и enum |
| `dto/` | Модели запросов и ответов API |
| `security/` | JWT, роли, текущий пользователь, пароли |
| `filter/` | Безопасная фильтрация и поиск |
| `error/` | Единая обработка ошибок |
| `config/` | Настройки Spring Security, OpenAPI и приложения |

В каждой папке могут находиться классы разных функций. Например,
`UserController` — в `controller/`, `UserService` — в `service/`,
`UserRepository` — в `repository/`. Согласуйте общие DTO и сущности перед
параллельной разработкой. Пока единственный Java-класс — `PetGoApplication`.

Миграции БД размещаются в `src/main/resources/db/migration/` в формате
`V1__description.sql`. Схема из КР №1 служит основой, но требует адаптации:
основной сущности нужен UUID и дополнительные поля из требований КР №3.
Старый `schema.sql` содержит `DROP TABLE`, поэтому его нельзя просто
скопировать в Flyway. Hibernate настроен на проверку схемы
(`ddl-auto: validate`); таблицы создают миграции. Тесты размещаются в
`src/test/java/ru/mirea/petgo/`.

## Что ещё предстоит реализовать

Миграции, REST API, JWT и роли, аудит, CSV, аналитика и тесты — следующие
этапы. Frontend в текущий план работ не включён.
