CREATE TABLE organization_units
(
    id         BIGINT                                                       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(150)                                                 NOT NULL,
    code       VARCHAR(100)                                                 NOT NULL,
    type       ENUM ('ORGANIZATION', 'COUNTRY', 'REGION', 'OFFICE',
                    'DEPARTMENT', 'UNIT')                                   NOT NULL,
    parent_id  BIGINT                                                       NULL,
    manager_id BIGINT                                                       NULL,
    active     BOOLEAN                                                      NOT NULL DEFAULT TRUE,
    sort_order INT                                                          NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_organization_units PRIMARY KEY (id),
    CONSTRAINT fk_organization_units_parent
        FOREIGN KEY (parent_id) REFERENCES organization_units (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_organization_units_parent_id (parent_id),
    INDEX idx_organization_units_type (type),
    INDEX idx_organization_units_manager_id (manager_id)
);

CREATE TABLE users
(
    id                             BIGINT       NOT NULL AUTO_INCREMENT,
    username                       VARCHAR(100) NOT NULL,
    resource_number                VARCHAR(100) NOT NULL,
    grade                          VARCHAR(100) NULL,
    first_name                     VARCHAR(100) NOT NULL,
    last_name                      VARCHAR(100) NOT NULL,
    mobile_number                  VARCHAR(50)  NULL,
    email                          VARCHAR(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    password_hash                  VARCHAR(255) NOT NULL,
    country                        VARCHAR(100) NULL,
    city                           VARCHAR(100) NULL,
    office                         VARCHAR(150) NULL,
    organization_unit_id           BIGINT       NULL,
    position                       VARCHAR(150) NULL,
    line_manager_id                BIGINT       NULL,
    address                        VARCHAR(500) NULL,
    authorized_person_phone_number VARCHAR(50)  NULL,
    time_zone                      VARCHAR(100) NOT NULL,
    status                         ENUM ('PENDING_EMAIL_VERIFICATION', 'PENDING_APPROVAL', 'ACTIVE',
                                         'REJECTED', 'SUSPENDED', 'ARCHIVED') NOT NULL DEFAULT 'PENDING_EMAIL_VERIFICATION',
    enabled                        BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verified                 BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at                  TIMESTAMP(6) NULL,
    created_at                     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_resource_number UNIQUE (resource_number),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_organization_unit
        FOREIGN KEY (organization_unit_id) REFERENCES organization_units (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_users_line_manager
        FOREIGN KEY (line_manager_id) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_users_organization_unit_id (organization_unit_id),
    INDEX idx_users_line_manager_id (line_manager_id),
    INDEX idx_users_status (status),
    INDEX idx_users_name (last_name, first_name)
);

ALTER TABLE organization_units
    ADD CONSTRAINT fk_organization_units_manager
        FOREIGN KEY (manager_id) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE TABLE roles
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_roles
(
    user_id    BIGINT       NOT NULL,
    role_id    BIGINT       NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_user_roles_role_id (role_id)
);

INSERT INTO roles (name)
VALUES ('EMPLOYEE'),
       ('COUNTRY_MANAGER'),
       ('REGIONAL_MANAGER'),
       ('SUPPORT_MANAGER'),
       ('PROGRAM_MANAGER'),
       ('DEPARTMENT_MANAGER'),
       ('UNIT_MANAGER'),
       ('SECURITY_OFFICER'),
       ('SECURITY_MANAGER'),
       ('ADMIN');

CREATE TABLE headcount_events
(
    id                         BIGINT                                  NOT NULL AUTO_INCREMENT,
    title                      VARCHAR(200)                            NOT NULL,
    description                TEXT                                    NULL,
    status                     ENUM ('ACTIVE', 'CLOSED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    scope_organization_unit_id BIGINT                                  NOT NULL,
    started_at                 TIMESTAMP(6)                            NOT NULL,
    started_by                 BIGINT                                  NOT NULL,
    closed_at                  TIMESTAMP(6)                            NULL,
    closed_by                  BIGINT                                  NULL,
    cancelled_at               TIMESTAMP(6)                            NULL,
    cancelled_by               BIGINT                                  NULL,
    created_at                 TIMESTAMP(6)                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                 TIMESTAMP(6)                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_headcount_events PRIMARY KEY (id),
    CONSTRAINT fk_headcount_events_scope_organization_unit
        FOREIGN KEY (scope_organization_unit_id) REFERENCES organization_units (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_headcount_events_started_by
        FOREIGN KEY (started_by) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_headcount_events_closed_by
        FOREIGN KEY (closed_by) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_headcount_events_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_headcount_events_status (status),
    INDEX idx_headcount_events_scope_organization_unit_id (scope_organization_unit_id),
    INDEX idx_headcount_events_started_at (started_at),
    INDEX idx_headcount_events_started_by (started_by),
    INDEX idx_headcount_events_closed_by (closed_by),
    INDEX idx_headcount_events_cancelled_by (cancelled_by)
);

CREATE TABLE headcount_participants
(
    id                         BIGINT                                NOT NULL AUTO_INCREMENT,
    event_id                   BIGINT                                NOT NULL,
    employee_id                BIGINT                                NOT NULL,
    employee_name_snapshot     VARCHAR(201)                          NOT NULL,
    resource_number_snapshot   VARCHAR(100)                          NOT NULL,
    organization_path_snapshot VARCHAR(1000)                         NOT NULL,
    status                     ENUM ('PENDING', 'SAFE', 'NEED_HELP') NOT NULL DEFAULT 'PENDING',
    confirmed_at               TIMESTAMP(6)                          NULL,
    confirmed_by               BIGINT                                NULL,
    confirmation_source        VARCHAR(100)                          NULL,
    help_message               VARCHAR(1000)                         NULL,
    help_requested_at          TIMESTAMP(6)                          NULL,
    version                    BIGINT                                NOT NULL DEFAULT 0,
    created_at                 TIMESTAMP(6)                          NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                 TIMESTAMP(6)                          NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_headcount_participants PRIMARY KEY (id),
    CONSTRAINT uk_headcount_participants_event_employee UNIQUE (event_id, employee_id),
    CONSTRAINT fk_headcount_participants_event
        FOREIGN KEY (event_id) REFERENCES headcount_events (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_headcount_participants_employee
        FOREIGN KEY (employee_id) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_headcount_participants_confirmed_by
        FOREIGN KEY (confirmed_by) REFERENCES users (id)
            ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX idx_headcount_participants_employee_id (employee_id),
    INDEX idx_headcount_participants_status (status),
    INDEX idx_headcount_participants_confirmed_by (confirmed_by)
);
