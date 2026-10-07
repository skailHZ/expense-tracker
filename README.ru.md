*[Read this in English](README.md)*

---

# Corporate Task & Expense Tracker (REST API)

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED.svg)

Энтерпрайз B2B REST API сервис для управления корпоративными проектами, учета задач сотрудников и командировочных расходов. Спроектирован с упором на чистую архитектуру, безопасность и готовность к развертыванию в облаке (Cloud-Ready).

## 🚀 Стек технологий

* **Core:** Java 21, Spring Boot 4.1.0
* **Data Access:** Spring Data JPA, Hibernate, PostgreSQL
* **Миграции БД:** Liquibase
* **Безопасность:** Spring Security, Stateless JWT (JSON Web Tokens)
* **Маппинг:** MapStruct, Lombok
* **Валидация:** Hibernate Validator
* **Тестирование:** JUnit 5, Mockito
* **Документация:** OpenAPI 3.0 (Swagger UI)
* **DevOps:** Docker, Docker Compose (Multi-stage сборка)

## 🏗️ Архитектура и внедренные Best Practices

* **Слоистая архитектура (Layered Architecture):** Строгое разделение ответственности (Controller -> Service -> Repository).
* **Паттерн DTO:** Сущности БД (Entities) изолированы от клиента. Использован MapStruct для быстрой кодогенерации мапперов на этапе компиляции.
* **Решение проблемы N+1:** Использование `@EntityGraph` в JPA репозиториях для оптимизированной жадной загрузки связей.
* **Безопасность:** Кастомный `OncePerRequestFilter` для валидации JWT. Проверка ролей (`@PreAuthorize`) дополнена проверкой доступа к объекту: проект доступен только своему админу и добавленным им участникам (`ProjectAccessService`), что предотвращает уязвимости BOLA (Broken Object Level Authorization).
* **Глобальный перехват ошибок:** `@RestControllerAdvice` сопоставляет исключения с корректными HTTP-статусами (400, 401, 403, 404, 405, 409) и возвращает единый формат ошибки.
* **Пагинация и сортировка:** Нативная реализация через `Pageable` от Spring Data для работы с большими массивами данных.
* **Промышленное логирование:** Настроен `Logback` с ежедневной ротацией файлов и строгими паттернами вывода.

## 🛠️ Запуск проекта

Приложение полностью контейнеризовано. Для запуска не требуется локальная установка Java или PostgreSQL.

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/skailHZ/expense-tracker.git
   cd expense-tracker
   ```

2. Запустите кластер через Docker Compose:
   ```bash
   docker-compose up -d --build
   ```

3. API будет доступно по адресу `http://localhost:8080`.

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

## 🧪 Тестирование
Unit-тесты написаны с использованием JUnit 5 и Mockito. Бизнес-логика тестируется в полной изоляции без поднятия тяжелого контекста Spring.

Локальный запуск тестов:
```bash
mvn clean test
```
