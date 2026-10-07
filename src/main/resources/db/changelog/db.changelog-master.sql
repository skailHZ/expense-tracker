-- liquibase formatted sql

-- changeset admin:1
CREATE TABLE users (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- changeset admin:2
CREATE TABLE projects (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          admin_id BIGINT NOT NULL REFERENCES users(id),
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_projects_admin_id ON projects(admin_id);

-- changeset admin:3
CREATE TABLE tasks (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       status VARCHAR(50) NOT NULL,
                       project_id BIGINT NOT NULL REFERENCES projects(id),
                       employee_id BIGINT NOT NULL REFERENCES users(id),
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_employee_id ON tasks(employee_id);

-- changeset admin:4
CREATE TABLE expenses (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          amount DECIMAL(10, 2) NOT NULL,
                          description TEXT,
                          project_id BIGINT NOT NULL REFERENCES projects(id),
                          employee_id BIGINT NOT NULL REFERENCES users(id),
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_expenses_project_id ON expenses(project_id);
CREATE INDEX idx_expenses_employee_id ON expenses(employee_id);

-- changeset admin:5
INSERT INTO users (username, password, role) VALUES ('ceo_boss', 'secret123', 'ROLE_ADMIN');

-- changeset admin:6
CREATE TABLE project_members (
                                 project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                 user_id BIGINT NOT NULL REFERENCES users(id),
                                 PRIMARY KEY (project_id, user_id)
);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);


-- changeset admin:7
-- Удаляем тестового админа из changeset 5: его пароль хранился в открытом виде.
-- Сам changeset 5 не редактируем: Liquibase хранит его checksum, и правка сломала бы уже развернутые базы.
DELETE FROM users u
WHERE u.username = 'ceo_boss'
  AND NOT EXISTS (SELECT 1 FROM projects WHERE admin_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM tasks WHERE employee_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM expenses WHERE employee_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM project_members WHERE user_id = u.id);


-- changeset admin:8
-- Валюта, статус жизненного цикла и версия для оптимистичной блокировки.
-- Существующие расходы считаем рублевыми и ожидающими проверки, после чего убираем DEFAULT:
-- приложение обязано задавать значения явно.
ALTER TABLE expenses ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'RUB';
ALTER TABLE expenses ALTER COLUMN currency DROP DEFAULT;
ALTER TABLE expenses ADD CONSTRAINT chk_expenses_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE expenses ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE expenses ALTER COLUMN status DROP DEFAULT;
ALTER TABLE expenses ADD CONSTRAINT chk_expenses_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'PAID', 'CANCELLED'));
ALTER TABLE expenses ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE expenses ADD CONSTRAINT chk_expenses_amount_positive CHECK (amount > 0);
CREATE INDEX idx_expenses_project_status ON expenses(project_id, status);

-- changeset admin:9
-- Аудит: каждое изменение статуса фиксируется кто, когда и из какого состояния в какое.
CREATE TABLE expense_status_history (
                                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        expense_id BIGINT NOT NULL REFERENCES expenses(id),
                                        from_status VARCHAR(20),
                                        to_status VARCHAR(20) NOT NULL,
                                        changed_by BIGINT NOT NULL REFERENCES users(id),
                                        comment VARCHAR(500),
                                        changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_expense_status_history_expense_id ON expense_status_history(expense_id);

-- changeset admin:10
-- Идемпотентность: уникальная пара (пользователь, ключ) гарантирует, что повторный запрос
-- с тем же Idempotency-Key не создаст второй расход даже при одновременной отправке.
CREATE TABLE idempotency_keys (
                                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  user_id BIGINT NOT NULL REFERENCES users(id),
                                  idempotency_key VARCHAR(255) NOT NULL,
                                  request_hash VARCHAR(64) NOT NULL,
                                  expense_id BIGINT REFERENCES expenses(id),
                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                  CONSTRAINT uq_idempotency_user_key UNIQUE (user_id, idempotency_key)
);
