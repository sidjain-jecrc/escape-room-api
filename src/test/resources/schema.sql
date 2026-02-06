-- Seed schema for tests (H2). Unquoted names become uppercase in H2 (SLOTS, SLOT_ID, ...).

CREATE TABLE IF NOT EXISTS SLOTS (
    SLOT_ID          BIGINT       NOT NULL PRIMARY KEY,
    STATUS           VARCHAR(50)  NOT NULL,
    HOLD_ID          VARBINARY(16) NULL,
    HOLD_EXPIRES_AT  TIMESTAMP    NULL
);
