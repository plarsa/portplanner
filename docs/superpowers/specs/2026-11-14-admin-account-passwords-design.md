# Admin: create an account and issue a new password from the person list

## Intent and why

An account's password is generated once when the account is created and returned once.
Nothing can issue another one: there is no reset endpoint and no change-password endpoint,
so a member who never received the password, or lost it, is locked out permanently. The
admin app has no account view at all, so `/api/users` is reachable only with curl or
Swagger. The harbour office needs to do this from the admin app, where the person already is.

## Goals

- An administrator can give an existing account a new password, read it once, and pass it on.
- An administrator can create an account for a person who has none, without leaving the person list.
- A new password immediately ends that account's existing sessions.

## Non-goals

- A full account administration page (listing every account, editing roles, disabling). That is
  approach B from the discussion and stays API-only for now.
- Members changing their own password, and forcing a change at first login. That is approach C.
  `mustChangePassword` therefore remains a flag nothing acts on.
- Any new login method. `LoginMethod` keeps its single value `PASSWORD`.

## Anti-goals

- An administrator choosing or seeing a password they picked themselves. Passwords are generated.
- A password reachable twice. It is shown once, exactly as on account creation.
- Reaching the configured superadmin. It has no database row and must stay unreachable here.

## Constraints

- Java 25, Spring Boot 4.1, Vue 3 + Vite. No new dependencies in either SPA.
- Code, comments, log and error messages in English; interface text in Swedish.
- `spring.jpa.hibernate.ddl-auto: validate`, so any new column needs a Flyway migration. This
  change needs none: it writes `password_hash` and `must_change_password`, both already present.

## Approaches considered

- **A — account actions on the person (chosen).** Two buttons per row in `PersonsView`, plus one
  reset endpoint. Smallest thing that solves the lockout, in the view the office already uses.
  Leaves roles, disabling and the account list API-only.
- **B — a Konten page in admin.** Full account administration. Closes the whole gap but is three
  to four times the work, and the password button becomes one control among ten.
- **C — B plus members changing their own password.** The right end state for security; touches
  the login flow and the member SPA, far beyond what was asked.

Chosen: A, with a generated password rather than one the administrator types, because the
generator already exists and an administrator should never pick someone else's password.

## The design

**Backend**

- `POST /api/users/{id}/reset-password` → 200 with the existing `UserCreatedResponse`
  (`{user, temporaryPassword}`), reusing the shape account creation already returns.
- `UserService.resetPassword(Long id)`: generate, hash, set `mustChangePassword = true`,
  invalidate the account's sessions through `SessionInvalidator`, audit as
  `USER_PASSWORD_RESET`, return the plaintext once.
- Authorization: `UserPolicy.assertCanManage`, the same gate as creating an account.

**Frontend** (`portplanner-frontend`)

- New `src/api/users.js`: `getUsers`, `getAssignableRoles`, `createUser`, `resetPassword`.
- `PersonsView`'s detail panel gains a Konto section, and its footer gains one account button:
  *Skapa konto* when the person has none, *Nytt lösenord* when they do. The rows themselves are
  clickable and open the panel, so nothing is added to them. Both buttons open a modal showing
  the generated password once, with a line saying it cannot be fetched again. The create modal
  prefills the person's email and offers the assignable roles, defaulting to `member:rw`.

```
┌ Anna Ek ─────────────────────────────────────────┐
│ E-post      anna@hamn.se                         │
│ Telefon     070-123 45 67                        │
│ ...                                              │
│ KONTO                                            │
│ anna@hamn.se · member:rw · Aktivt                │
│ Senaste inloggning 2026-11-12 09:14              │
│ BÅTAR                                            │
│ ...                                              │
│            [Ta bort] [Redigera] [Nytt lösenord]  │
└──────────────────────────────────────────────────┘

  ┌ Nytt lösenord för anna@hamn.se ──────────────┐
  │ 7fKp2mQx9nTw4bYz                             │
  │ Visas bara nu och kan inte hämtas igen.      │
  │ Kontots aktiva sessioner har avslutats.      │
  │                                     [Klar]   │
  └──────────────────────────────────────────────┘
```

## Calls I made, for checking

- **Reset reuses `UserCreatedResponse`** rather than a new DTO: same two fields, same meaning.
- **Status is left alone.** A reset does not move an ACTIVE account back to INVITED; only
  `mustChangePassword` is set. A DISABLED account may be reset — it still cannot sign in until
  re-enabled, so refusing would add a rule without protecting anything.
- **Resetting your own account is allowed.** It ends your own session, which is honest: you have
  the new password. Self-modification is blocked for roles and for disabling because those can
  lock an administrator out of their own rights; a password cannot.
- **The configured superadmin is unreachable** without extra code: it is listed with `id: null`,
  so there is no id to post to. Its password lives in configuration.
- **Account creation from the person needs a role**, so the modal shows the roles from
  `GET /api/roles` and defaults to `member:rw`, the role a person in the harbour register gets.
- **Branch:** a new branch off `main`, not a continuation of `feat/rentals`, since the two are
  independent. `feat/rentals` is still unmerged.

## Left to the builder

- Whether the reset modal and the create modal are one component or two.
- Whether the Konto section renders before or after the Båtar section in the detail panel.
- The exact Swedish wording in the modals and the Konto column.
- Where the audit description sits in `UserService` relative to the existing log calls.
