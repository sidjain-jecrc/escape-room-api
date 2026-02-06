-- Runs when MySQL container is first created (empty volume).
-- Creates the database and slots table so the API can start even if Hibernate DDL is delayed or fails.

CREATE DATABASE IF NOT EXISTS escape_room;
USE escape_room;

CREATE TABLE IF NOT EXISTS slots (
    slot_id          BIGINT       NOT NULL PRIMARY KEY,
    status           VARCHAR(50)  NOT NULL,
    hold_id          BINARY(16)   NULL,
    hold_expires_at  TIMESTAMP(6) NULL
);
