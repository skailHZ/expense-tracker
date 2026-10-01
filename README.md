*[Read this in Russian (Русский)](README.ru.md)*

---

# Corporate Task & Expense Tracker (REST API)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED.svg)

An Enterprise-level B2B REST API service designed for managing corporate projects, tracking employee tasks, and logging business expenses. Built with a focus on clean architecture, security, and cloud-readiness.

## 🚀 Tech Stack

* **Core:** Java 21, Spring Boot 4.1.0
* **Data Access:** Spring Data JPA, Hibernate, PostgreSQL
* **Database Migration:** Liquibase
* **Security:** Spring Security, Stateless JWT (JSON Web Tokens)
* **Mapping:** MapStruct, Lombok
* **Validation:** Hibernate Validator
* **Testing:** JUnit 5, Mockito
* **Documentation:** OpenAPI 3.0 (Swagger UI)
* **DevOps:** Docker, Docker Compose (Multi-stage build)

## 🏗️ Architecture & Best Practices Implemented

* **Layered Architecture:** Strict separation of concerns (Controller -> Service -> Repository).
* **DTO Pattern:** Entities are never exposed to the client. MapStruct is used for fast, compile-time object mapping.
* **N+1 Problem Solved:** Configured `@EntityGraph` in JPA repositories to fetch lazy associations efficiently.
* **Robust Security:** Custom `OncePerRequestFilter` for JWT validation. The author identity is securely extracted from the `SecurityContext`, preventing broken object level authorization (BOLA).
* **Global Exception Handling:** Custom `@RestControllerAdvice` to intercept exceptions and return standardized API error responses (e.g., 404, 400).
* **Pagination & Sorting:** Built-in Spring Data `Pageable` implementation for large datasets.
* **Industrial Logging:** Configured `Logback` with daily rolling file appenders and strict log patterns.

## 🛠️ How to Run

The application is fully containerized. You do not need Java or PostgreSQL installed on your host machine.

1. Clone the repository:
   ```bash
   git clone https://github.com/skailHZ/expense-tracker.git
   cd expense-tracker
   ```

2. Start the cluster using Docker Compose:
   ```bash
   docker-compose up -d --build
   ```

3. The API will be available at `http://localhost:8080`.

## 📚 API Documentation (Swagger)

Once the application is running, the interactive API documentation is automatically generated and accessible via:

🔗 **[Swagger UI: http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Quick Start Guide for Testing via Swagger:
1. Navigate to the `auth-controller` section.
2. If you are a new user, use `POST /api/v1/auth/register` to create an account (e.g., set role to `ROLE_ADMIN`).
3. If you already have an account, use `POST /api/v1/auth/login` to authenticate.
4. Copy the JWT token from the response body (`"token": "eyJhb..."`).
5. Click the green **"Authorize"** button at the top of the Swagger page and paste the token.
6. You now have access to protected endpoints (Note: creating projects requires `ROLE_ADMIN`).

## 🧪 Testing
Unit tests are written using JUnit 5 and Mockito, isolating the business logic from the Spring Context.

Run the tests locally:
```bash
mvn clean test
```
