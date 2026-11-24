CREATE TABLE auth_session (
    session_id      VARCHAR(36) NOT NULL,
    user_id         BIGINT      NOT NULL,
    client_ip       VARCHAR(45) NULL,
    user_agent      VARCHAR(255) NULL,
    create_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_time  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at      TIMESTAMP(6) NOT NULL,
    revoked_at      TIMESTAMP(6) NULL,
    PRIMARY KEY (session_id),
    CONSTRAINT fk_auth_session_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_session_user_active
    ON auth_session (user_id, revoked_at, expires_at);

CREATE TABLE auth_security_event (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    event_type      VARCHAR(40)  NOT NULL,
    description     VARCHAR(255) NOT NULL,
    client_ip       VARCHAR(45)  NULL,
    user_agent      VARCHAR(255) NULL,
    create_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_auth_security_event_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_security_event_user_time
    ON auth_security_event (user_id, create_time);

CREATE TABLE user_notification_preferences (
    user_id         BIGINT       NOT NULL,
    trip_reminder   BOOLEAN      NOT NULL DEFAULT TRUE,
    trip_change     BOOLEAN      NOT NULL DEFAULT TRUE,
    system_message  BOOLEAN      NOT NULL DEFAULT TRUE,
    email_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    update_time     TIMESTAMP(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_preferences_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
