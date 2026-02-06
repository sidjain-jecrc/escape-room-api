# Escape Room Booking API (Hold + Confirm)

Build an API for booking escape room time slots with a "hold" mechanism.

A small Spring Boot (Java 11) service that supports:
- **Hold a slot** for 5 minutes (auto-expiration)
- **Confirm** a hold into a booking
- **Release** a hold explicitly

The core of the design is a simple state machine with **atomic transitions** (conditional updates) to prevent double holds / double bookings under concurrency.

---

## Architecture & Key Decisions

### State machine

Each slot is one of:
- `AVAILABLE`
- `HELD` (has `holdId` + `holdExpiresAt`)
- `BOOKED`

Transitions:
- `AVAILABLE -> HELD`
- `HELD -> BOOKED`
- `HELD -> AVAILABLE` (expiration or release)

### Concurrency control (why it’s safe)

Concurrency is handled via **atomic SQL updates** (compare-and-set semantics):

- A hold only succeeds if the slot is currently `AVAILABLE`
- A confirm only succeeds if the slot is `HELD` **and** the hold has not expired

This avoids relying on in-process locks (which don’t work across multiple instances) and ports cleanly to production databases.

### Expiration strategy

This implementation uses **opportunistic cleanup**:
- On `holdSlot()` and `confirmHold()` the service runs a lightweight query to release expired holds.

In a real production system, I’d also add a background cleanup job (or a TTL cache layer) for better hygiene/visibility, but opportunistic cleanup keeps the solution small while remaining correct.

---

## API Endpoints

Base path: `/api`

### Hold a slot
`POST /api/slots/{slotId}/hold`

Response:
```json
{ "holdId": "uuid", "expiresAt": "2026-01-01T00:00:05Z" }
```

### Confirm a hold
`POST /api/holds/{holdId}/confirm`

- 200 on success
- 410 GONE if expired/invalid

### Release a hold
`DELETE /api/holds/{holdId}`

- 204/200 (no-op if already released)

### Read slot state (debug/demo)
`GET /api/slots/{slotId}`

---

## Gradle Wrapper

This project includes the Gradle wrapper (`gradlew`, `gradlew.bat`, and `gradle/wrapper/`). If `./gradlew` fails (e.g. missing or invalid `gradle-wrapper.jar`), generate the wrapper once with an installed Gradle:

```bash
gradle wrapper --gradle-version 8.5
```

Install Gradle if needed (e.g. `brew install gradle` or [gradle.org/install](https://gradle.org/install)), then run the command above from the project root.

---

## Run Locally with Docker (MySQL + API)

Prereqs:
- Docker + Docker Compose

Start everything:
```bash
docker compose up --build
```

API:
- http://localhost:8080

MySQL:
- localhost:3306 (user: `escape`, pass: `escape`, db: `escape_room`)

Try it:
```bash
curl -X POST http://localhost:8080/api/slots/1/hold
# copy holdId from response
curl -X POST http://localhost:8080/api/holds/<holdId>/confirm
```

Stop:
```bash
docker compose down
```

---

## Run Tests (fast, no Docker)

Tests run against an in-memory H2 DB in MySQL-compat mode.

```bash
./gradlew test
```

The tests focus on:
1. expiration behavior
2. concurrent hold attempts (only one should win)
3. confirm vs expire race (ensures system doesn’t end inconsistent)

---

## What’s intentionally not implemented (and how I’d do it)

- **Authentication / user identity**: add a `userId` to holds and require it on confirm/release
- **Multiple rooms / time ranges**: add `roomId`, `startTime`, `endTime` + a uniqueness constraint
- **Idempotency keys**: accept an idempotency header and store request outcomes
- **Background expiration worker**: scheduled job to release expired holds, emit metrics
- **Observability**: structured logs + metrics (holds created, confirms, expirations)

---

## AI tooling

Cursor AI can speed up scaffolding (controllers/DTOs), refactors, and test boilerplate.  
All correctness-critical logic (state transitions, concurrency handling, expiration semantics) was designed intentionally and kept simple enough to reason about.
