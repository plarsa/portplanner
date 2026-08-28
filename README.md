# Portplanner

Portplanner is a management system for a marina/harbor operator. It handles boat slip
assignment, a waiting queue, dock layout, tariffs, and winter storage (haul-out) — plus a
separate self-service portal for boat owners.

## Structure

This is a multi-module repo with one Spring Boot backend and two independent Vue 3 SPAs,
built together into a single deployable jar:

| Module | What it is |
|---|---|
| `portplanner-backend` | Spring Boot 4 REST API (Java 25). Owns the domain model, auth, and both frontends' build output. |
| `portplanner-frontend` | Admin SPA (Vue 3 + Vite) — used by harbor staff to manage persons, boats, docks, slips, queue, tariffs, winter storage, audit log, import/export. |
| `portplanner-member` | Member SPA (Vue 3 + Vite) — self-service portal for boat owners: dashboard, their boats, their slip, queue status, haul-out booking, profile. |

## Tech stack

- **Backend**: Java 25, Spring Boot 4.1 (Spring Framework 7), Spring Data JPA (Hibernate 7), Spring Security 7, Flyway
- **Database**: H2 (file-based, embedded — see `data/`), Flyway-managed schema
- **API docs**: springdoc-openapi / Swagger UI
- **Frontend**: Vue 3 (Composition API), Vite, Vue Router, Pinia, Axios
- **Build**: Maven (backend), with `frontend-maven-plugin` building both SPAs and bundling
  their output into the backend jar

## Running locally

Requires JDK 25 and Node.js (Maven will install its own pinned Node/npm via
`frontend-maven-plugin` if needed).

```bash
cd portplanner-backend
mvn spring-boot:run
```

This builds both frontends, runs Flyway migrations against the local H2 database
(`data/portplanner`), and starts the app on `http://localhost:8080`. API docs are at
`/swagger-ui/index.html`.

To work on a frontend with hot reload instead of the bundled build:

```bash
cd portplanner-frontend   # or portplanner-member
npm install
npm run dev
```

## Deployment

CI builds the jar and ships it to an EC2 instance via GitHub Actions + S3 + SSM; see
`deploy/README.md` for one-time infrastructure setup and `deploy/portplaner.service` for
the systemd unit.

## License

Apache License 2.0 — see `LICENSE`.
