# Online Teaching Platform Backend

Spring Boot REST API for the diploma project "Time Teacher Online Platform".

## Architecture

- Modular monolith (`auth`, `user`, `teacher`, `course`, `schedule`, `booking`, `notification`)
- JWT authentication + role-based access (`STUDENT`, `TEACHER`, `ADMIN`)
- PostgreSQL for runtime, H2 for tests

## ER Model (implemented entities)

- `users` (id, full_name, email, password, role, timestamps)
- `teacher_profiles` (user 1:1, bio/headline, subjects, skills, languages, rate, contact)
- `course_categories` 1:N `course_subjects`
- `teacher_availability_slots` (teacher profile N:1, start/end, booked)
- `bookings` (student user N:1, teacher profile N:1, slot 1:1, status)
- `notifications` (recipient user N:1, title/message, read flag)

## Key API endpoints

- Auth: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- Teacher:
  - `GET /api/teachers` (search/filter)
  - `GET /api/teachers/{teacherId}` (detail)
  - `GET/PUT /api/teachers/me` (teacher self profile)
- Course catalog: `GET /api/course/categories`, `GET /api/course/subjects`
- Schedule:
  - `GET /api/schedules/teacher/{teacherId}?from=&to=`
  - `GET/POST /api/schedules/me`, `DELETE /api/schedules/me/{slotId}`
- Booking:
  - `POST /api/bookings`
  - `GET /api/bookings/me`
  - `PATCH /api/bookings/{bookingId}/confirm`
  - `PATCH /api/bookings/{bookingId}/cancel`
- Notifications:
  - `GET /api/notifications/me`
  - `PATCH /api/notifications/{notificationId}/read`
- Admin users:
  - `GET /api/users`
  - `PATCH /api/users/{id}/role`

## Run

Use `application.yaml` for DB/JWT, then run:

```bash
./mvnw spring-boot:run
```

Default seeded admin (if not existing):

- Email: `admin@otp.mn`
- Password: `Admin123!`

Override with env/properties:

- `app.admin.email`
- `app.admin.password`
- `app.admin.fullName`

## Test

```bash
./mvnw test
```

