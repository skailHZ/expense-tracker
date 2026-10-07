*[Read this in English](README.md)*

---

# Corporate Task & Expense Tracker (REST API)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED.svg)
[![CI](https://github.com/skailHZ/expense-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/skailHZ/expense-tracker/actions/workflows/ci.yml)

Энтерпрайз B2B REST API сервис для управления корпоративными проектами, учета задач сотрудников и командировочных расходов. Спроектирован с упором на чистую архитектуру, безопасность и готовность к развертыванию в облаке (Cloud-Ready).

## 🚀 Стек технологий

* **Core:** Java 21, Spring Boot 4.1.0
* **Data Access:** Spring Data JPA, Hibernate, PostgreSQL
* **Миграции БД:** Liquibase
* **Безопасность:** Spring Security, Stateless JWT (JSON Web Tokens)
* **Маппинг:** MapStruct, Lombok
* **Валидация:** Hibernate Validator
* **Тестирование:** JUnit 5, Mockito, MockMvc, Testcontainers (настоящий PostgreSQL), GitHub Actions CI
* **Документация:** OpenAPI 3.0 (Swagger UI)
* **DevOps:** Docker (multi-stage, запуск не от root), Docker Compose с healthcheck, Spring Boot Actuator

## 🏗️ Архитектура и внедренные Best Practices

* **Слоистая архитектура (Layered Architecture):** Строгое разделение ответственности (Controller -> Service -> Repository).
* **Паттерн DTO:** Сущности БД (Entities) изолированы от клиента. Использован MapStruct для быстрой кодогенерации мапперов на этапе компиляции.
* **Без N+1 запросов:** списки маппятся в DTO, которым нужны только id ленивых связей (Hibernate отдает их из прокси без дополнительных запросов). `QueryCountIntegrationTest` падает, если число SQL-запросов начинает зависеть от размера страницы.
* **Безопасность:** Кастомный `OncePerRequestFilter` для валидации JWT. Проверка ролей (`@PreAuthorize`) дополнена проверкой доступа к объекту: проект доступен только своему админу и добавленным им участникам (`ProjectAccessService`), что предотвращает уязвимости BOLA (Broken Object Level Authorization).
* **Глобальный перехват ошибок:** `@RestControllerAdvice` сопоставляет исключения с корректными HTTP-статусами (400, 401, 403, 404, 405, 409) и возвращает единый формат ошибки.
* **Пагинация и сортировка:** Нативная реализация через `Pageable` от Spring Data для работы с большими массивами данных.
* **Промышленное логирование:** Настроен `Logback` с ежедневной ротацией файлов и строгими паттернами вывода.

## 💳 Жизненный цикл расходов, идемпотентность и аналитика

Расходы ведут себя как платёжные документы, а не как простые строки таблицы:

* **Статусы (конечный автомат).** `PENDING → APPROVED → PAID`, а также `REJECTED` и отмена автором `CANCELLED`. `PAID`, `REJECTED` и `CANCELLED` конечные. Согласовать, отклонить и оплатить может только админ проекта; отменить может только автор и только пока расход ожидает решения. Недопустимый переход возвращает `409`.
* **Аудит.** Каждое изменение статуса (кто, когда, из какого в какой, комментарий) сохраняется и доступно в `GET /projects/{id}/expenses/{expenseId}/history`.
* **Идемпотентное создание.** `POST /projects/{id}/expenses` принимает заголовок `Idempotency-Key`. Повтор с тем же ключом и телом возвращает исходный расход (`Idempotent-Replayed: true`), а не создаёт дубль, в том числе при одновременных запросах. Тот же ключ с другим телом даёт `422`. Ключи действуют в пределах пользователя.
* **Оптимистичная блокировка.** Одновременные изменения статуса одного расхода разрешаются колонкой `@Version`: проигравший получает `409`, ничего не применяется частично.
* **Валюты.** У каждого расхода код ISO 4217 (`RUB`, `USD`, `EUR`, ...). Суммы в разных валютах никогда не складываются: итоги группируются по валютам, отклонённые и отменённые расходы не учитываются.
* **Аналитика (агрегирует PostgreSQL).** `GET .../expenses/total` (по валютам), `.../analytics/by-status` и `.../analytics/by-employee?status=...` (только админ проекта).

```bash
curl -X POST http://localhost:8080/api/v1/projects/1/expenses \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -H "Idempotency-Key: 6f1c1d8e-3d52-4d0e-9b1a-0a1b2c3d4e5f" \
  -d '{"amount": 150.50, "currency": "RUB", "description": "Такси до клиента"}'
```

## 🛠️ Запуск проекта

Приложение полностью контейнеризовано. Для запуска не требуется локальная установка Java или PostgreSQL.

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/skailHZ/expense-tracker.git
   cd expense-tracker
   ```

2. Создайте файл `.env` со своими секретами (он в `.gitignore`, в репозитории секретов нет):
   ```bash
   cp .env.example .env
   # задайте DB_PASSWORD, ADMIN_PASSWORD и сгенерируйте JWT_SECRET:
   openssl rand -base64 48
   ```

3. Запустите кластер через Docker Compose:
   ```bash
   docker-compose up -d --build
   ```

4. API будет доступно по адресу `http://localhost:8080`.

## 📚 Документация API (Swagger)

После успешного запуска интерактивная документация генерируется автоматически и доступна по ссылке:

🔗 **[Swagger UI: http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Инструкция по тестированию через Swagger:
1. Перейдите в секцию `auth-controller`.
2. Если вы новый пользователь, используйте `POST /api/v1/auth/register` для создания аккаунта. Саморегистрация всегда создает `ROLE_EMPLOYEE`; первый админ создается при старте из переменных окружения `ADMIN_USERNAME` / `ADMIN_PASSWORD`.
3. Если у вас уже есть аккаунт, используйте `POST /api/v1/auth/login` для авторизации.
4. Скопируйте JWT токен из тела ответа (`"token": "eyJhb..."`).
5. Нажмите зеленую кнопку **"Authorize"** в верхней части страницы Swagger и вставьте токен.
6. Теперь у вас есть доступ к защищенным эндпоинтам (Примечание: создание проектов и добавление участников требует `ROLE_ADMIN`; сотрудники видят только проекты, в которые их добавили).

## 🔌 Обзор API

Все пути начинаются с `/api/v1`. «Участники» - это админ проекта и сотрудники, которых он добавил.

| Метод и путь | Кто может вызвать |
|---|---|
| `POST /auth/register` | любой (всегда создает `ROLE_EMPLOYEE`) |
| `POST /auth/login` | любой |
| `POST /projects` | любой `ROLE_ADMIN` (становится админом проекта) |
| `GET /projects/{id}` | участники проекта |
| `POST /projects/{id}/members` | админ проекта |
| `GET`, `POST /projects/{id}/tasks` | участники проекта |
| `GET /projects/{id}/expenses?status=` | участники проекта |
| `POST /projects/{id}/expenses` (`Idempotency-Key`) | участники проекта |
| `POST /projects/{id}/expenses/{expenseId}/status` | админ проекта: согласовать, отклонить, оплатить; автор: отменить |
| `GET /projects/{id}/expenses/{expenseId}/history` | участники проекта |
| `GET /projects/{id}/expenses/total` | участники проекта |
| `GET /projects/{id}/expenses/analytics/by-status` | участники проекта |
| `GET /projects/{id}/expenses/analytics/by-employee` | админ проекта |

`GET /actuator/health` (вне `/api/v1`) публичный и используется healthcheck-ом Docker.

## ⚙️ Конфигурация

| Переменная | Обязательна | Назначение |
|---|---|---|
| `DB_USER`, `DB_PASSWORD` | да | логин и пароль PostgreSQL |
| `JWT_SECRET` | да | ключ подписи в Base64, минимум 256 бит (`openssl rand -base64 48`); со слабым ключом приложение не стартует |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | нет | первый админ, создается при старте, если его еще нет |
| `JWT_EXPIRATION` | нет | срок жизни токена в мс (по умолчанию 24 ч) |
| `JPA_SHOW_SQL` | нет | `true` выводит SQL в лог (по умолчанию `false`) |

## 🧪 Тестирование
В проекте два уровня тестов:

* **Unit-тесты** (JUnit 5 + Mockito) проверяют бизнес-логику в изоляции, без Spring-контекста.
* **Интеграционные тесты** (`@SpringBootTest` + MockMvc + Testcontainers) прогоняют настоящую цепочку Spring Security на реальном PostgreSQL в Docker: аутентификацию (401), доступ по ролям и к объектам (403), валидацию (400), конфликты (409), миграции Liquibase и агрегации.

Для интеграционных тестов должен быть запущен Docker. Секреты не нужны: тесты используют профиль `test`.

Локальный запуск всех тестов:
```bash
./mvnw clean verify
```

Каждый push и pull request дополнительно проверяет GitHub Actions (`.github/workflows/ci.yml`).
