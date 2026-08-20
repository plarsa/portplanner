# Portplaner – Implementationsplan

## Sammanfattning

System för att hantera båtplatstilldelning vid en marina/hamn. Handläggare (admin/hamnfogde) tilldelar båtplatser löpande baserat på båtstorlek kontra platsstorlek. Kösystem finns för väntande. AI-stöd via Claude API läser inkommande Gmail och föreslår svar som godkänns manuellt.

---

## Teknisk stack

| Lager | Teknik |
|---|---|
| Backend | Java 25, Spring Boot 3.x |
| Frontend | Vue.js 3 (Composition API, Vite) |
| API-dokumentation | Swagger / SpringDoc OpenAPI 3 |
| Autentisering | Spring Security – username/password (JWT) |
| Databas | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| AI | Claude API (claude-sonnet-4-6) |
| E-post | Gmail API (OAuth2 service account) |
| Byggverktyg backend | Maven |
| Byggverktyg frontend | Vite + npm |

---

## Domänmodell

```
Person
├── id
├── firstName
├── lastName
├── email
├── phone
└── boats: List<Boat>

Boat (Båt)
├── id
├── name
├── registrationNumber
├── length (meter)
├── width (meter)
├── draft (djupgång, meter)
└── owner: Person

Dock (Brygga)
├── id
├── name
├── description
└── slips: List<Slip>

Slip (Båtplats)
├── id
├── slipNumber
├── maxLength (meter)
├── maxWidth (meter)
├── maxDraft (meter)
├── dock: Dock
└── status: AVAILABLE | OCCUPIED | MAINTENANCE

Assignment (Tilldelning)
├── id
├── boat: Boat
├── slip: Slip
├── assignedDate
├── endDate (nullable)
└── status: ACTIVE | ENDED

QueueEntry (Kö)
├── id
├── person: Person
├── boat: Boat
├── requestedDate
├── notes
└── status: WAITING | ASSIGNED | CANCELLED

MailThread (E-posttråd)
├── id
├── gmailThreadId
├── subject
├── fromEmail
├── receivedAt
├── rawContent
├── aiSuggestedReply (text)
├── status: NEW | DRAFT | SENT | IGNORED
└── linkedPerson: Person (nullable)

AppUser (Systemanvändare)
├── id
├── username
├── passwordHash
└── role: ADMIN | HARBOUR_MASTER
```

---

## Användarroller

| Roll | Behörighet |
|---|---|
| **ADMIN** | Full åtkomst: CRUD på alla entiteter, hantera användare |
| **HARBOUR_MASTER** | Hantera tilldelningar, kö, läsa/svara på mail |

---

## Funktionella moduler

### 1. Personhantering
- Skapa, redigera, ta bort person
- Sök på namn, e-post
- Visa personens båtar och aktiva tilldelningar

### 2. Båthantering
- Skapa, redigera, ta bort båt kopplad till ägare
- Visa båtens nuvarande placering och historik

### 3. Bryggor & Båtplatser
- Skapa/redigera bryggor
- Skapa/redigera platser per brygga med måttangivelser
- Sätt plats i underhållsläge

### 4. Tilldelning
- Tilldela en båt till en plats manuellt (validering: båten får inte överstiga platsens mått)
- Avsluta en tilldelning (frigör platsen)
- Se alla aktiva tilldelningar
- Historik per båt/plats

### 5. Kösystem
- Lägg till person+båt i kö
- Visa köordning (FIFO, sorterat på requestedDate)
- Föreslå lediga platser som passar båtens mått
- Tilldela direkt från kö → skapar Assignment och tar bort QueueEntry

### 6. AI-mailhantering
- **Hämta mail:** Gmail API hämtar olästa mail periodiskt (polling var 5 min)
- **AI-analys:** Claude API analyserar mailet och:
  - Klassificerar typ (ansökan om plats, fråga, uppsägning, övrigt)
  - Försöker matcha avsändaren mot registrerad Person
  - Genererar förslag på svar
- **Handläggarvy:** Lista nya mail med AI-förslag
- **Godkänn/redigera/skicka:** Handläggaren kan redigera förslaget och godkänna → skickar via Gmail API
- **Ignorera:** Markera som ej relevant

---

## Projektstruktur (backend)

```
portplaner-backend/
├── src/main/java/se/portplaner/
│   ├── config/          # SecurityConfig, SwaggerConfig, GmailConfig
│   ├── controller/      # REST-controllers per modul
│   ├── service/         # Affärslogik
│   ├── repository/      # Spring Data JPA repositories
│   ├── model/           # JPA-entiteter
│   ├── dto/             # Request/Response DTOs
│   ├── ai/              # Claude API-integration
│   ├── mail/            # Gmail API-integration + polling
│   └── security/        # JWT-filter, UserDetailsService
└── src/main/resources/
    └── application.yml
```

## Projektstruktur (frontend)

```
portplaner-frontend/
├── src/
│   ├── views/           # Persons, Boats, Docks, Slips, Assignments, Queue, Mail
│   ├── components/      # Delade komponenter (tabeller, formulär, modal)
│   ├── stores/          # Pinia stores
│   ├── api/             # Axios-klienter per modul
│   ├── router/          # Vue Router
│   └── auth/            # Login, JWT-hantering
```

---

## Implementationsfaser

### Fas 1 – Grund & infrastruktur
- [ ] Initiera Spring Boot-projekt (Java 25, Maven)
- [ ] Konfigurera PostgreSQL + JPA + Flyway
- [ ] Spring Security med JWT (login-endpoint)
- [ ] SpringDoc OpenAPI / Swagger UI
- [ ] Initiera Vue 3-projekt (Vite, Vue Router, Pinia, Axios)
- [ ] Login-sida och JWT-hantering i frontend
- [ ] Grundlayout med navigering och rollbaserade menyer

### Fas 2 – Kärnentiteter (CRUD)
- [ ] Person – backend + frontend
- [ ] Båt – backend + frontend
- [ ] Brygga – backend + frontend
- [ ] Båtplats – backend + frontend

### Fas 3 – Tilldelning & kö
- [ ] Tilldelningslogik med måttvalidering
- [ ] Tilldelnings-vy (aktiva, historik)
- [ ] Kösystem – backend
- [ ] Kö-vy med platsförslag baserat på båtmått

### Fas 4 – AI-mailhantering
- [ ] Gmail API-integration (Google OAuth2 service account, polling var 5 min)
- [ ] Claude API-integration (klassificering + svarsförslag)
- [ ] MailThread-entitet och repository
- [ ] Mailhanterarvy (lista, läs, redigera svar, skicka/ignorera)

### Fas 5 – Finslipning
- [ ] Dashboard med översikt (lediga platser, kölängd, nya mail)
- [ ] Buggfixar och UX-förbättringar

---

## API-översikt (exempel)

```
POST   /api/auth/login

GET    /api/persons
POST   /api/persons
PUT    /api/persons/{id}
DELETE /api/persons/{id}

GET    /api/boats
POST   /api/boats
PUT    /api/boats/{id}

GET    /api/docks
POST   /api/docks
GET    /api/docks/{id}/slips

GET    /api/slips
POST   /api/slips
PUT    /api/slips/{id}

GET    /api/assignments
POST   /api/assignments
PUT    /api/assignments/{id}/end

GET    /api/queue
POST   /api/queue
DELETE /api/queue/{id}
GET    /api/queue/{id}/suggestions   # Föreslår lämpliga lediga platser

GET    /api/mail
POST   /api/mail/{id}/send           # Skickar godkänt AI-svar
POST   /api/mail/{id}/ignore
PUT    /api/mail/{id}/draft          # Uppdaterar utkast
```

---

## Miljövariabler / konfiguration

```yaml
# application.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000

gmail:
  service-account-key-path: ${GMAIL_SERVICE_ACCOUNT_KEY}
  delegated-user: ${GMAIL_DELEGATED_USER}
  poll-interval-ms: 300000

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: claude-sonnet-4-6
```

---

## Beslut

| Fråga | Beslut |
|---|---|
| Excel-import | Ej aktuellt – systemet byggs från grunden |
| Gmail-autentisering | Google OAuth2 Service Account (server-till-server) |
| JWT-livslängd | 24 timmar |
| Schema-migrationer | Flyway |
