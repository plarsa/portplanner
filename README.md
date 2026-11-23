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

| Path | What it serves |
|---|---|
| `/` | Landing page with a link to each app |
| `/member/` | Member SPA |
| `/admin/` | Admin SPA |
| `/api/` | REST API |

Without a configured superadmin the service starts on a generated password that it logs
once at startup. To keep an account across restarts, set `SUPERADMIN_USERNAME` and
`SUPERADMIN_PASSWORD`, or use the demo profile, which enables every module and ships a
known demo account:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

To work on a frontend with hot reload instead of the bundled build:

```bash
cd portplanner-frontend   # or portplanner-member
npm install
npm run dev
```

The admin dev server is on `http://localhost:5173/admin/` and the member one on
`http://localhost:5174/member/`; both proxy `/api` to port 8080.

## Deployment

CI publishes the jar to the repository's Maven registry on GitHub Packages. A push to
main publishes the snapshot version set in the pom; a tag `vX.Y.Z` publishes that
version. Downloading a package needs a token with `read:packages`, even though the
repository is public.

See `deploy/README.md` for the one-time EC2 setup and `deploy/portplaner.service` for the
systemd unit.

## License

Apache License 2.0 — see `LICENSE`.
# portplanner-www
