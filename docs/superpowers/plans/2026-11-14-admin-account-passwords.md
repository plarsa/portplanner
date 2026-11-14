# Admin account passwords Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An administrator can create an account for a person and issue that account a new generated password, from the person's detail panel in the admin app.

**Architecture:** One new backend endpoint beside the existing account API, reusing `PasswordGenerator`, `SessionInvalidator` and `UserCreatedResponse`. One new frontend API module and a Konto section plus one footer button in the existing `PersonsView` detail panel. No migration: `password_hash` and `must_change_password` already exist.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, H2. Vue 3 + Vite.

**Spec:** `docs/superpowers/specs/2026-11-14-admin-account-passwords-design.md`

## Global Constraints

- Code, comments, log and error messages in English. Interface text in Swedish.
- No new dependencies in either SPA.
- `spring.jpa.hibernate.ddl-auto: validate`. This change adds no column and needs no Flyway migration.
- A password is generated, never chosen by an administrator, and returned exactly once.
- Backend tests: `cd portplanner-backend && mvn -o test -Dtest=<Class> -Dskip.npm=true -Dskip.installnodenpm=true`.
- Frontend has no test harness; it is verified with `npm run build`.

## Review Focus

Five inputs the design implies but does not describe. Each has a test in the task that owns the code.

1. A reset posted for an id that is not an account (including `0`, which is what the configured superadmin's `id: null` becomes in a hand-written call) must answer 404, never 500 (task 1).
2. A reset on a DISABLED account must succeed and must leave the status DISABLED, not re-open it (task 1).
3. The old password must stop working the moment a new one is issued (task 1).
4. An administrator who resets their own account ends their own session; the admin app must say so rather than appear broken when the next request 401s (task 2).
5. Creating an account for a person who already has one must be refused with the existing 409 and surfaced in the modal, not swallowed (task 2).

---

### Task 1: The reset endpoint

**Files:**
- Modify: `portplanner-backend/src/main/java/se/portplanner/service/UserService.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/controller/UserController.java`
- Test: `portplanner-backend/src/test/java/se/portplanner/security/UserApiTest.java`

**Interfaces:**
- Consumes: `PasswordGenerator.generate()`, `PasswordEncoder.encode(String)`, `SessionInvalidator.invalidate(String username)`, `UserPolicy.assertCanManage(Actor)`, `AuditService.log(String, String, Long, String)` — all already injected into `UserService`.
- Produces:
  - `UserCreatedResponse UserService.resetPassword(Actor actor, Long id)` — the existing record `(UserResponse user, String temporaryPassword)`.
  - `POST /api/users/{id}/reset-password` → 200 `UserCreatedResponse`.

- [ ] **Step 1: Write the failing tests**

Add to `UserApiTest`, following its existing setup for an authenticated admin and a created account.

```java
@Test void resetReturnsANewPasswordThatWorksAndRetiresTheOld() throws Exception {
    // create an account, keep its temporaryPassword as `first`
    // POST /api/users/{id}/reset-password -> 200, temporaryPassword != first
    // login with the new one -> 200; login with `first` -> 4xx
}

@Test void resetSetsMustChangePassword() throws Exception {
    // the response's user.mustChangePassword is true
}

@Test void resetLeavesADisabledAccountDisabled() throws Exception {
    // POST /users/{id}/disable, then reset -> 200 and user.status is still "DISABLED"
}

@Test void resetEndsTheAccountsSessions() throws Exception {
    // log the account in, reset, then a request with that session cookie is no longer authenticated
}

@Test void resetOnAnUnknownIdGives404() throws Exception {
    // POST /api/users/0/reset-password -> 404
}

@Test void aMemberCannotResetAPassword() throws Exception {
    // asUser("member:rw") -> 403
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd portplanner-backend && mvn -o test -Dtest=UserApiTest -Dskip.npm=true -Dskip.installnodenpm=true`
Expected: FAIL, the endpoint answers 404 for every case.

- [ ] **Step 3: Implement `resetPassword(Actor actor, Long id)` in `UserService`**

Place it after `update` and before `disable`. It calls `getOrThrow(id)` (which already throws `ResourceNotFoundException` → 404), `policy.assertCanManage(actor)`, generates and encodes a new password, sets `mustChangePassword` to true, leaves `status` untouched, saves, invalidates the account's sessions, and logs `USER_PASSWORD_RESET` with the username. Returns the plaintext once in `UserCreatedResponse`.

- [ ] **Step 4: Add the endpoint to `UserController`**

`@PostMapping("/users/{id}/reset-password")` with `@Operation(summary = "Issue a new one-time password. It is returned once")`, delegating to `users.resetPassword(Actor.from(auth), id)`.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `cd portplanner-backend && mvn -o test -Dtest=UserApiTest -Dskip.npm=true -Dskip.installnodenpm=true`
Expected: PASS

- [ ] **Step 6: Run the whole suite**

Run: `cd portplanner-backend && mvn -o test -Dskip.npm=true -Dskip.installnodenpm=true`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add portplanner-backend/src
git commit -m "feat: issue a new one-time password for an account"
```

---

### Task 2: Account actions in the person's detail panel

**Files:**
- Create: `portplanner-frontend/src/api/users.js`
- Modify: `portplanner-frontend/src/views/PersonsView.vue`

**Interfaces:**
- Consumes: `POST /api/users/{id}/reset-password` (task 1), and the existing `GET /api/users`, `GET /api/roles`, `POST /api/users`.
- Produces: `src/api/users.js` exporting `getUsers`, `getAssignableRoles`, `createUser`, `resetPassword`, in the style of `src/api/persons.js`.

- [ ] **Step 1: Write the API module**

```js
import api from './axios'

export const getUsers = () => api.get('/users')
export const getAssignableRoles = () => api.get('/roles')
export const createUser = (data) => api.post('/users', data)
export const resetPassword = (id) => api.post(`/users/${id}/reset-password`)
```

- [ ] **Step 2: Load the accounts alongside the persons**

In `PersonsView`, `load()` also calls `getUsers()` and keeps a map from `personId` to the account, so each person's account is known without a second request when the panel opens. An account whose `personId` is null is ignored, and so is the configured superadmin, which has `id: null`.

- [ ] **Step 3: Add the Konto section to the detail panel**

Between the `detail-grid` and the `boats-section`, a section titled KONTO showing either the account's username, roles and status in Swedish (`INVITED` → Inbjudet, `ACTIVE` → Aktivt, `DISABLED` → Avstängt) and its last login, or the line `Inget konto`.

- [ ] **Step 4: Add the footer button and the two modals**

The detail footer gains one button before Redigera: *Skapa konto* when the person has no account, *Nytt lösenord* when they do. Both use `BaseModal`.

The create modal prefills the person's email read-only, offers the roles from `getAssignableRoles()` in a select defaulting to `member:rw`, and posts `{email, roles: [selected], personId}`.

Both paths end in a password modal that shows `temporaryPassword` in a monospaced block with the two lines from the spec's sketch — that it is shown only now, and that the account's sessions have ended. Review Focus 4: when the reset targets the signed-in administrator's own account, the modal adds that they will be signed out. Review Focus 5: a failed create shows `e.response?.data?.error` in the modal, the way `save()` already does.

- [ ] **Step 5: Verify the build**

Run: `cd portplanner-frontend && npm run build`
Expected: builds with no errors, and a `PersonsView` chunk is emitted.

- [ ] **Step 6: Commit**

```bash
git add portplanner-frontend/src
git commit -m "feat: create an account and issue a new password from the person panel"
```
