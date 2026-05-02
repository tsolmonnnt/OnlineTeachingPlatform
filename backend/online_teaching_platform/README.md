# Online Teaching Platform (backend)

Spring Boot 4 + Java 21 REST API for the diploma project (teacher marketplace, bookings, materials, quizzes).

## Run locally

```bash
cd backend/online_teaching_platform
mvn spring-boot:run
```

**IntelliJ:** set the run configuration **Working directory** to this module folder (`…/backend/online_teaching_platform`), not the parent `backend` folder, so `.env` resolution and relative paths match command-line runs.

### PostgreSQL schema (Flyway + Hibernate)

- Flyway runs **`db/migration`** scripts before JPA. Logs include Flyway at INFO (`logging.level.org.flywaydb`).
- Default Hibernate mode is **`validate`** (no automatic DDL changes). Flyway **V1** and **V2** repair legacy data (`quiz_questions`, `teacher_profiles.verified`, orphan rows). **V3** adds `course_subject_id` to `teacher_availability_slots`, `bookings`, `teaching_materials`, and `quizzes`.

If startup fails with **`Schema validation: missing column [course_subject_id]`**, Flyway **V3** has not been applied to your PostgreSQL database. From `backend/online_teaching_platform` run `mvn clean compile` (so `target/classes/db/migration` contains all scripts), then start the app again so Flyway can migrate. If it still fails, open `db/migration/V3__course_subject_links.sql` and run the same SQL in your SQL client against the `OnlineTeachingPlatform` database, then insert or repair the Flyway history row for version `3` if needed (`flyway_schema_history`).

If Flyway reports a **checksum mismatch** for version **3** after pulling an updated `V3__course_subject_links.sql`, run **`flyway repair`** (or delete the `3` row from `flyway_schema_history` only if you know the migration effects), then restart.

**Empty database (first run ever — no tables yet):** Hibernate must create the schema once. The `bootstrap` profile sets `ddl-auto: update` and **turns Flyway off** so startup does not run repair migrations before tables exist. After one successful bootstrap start (wait until the app is up, then stop it), run again **without** `bootstrap` so Flyway applies **V1–V3** and Hibernate uses `validate`.

```bash
cd backend/online_teaching_platform
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=bootstrap"
```

**IntelliJ:** add active profile `bootstrap` for that first run only. Then remove it and start normally.

If you see **`Schema validation: missing table [bookings]`** (or similar), the database is still empty — use `bootstrap` once as above.

Then run again **without** `bootstrap`. Profile `bootstrap` is in `application-bootstrap.yaml`.

**`flyway_schema_history` out of sync** (migrations marked applied but DB was restored or edited manually): in PostgreSQL inspect `flyway_schema_history`; if needed delete the rows for versions `1`/`2` and restart so migrations re-run, or execute the SQL from `V2__postgresql_repair_idempotent.sql` manually.

### Local environment file (Cloudinary, etc.)

1. Copy `.env.example` to `.env` in this directory (the real `.env` is **gitignored**).
2. Set **`CLOUDINARY_CLOUD_NAME`** to the value from the Cloudinary dashboard (Product environment → **Cloud name**). Uploads will not work until this is set.
3. Optional: `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `CLOUDINARY_FOLDER` — or rely on the same keys in the process environment.

On startup, `DotEnvBootstrap` loads `.env` into JVM system properties (real OS env vars take precedence), so `application.yaml` references like `${CLOUDINARY_CLOUD_NAME}` work.

Requires PostgreSQL by default (`application.yaml`). Automated tests use in-memory H2 with Flyway disabled (`src/test/resources/application.yaml` and `application-test.yaml`).

## Configuration

| Purpose | Property / env |
|--------|----------------|
| JWT signing | `security.jwt.secret` / `JWT_SECRET` |
| PostgreSQL | `spring.datasource.*` |
| Cloudinary (optional uploads) | `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` |
| Cloudinary folder | `CLOUDINARY_FOLDER` (default `subjectFiles`) |
| Upload preset (optional) | `CLOUDINARY_UPLOAD_PRESET` — only set if you use a dashboard preset (e.g. `TeacherFile_presets`). If uploads fail with “invalid preset”, leave this unset for signed server uploads. |

Never commit API secrets to git — use environment variables or a local `.env` not tracked by version control.

Without Cloudinary credentials, material upload returns HTTP 503 with a clear message.

## API docs

With the app running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Tests

```bash
mvn test
```

Integration flow uses JDK `HttpClient` + random port (no MockMvc).

## Schedule, courses, and student access

- **Slots:** One availability slot is exactly **30 minutes** (`end_time = start_time + 30m`). Starts must align to **:00** or **:30** minutes; the API returns **400** otherwise.
- Each slot is tied to a **`course_subject_id`** from `course_subjects` (Flyway **V3** adds FKs on slots, bookings, `teaching_materials`, `quizzes`). Legacy rows may have null course FK until migrated or recreated.
- **Bookings:** Creating a booking copies the slot’s course onto the booking; optional `subject` text must match the catalog course name. Students only get **material download URLs** and **quiz access** when **`CourseAccessService`** finds a **CONFIRMED** booking for the same **teacher + course subject**.
- **`/error`** is permitted without auth so Spring Boot’s error pipeline does not turn **400/409** API responses into misleading **403** for API clients.

Teachers can **update** an unbooked slot with **`PUT /api/schedules/me/{slotId}`** (same JSON body as create).
