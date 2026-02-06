# Escape Room Booking API (Hold + Confirm)

A small Spring Boot API for booking escape room time slots with a hold mechanism: **hold** a slot for 5 minutes, **confirm** a hold into a booking, or **release** a hold. The design uses a simple state machine with atomic SQL updates to avoid double holds/bookings under concurrency.

---

## 1. Setup instructions to run tests

### Prerequisites

- **JDK 11**
- **Gradle 7.6.1** (via wrapper; no global install required)

The project uses the Gradle wrapper (`./gradlew`), so you do not need to install Gradle.

### Build and run tests

```bash
./gradlew build    # build
./gradlew test     # run tests (no Docker required)
```

Tests run against an in-memory **H2** database in MySQL-compatibility mode. They cover:

- Expiration behaviour (hold expires after 5 minutes, confirm returns 410 when expired)
- Concurrent hold attempts (only one succeeds per slot)
- Confirm vs expire race (no inconsistent state)

If the wrapper fails (e.g. missing or invalid `gradle-wrapper.jar`), regenerate it:

```bash
gradle wrapper --gradle-version 7.6.1
```

(Install Gradle if needed, e.g. `brew install gradle` or [gradle.org/install](https://gradle.org/install), then run the command above from the project root.)

### Run the application (optional)

**With Docker (MySQL + API):**

- Docker + Docker Compose required.

```bash
docker compose up --build
```

- API: http://localhost:8080  
- MySQL: localhost:3306 (user: `escape`, pass: `escape`, db: `escape_room`)

**Without Docker (dev profile, H2 in-memory):**

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**Quick manual check:**

```bash
curl -X POST http://localhost:8080/api/slots/1/hold
# use holdId from response:
curl -X POST http://localhost:8080/api/holds/<holdId>/confirm
```

Stop Docker: `docker compose down`.

### API reference (for manual testing)

| Action           | Method | Path                          |
|------------------|--------|-------------------------------|
| Hold a slot      | POST   | `/api/slots/{slotId}/hold`    |
| Confirm a hold   | POST   | `/api/holds/{holdId}/confirm` |
| Release a hold   | DELETE | `/api/holds/{holdId}`         |
| Get slot (debug) | GET    | `/api/slots/{slotId}`         |

Hold response: `{ "holdId": "uuid", "expiresAt": "2026-01-01T00:00:05Z" }`. Confirm returns 200 on success, 410 GONE if expired or invalid.

---

## 2. Key architecture decisions and trade-offs

### Why Spring Boot

Spring Boot is used to reduce boilerplate and configuration, and to build on the Spring ecosystem (data, web, validation) with sensible defaults. Trade-off: more framework “magic” in exchange for faster development and consistency.

### State machine

Each slot is in one of:

- **AVAILABLE**
- **HELD** (has `holdId` and `holdExpiresAt`)
- **BOOKED**

Allowed transitions:

- AVAILABLE → HELD (hold)
- HELD → BOOKED (confirm)
- HELD → AVAILABLE (release or expiration)

All transitions are enforced in the service layer and implemented via conditional SQL updates.

### Concurrency: atomic updates

Concurrency is handled with **atomic compare-and-set style updates** in SQL (e.g. “update only if status = AVAILABLE”). This:

- Avoids in-process locking, which does not scale across multiple API instances.
- Keeps correctness in the database and makes the behaviour easy to reason about.

Trade-off: we rely on the database for consistency instead of application-level locks or distributed locking.

### Expiration: opportunistic cleanup

Expiration uses **opportunistic cleanup**: when handling a hold or confirm, the service runs a lightweight query to release any expired holds. There is no dedicated background job.

- **Trade-off:** No guaranteed cleanup interval; expired holds may remain until the next relevant request. For a small service and 5-minute TTL this is acceptable.
- **Alternative (not implemented):** A scheduled job or TTL-based cache (e.g. ElastiCache) for more predictable cleanup and better observability; see section 3.

---

## 3. What wasn’t implemented: how I’d design it, alternatives, and why

### Background expiration worker

**What:** A scheduled job (e.g. every minute) that finds slots in `HELD` with `hold_expires_at < now()` and sets them back to `AVAILABLE`.

**Design:** Use Spring `@Scheduled` or a separate worker process. Use a single SQL update with `WHERE status = 'HELD' AND hold_expires_at < ?` to avoid touching rows that are still valid. Add metrics (e.g. number of expired holds per run) and optional alerting.

**Why not in this repo:** Kept the solution minimal; opportunistic cleanup is correct and sufficient for the scope. A background job would be the next step for production.

### TTL-based expiration (e.g. ElastiCache)

**What:** Store hold metadata in a cache with TTL; when the key expires, a callback or a separate process marks the slot as AVAILABLE.

**Alternatives:** Redis/ElastiCache TTL + worker or keyspace notifications; or a “sweep” job that reads expired keys and updates the DB.

**Trade-offs:** Reduces load on the DB for expiration checks and gives precise TTL semantics, but adds infrastructure and eventual consistency between cache and DB. For a single-DB, small-scale API, DB-only expiration (opportunistic or scheduled) is simpler.

### Observability (logs and metrics)

**What:** Structured logging (e.g. JSON logs with correlation IDs) and metrics (counters for holds, confirms, releases, expirations; latency percentiles).

**Design:** Use Micrometer + Prometheus (or similar) and log in a structured format (e.g. Logback JSON). Optionally add tracing (e.g. Sleuth/Zipkin) for request flows.

**Why not here:** Not required for the exercise; would be part of production hardening.

---

## 4. AI tools used (if any) and how they helped

**Cursor** was used for:

- **Scaffolding:** Controllers, DTOs, and repository/service boilerplate.
- **Refactoring:** Package renames, `jakarta` → `javax` for Spring Boot 2.7, and README formatting.
- **Tests:** Generating test structure and three tests that cover expiration, concurrent holds, and confirm-vs-expire behaviour.
- **README:** Formatting and restructuring (including this layout).

All correctness-critical logic (state transitions, atomic updates, expiration semantics) was designed and reviewed manually so the core behaviour remains simple and auditable.
