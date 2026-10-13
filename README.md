# Goal Tracking System

A full-stack goal planner with JWT authentication, milestone tracking, skill-plan generation, dashboard progress, and optional Groq-powered breakdowns.

## Stack

- Backend: Java 21, Spring Boot 3, Spring Security, JPA, MySQL 8, jjwt, BCrypt
- Frontend: React 18, Vite, React Router, Axios, plain CSS

## Setup

1. Create MySQL database access. Hibernate uses `ddl-auto=update`; the database itself can be created with:

```sql
CREATE DATABASE goal_tracker;
```

Alternatively, with Docker installed, start the included MySQL 8 service:

```bash
docker compose up -d mysql
```

The Compose development password is `goal_tracker_dev`. For a locally installed MySQL server, make sure it is running on port `3306` and update `DB_USER`/`DB_PASSWORD` in `backend/.env`.

2. Copy `backend/.env.example` to `backend/.env` and fill in `DB_PASSWORD`, a long `JWT_SECRET`, and `GROQ_API_KEY` for AI breakdowns. The key is read only by the backend and is never sent to the browser.

3. Run the backend:

```bash
cd backend
mvn spring-boot:run
```

4. Run the frontend in another terminal:

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. Login and registration are the entry routes; users land in `/app`, managers in `/manager`, and admins in `/admin`. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` in `backend/.env` to seed the first admin account.

## API examples

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"password123","displayName":"You"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"password123"}'

curl -H 'Authorization: Bearer YOUR_TOKEN' http://localhost:8080/api/user/dashboard

curl -X POST http://localhost:8080/api/user/goals \
  -H 'Authorization: Bearer YOUR_TOKEN' -H 'Content-Type: application/json' \
  -d '{"title":"Launch my portfolio","description":"Publish three case studies","targetDate":"2026-12-31"}'
```

Manager routes are under `/api/manager/**`; admin routes are under `/api/admin/**`. Role checks are enforced by Spring Security and method security, not just by the frontend.

## Security notes

All non-auth endpoints require Bearer JWT authentication and scope every query to the authenticated user. AI planning is limited to 10 requests per user per hour. Inputs are validated before a Groq call, the system prompt is fixed, response JSON is validated and retried once, and rejected content returns HTTP 422.

## Removed prototype files

The old OpenSpace Skill Planner Java package, PostgreSQL/Flyway configuration, skill-plan DTOs/services/tests, React demo components, and generated build outputs were removed. Only files used by this Goal Tracking System remain.
