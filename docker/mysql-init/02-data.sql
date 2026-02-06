-- Seed demo slots (idempotent: only if table is empty).

USE escape_room;

INSERT IGNORE INTO slots (slot_id, status, hold_id, hold_expires_at)
VALUES (1, 'AVAILABLE', NULL, NULL);

INSERT IGNORE INTO slots (slot_id, status, hold_id, hold_expires_at)
VALUES (2, 'AVAILABLE', NULL, NULL);

INSERT IGNORE INTO slots (slot_id, status, hold_id, hold_expires_at)
VALUES (3, 'AVAILABLE', NULL, NULL);
