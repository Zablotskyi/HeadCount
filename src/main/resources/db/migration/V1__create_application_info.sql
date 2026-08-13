CREATE TABLE application_info
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_application_info PRIMARY KEY (id)
);

INSERT INTO application_info (name, description)
VALUES ('HeadCount', 'HeadCount database initialized successfully');