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