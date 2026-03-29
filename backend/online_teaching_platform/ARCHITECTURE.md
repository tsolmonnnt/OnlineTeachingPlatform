# System Architecture and ER Model

## High-level architecture (Modular Monolith)

- **Frontend**: React + Vite SPA
- **Backend**: Spring Boot REST API
- **Security**: JWT authentication + Role-based access control (`STUDENT`, `TEACHER`, `ADMIN`)
- **Database**: PostgreSQL (runtime), H2 (test)

Modules in backend:

- `auth`: register/login/me, JWT provider
- `user`: admin user management
- `teacher`: teacher profile + teacher search/detail
- `course`: subject/category catalog + seed data
- `schedule`: teacher availability slots
- `booking`: lesson booking lifecycle
- `notification`: in-app notification feed

## ER diagram (text model)

```mermaid
erDiagram
    USERS ||--|| TEACHER_PROFILES : "teacher account"
    USERS ||--o{ BOOKINGS : "student creates"
    USERS ||--o{ NOTIFICATIONS : "receives"

    TEACHER_PROFILES ||--o{ TEACHER_PROFILE_SUBJECTS : "has"
    TEACHER_PROFILES ||--o{ TEACHER_PROFILE_SKILLS : "has"
    TEACHER_PROFILES ||--o{ TEACHER_PROFILE_LANGUAGES : "has"

    COURSE_CATEGORIES ||--o{ COURSE_SUBJECTS : "contains"

    TEACHER_PROFILES ||--o{ TEACHER_AVAILABILITY_SLOTS : "publishes"
    TEACHER_PROFILES ||--o{ BOOKINGS : "teaches"

    TEACHER_AVAILABILITY_SLOTS ||--|| BOOKINGS : "selected for"

    USERS {
      bigint id PK
      string full_name
      string email UK
      string password_hash
      enum role
      datetime created_at
      datetime updated_at
    }

    TEACHER_PROFILES {
      bigint id PK
      bigint user_id FK UK
      string headline
      text bio
      decimal hourly_rate
      string avatar_url
      string location
      string phone
      int years_experience
      datetime created_at
      datetime updated_at
    }

    COURSE_CATEGORIES {
      bigint id PK
      string name UK
      string description
    }

    COURSE_SUBJECTS {
      bigint id PK
      string name UK
      string description
      bigint category_id FK
    }

    TEACHER_AVAILABILITY_SLOTS {
      bigint id PK
      bigint teacher_profile_id FK
      datetime start_time
      datetime end_time
      boolean booked
    }

    BOOKINGS {
      bigint id PK
      bigint student_user_id FK
      bigint teacher_profile_id FK
      bigint availability_slot_id FK UK
      string subject
      text note
      enum status
      datetime created_at
      datetime updated_at
    }

    NOTIFICATIONS {
      bigint id PK
      bigint recipient_user_id FK
      string title
      string message
      boolean is_read
      datetime created_at
    }
```

