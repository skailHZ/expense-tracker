*[Read this in Russian (Русский)](README.ru.md)*

---

# Corporate Task & Expense Tracker (REST API)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED.svg)
[![CI](https://github.com/skailHZ/expense-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/skailHZ/expense-tracker/actions/workflows/ci.yml)

An Enterprise-level B2B REST API service designed for managing corporate projects, tracking employee tasks, and logging business expenses. Built with a focus on clean architecture, security, and cloud-readiness.

## 🚀 Tech Stack

* **Core:** Java 21, Spring Boot 4.1.0
* **Data Access:** Spring Data JPA, Hibernate, PostgreSQL
* **Database Migration:** Liquibase
* **Security:** Spring Security, Stateless JWT (JSON Web Tokens)
* **Mapping:** MapStruct, Lombok
* **Validation:** Hibernate Validator
* **Testing:** JUnit 5, Mockito, MockMvc, Testcontainers (real PostgreSQL), GitHub Actions CI
* **Documentation:** OpenAPI 3.0 (Swagger UI)
* **DevOps:** Docker (multi-stage, non-root), Docker Compose with healthchecks, Spring Boot Actuator

## 🏗️ Architecture & Best Practices Implemented

* **Layered Architecture:** Strict separation of concerns (Controller -> Service -> Repository).
* **DTO Pattern:** Entities are never exposed to the client. MapStruct is used for fast, compile-time object mapping.
* **No N+1 queries:** list endpoints map entities to DTOs that need only the IDs of lazy associations (Hibernate returns them from proxies without extra queries). `QueryCountIntegrationTest` fails if the number of SQL statements starts to depend on the page size.
* **Robust Security:** Custom `OncePerRequestFilter` for JWT validation. Role-based access (`@PreAuthorize`) is combined with object-level checks: a project is accessible only to its admin and to members the admin added (`ProjectAccessService`), which prevents broken object level authorization (BOLA).
* **Global Exception Handling:** Custom `@RestControllerAdvice` maps exceptions to proper HTTP statuses (400, 401, 403, 404, 405, 409) and returns a standardized error body.
* **Pagination & Sorting:** Built-in Spring Data `Pageable` implementation for large datasets.
* **Industrial Logging:** Configured `Logback` with daily rolling file appenders and strict log patterns.

## 💳 Expense Workflow, Idempotency & Analytics

Expenses behave like payment documents rather than plain rows:

* **Status workflow (state machine).** `PENDING → APPROVED → PAID`, with `REJECTED` and author-initiated `CANCELLED`. `PAID`, `REJECTED` and `CANCELLED` are terminal. Only the project admin approves, rejects and pays; only the author can cancel, and only while the expense is pending. An illegal transition returns `409`.
* **Audit trail.** Every status change (who, when, from → to, comment) is stored and available at `GET /projects/{id}/expenses/{expenseId}/history`.
* **Idempotent creation.** `POST /projects/{id}/expenses` accepts an `Idempotency-Key` header. A retry with the same key and body returns the original expense (`Idempotent-Replayed: true`) instead of creating a duplicate, even when requests arrive concurrently. The same key with a different body returns `422`. Keys are scoped per user.
* **Optimistic locking.** Concurrent status changes of one expense are resolved with a `@Version` column: the loser gets `409` and nothing is half-applied.
* **Currencies.** Each expense has an ISO 4217 code (`RUB`, `USD`, `EUR`, ...). Amounts in different currencies are never added together; totals are grouped by currency, and rejected/cancelled expenses are excluded.
* **Analytics (aggregated by PostgreSQL).** `GET .../expenses/total` (by currency), `.../analytics/by-status`, and `.../analytics/by-employee?status=...` (project admin only).

```bash
curl -X POST http://localhost:8080/api/v1/projects/1/expenses \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -H "Idempotency-Key: 6f1c1d8e-3d52-4d0e-9b1a-0a1b2c3d4e5f" \
  -d '{"amount": 150.50, "currency": "RUB", "description": "Taxi to the client"}'
```

## 🛠️ How to Run

The application is fully containerized. You do not need Java or PostgreSQL installed on your host machine.

1. Clone the repository:
   ```bash
   git clone https://github.com/skailHZ/expense-tracker.git
   cd expense-tracker
   ```

2. Create the `.env` file with your own secrets (it is git-ignored; nothing secret is stored in the repository):
   ```bash
   cp .env.example .env
   # set DB_PASSWORD, ADMIN_PASSWORD and generate JWT_SECRET:
   openssl rand -base64 48
   ```

3. Start the cluster using Docker Compose:
   ```bash
   docker-compose up -d --build
   ```

4. The API will be available at `http://localhost:8080`.

## 📚 API Documentation (Swagger)

Once the application is running, the interactive API documentation is automatically generated and accessible via:

🔗 **[Swagger UI: http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Quick Start Guide for Testing via Swagger:
1. Navigate to the `auth-controller` section.
2. If you are a new user, use `POST /api/v1/auth/register` to create an account. Self-registration always creates `ROLE_EMPLOYEE`; the first admin is created at startup from the `ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables.
3. If you already have an account, use `POST /api/v1/auth/login` to authenticate.
4. Copy the JWT token from the response body (`"token": "eyJhb..."`).
5. Click the green **"Authorize"** button at the top of the Swagger page and paste the token.
6. You now have access to protected endpoints (Note: creating projects and adding members requires `ROLE_ADMIN`; employees see only projects they were added to).

## 🔌 API Overview

All paths are prefixed with `/api/v1`. "Members" means the project admin plus employees added by the admin.

| Method & path | Who can call it |
|---|---|
| `POST /auth/register` | anyone (always creates `ROLE_EMPLOYEE`) |
| `POST /auth/login` | anyone |
| `POST /projects` | any `ROLE_ADMIN` (becomes the project admin) |
| `GET /projects/{id}` | project members |
| `POST /projects/{id}/members` | project admin |
| `GET`, `POST /projects/{id}/tasks` | project members |
| `GET /projects/{id}/expenses?status=` | project members |
| `POST /projects/{id}/expenses` (`Idempotency-Key`) | project members |
| `POST /projects/{id}/expenses/{expenseId}/status` | project admin: approve, reject, pay; author: cancel |
| `GET /projects/{id}/expenses/{expenseId}/history` | project members |
| `GET /projects/{id}/expenses/total` | project members |
| `GET /projects/{id}/expenses/analytics/by-status` | project members |
| `GET /projects/{id}/expenses/analytics/by-employee` | project admin |

`GET /actuator/health` (outside `/api/v1`) is public and used by the Docker healthcheck.

## ⚙️ Configuration

| Variable | Required | Purpose |
|---|---|---|
| `DB_USER`, `DB_PASSWORD` | yes | PostgreSQL credentials |
| `JWT_SECRET` | yes | Base64 signing key, at least 256 bits (`openssl rand -base64 48`); the app refuses to start with a weak key |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | no | first admin, created on startup if it does not exist |
| `JWT_EXPIRATION` | no | token lifetime in ms (default 24 h) |
| `JPA_SHOW_SQL` | no | `true` prints SQL to the log (default `false`) |

## 🧪 Testing
The project has two levels of tests:

* **Unit tests** (JUnit 5 + Mockito) test the business logic in isolation, without the Spring context.
* **Integration tests** (`@SpringBootTest` + MockMvc + Testcontainers) run the real Spring Security filter chain against a real PostgreSQL started in Docker: authentication (401), role and object-level access (403), validation (400), conflicts (409), Liquibase migrations and aggregations.

Docker must be running to execute the integration tests. No secrets are needed: tests use the `test` profile.

Run all tests locally:
```bash
./mvnw clean verify
```

Every push and pull request is also checked by GitHub Actions (`.github/workflows/ci.yml`).
