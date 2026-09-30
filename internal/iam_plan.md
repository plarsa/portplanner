# Plan: IAM, roller och kontohantering (utan Google-inloggning)

Genomförandeplan för `iam_spec.md`. Fokus: **kunna ge roller till användare** och genomdriva rollerna i backend. Google-inloggning (avsnitt 8, GOO-*) ingår inte, men inloggningsmetoder byggs som en lista så att Google och BankID kan läggas till senare.

## 1. Mål och avgränsning

**Ingår**
- Roller som lista per konto med nivå (`admin:rw`, `admin:r`, `member:rw`). Roller definieras implicit av modulernas `access`-konfiguration.
- Modulernas `access` som lista av `roll:nivå` (tak), med ny nivåberäkning.
- Konfigurerad `superadmin` i applikationskonfigurationen. Inget standardkonto i databasen.
- Kontomodell: e-post (= `username`), status, inloggningsmetoder (lista), senaste inloggning.
- Kontohantering (skapa, ändra roller, inaktivera/återaktivera, koppla till person) med `UserPolicy`.
- Rollkontroll i backend för alla anrop (ROL-9), så att ett konto med bara `member` inte når admin-API:er.
- Användarvy i adminverktyget och rätt rollhantering i båda SPA:erna.
- Sessioner avslutas när ett konto inaktiveras eller får ändrade roller eller metoder.

**Skjuts till senare faser**
- E-postutskick, välkomstbrev, glömt lösenord, e-postbekräftelse (KON-4, KON-5, KON-9, avsnitt 7).
- Ansökan från medlemsportalen (avsnitt 10).
- Kontolåsning efter felförsök, rate limiting, CSRF, sessionslista (LOG-5, SÄK-2, SÄK-5, SÄK-7).
- Google-inloggning (avsnitt 8) och BankID. Datamodellen förbereds (LOG-11), men bara `PASSWORD` implementeras.

## 2. Ändrade förutsättningar (beslut från produktägaren, införda i specen)

- **Roller är en lista per konto** med nivå: `admin:rw`, `admin:r`, `member:rw`. Rollnamn är strängar som **definieras implicit** av modulernas `access` (t.ex. `audit.access: [admin:r, audit_master:r]` gör `audit_master` tilldelningsbar). `superadmin`, `admin` och `member` är reserverade namn med fasta regler.
- **Modulernas `access` är en lista av `roll:nivå` och fungerar som tak.** Kontots nivå i en modul = högsta över matchande poster av `min(kontots nivå, postens nivå)`. Exempel: `audit.access: [admin:r]` och kontot har `admin:rw` ger `read`.
- **`username` = e-postadress**, fylls automatiskt. Kolumnen `username` behålls.
- **Inloggningsmetoder är en lista per konto** (policy): `PASSWORD` idag, senare `GOOGLE`, `BANKID`. Lagras som rader, inte en kolumn per metod.
- **`superadmin` lever bara i applikationskonfigurationen** (`portplanner.iam.superadmins`: `username`, `password`, `enabled`). Inget konto skapas av migreringar eller kod. `admin`/`admin123` i `DataInitializer` tas bort.

## 3. Nuläge (verifierat i koden)

- `AppUser` (`app_users`): `username`, `passwordHash`, `role` (en), `person`. Ingen e-post, status, metodlista eller senaste inloggning.
- `Role` = `ADMIN`, `HARBOUR_MASTER`, `MEMBER`. `HARBOUR_MASTER` förekommer i `SecurityConfig`, i `portplanner-member/src/router/index.js` och som stand-in för `superadmin` i modulsystemet (`PortModule`, `ModuleService.SUPERADMIN_EQUIVALENT`, `application.yml`, tester).
- Modulsystemet (commit `9d74e82`) bygger på `Map<Role, AccessLevel>` per modul. Måste göras om till listor (fas 1).
- `DataInitializer` skapar kontot `admin`/`admin123` om det saknas.
- Inloggning: `POST /api/auth/login` med sessionscookie (session i databasen, 8 h). `GET /api/auth/me` returnerar bara användarnamn och roll.
- **Det finns ingen användarhantering**: inget `/api/users`, ingen vy.
- `SecurityConfig` skiljer bara `/admin/**` och `/api/me/**`. Allt annat under `/api/**` kräver bara inloggning, så en `MEMBER` kan idag anropa `/api/persons` m.fl. (modulgatingen täcker bara annoterade controllers).
- `MeService` hämtar personen via inloggat konto och filtrerar på den, så ägarskap är redan uppfyllt för `/api/me/**`. Övriga controllers (t.ex. `QueueController`) har ingen ägarskapskontroll.
- `ImportExportIntegrationTest` är trasig sedan tidigare (login-steget använder `admin`/`admin123`).

## 4. Faser

### Fas 1: Modulsystemet till roll-listor (refaktorering av det som redan finns)

- `ModulesProperties.ModuleConfig.access`: `List<RoleGrant>` parsad från strängar `roll:nivå` (nivå `r`/`rw`, rollnamn `[a-z][a-z0-9_]*`). En `Converter<String, RoleGrant>` ger tydligt startfel vid ogiltig post.
- `PortModule`: standardposter som listor enligt `module_spec.md` avsnitt 3.
- `ModuleService`: ersätt `accessLevel(id, Role)` med `accessLevel(id, Collection<RoleGrant>)` med ny beräkning (tak, högsta över matchande). Ny metod `roleCatalog()`. Startvalidering: `base.access` innehåller `superadmin:rw`; ogiltiga poster stoppar start; en ny okänd roll är inget fel.
- `ModuleService.roleOf` och `SUPERADMIN_EQUIVALENT` tas bort. Authorities får formen `ROLE_<roll>:<nivå>` (en per tilldelning), tolkade av `RoleGrant.fromAuthority`.
- `application.yml` och modultesterna byter till listformatet. `Role`-enumen används inte längre i modulkoden.
- Enhetstester av beräkningen med tabellerna i specen som testfall.

Klart när: modultesterna är omskrivna och gröna, beräkningen har egna enhetstester, appen startar.

### Fas 2: Konfigurerad superadmin och borttag av standardkonto

- `IamProperties` (`@ConfigurationProperties("portplanner.iam")`) med `superadmins: List<{username, password, enabled}>`. Start stoppar om ingen aktiverad `superadmin` med användarnamn och lösenord finns (ROL-6). Varna om lösenordet är klartext (saknar `{bcrypt}`-prefix).
- `UserDetailsServiceImpl` slår först upp konfigurerade `superadmin` (jämför `username`), sedan `AppUser`. Konfigurerad `superadmin` får authority `ROLE_superadmin:rw`, saknar `person` och använder alltid `PASSWORD` (LOG-16).
- `DataInitializer`: ta bort skapandet av `admin`/`admin123`.
- Konfigurerad `superadmin` visas senare i kontolistan som skrivskyddad rad men finns inte i `app_users`.
- Testprofilen får en konfigurerad `superadmin`, och `ImportExportIntegrationTest` skrivs om att använda den.
- Tester: start misslyckas utan aktiverad `superadmin`; `enabled: false` stänger av inloggning; ingen `admin` finns i ny databas.

### Fas 3: Kontomodell (databas)

Flyway `V13__iam_accounts.sql`:
- `app_users`: lägg till `email` (unik när satt), `status` (`INVITED`/`ACTIVE`/`DISABLED`, standard `ACTIVE`), `last_login_at`, `created_at`, `must_change_password`.
- Ny tabell `app_user_roles (user_id, role, level)` med `level` `R`/`RW`, unik per `(user_id, role)`. Migrering: `ADMIN` och `HARBOUR_MASTER` → `admin:rw`, `MEMBER` → `member:rw`. Därefter `DROP COLUMN role`.
- Ny tabell `app_user_login_methods (user_id, method)`. Alla befintliga konton får `PASSWORD`.
- Unikt index på `person_id` där det är satt (KON-6).
- `username`: befintliga konton behåller sitt värde tills e-post fylls i (öppen punkt). Nya konton får `username = email`.

Backend:
- `AppUser`: `email`, `status`, `roles` (`@ElementCollection` av `RoleGrant`), `loginMethods` (`Set<LoginMethod>`), `lastLoginAt`. `Role`-enumen tas bort. `LoginMethod`: `PASSWORD` nu, `GOOGLE` och `BANKID` läggs till när de byggs.
- Setter för e-post sätter alltid `username` (KON-1).
- `UserDetailsServiceImpl`: authorities från tilldelningarna. Bara `ACTIVE` (och `INVITED` med engångslösenord) kan logga in, `DISABLED` ger `enabled=false`. Nekas om `PASSWORD` saknas i kontots metodlista (LOG-12), med samma neutrala fel som fel lösenord.
- Inloggning med e-post; uppdatera `last_login_at`.
- `GET /api/auth/me`: `username`, `email`, `roles` (lista av `roll:nivå`), `loginMethods`, `status`, `personId`.

### Fas 4: Regler för kontohantering (`UserPolicy`)

En ren, testbar klass i `se.portplanner.security`:

| Regel | Källa |
|---|---|
| Kontohantering kräver `superadmin` eller `admin:rw`. `admin:r` får bara läsa listan | ROL-4 |
| `superadmin` kan aldrig tilldelas eller tas bort i databasen, och den konfigurerade kan inte ändras | ROL-5, ROL-11 |
| Ingen ändrar sina egna rolltilldelningar eller inaktiverar sig själv | ROL-5, ROL-10 |
| Bara roller i rollkatalogen (modulernas `access` plus `admin` och `member`) kan tilldelas | ROL-2 |
| En roll förekommer högst en gång per konto, högsta nivån gäller | ROL-1 |
| Konto med `member` kräver koppling till en person som saknar konto | KON-6 |
| Konton raderas inte, de inaktiveras | KON-7 |
| Metodlistan får inte vara tom för ett aktivt konto; bara aktiverade metoder kan väljas | LOG-13, LOG-14 |

Fel ger 403 med kod (t.ex. `ROLE_ASSIGNMENT_FORBIDDEN`, `SELF_MODIFICATION`, `UNKNOWN_ROLE`).

### Fas 5: Användar-API och rolltilldelning

`UserService` + `UserController` (`/api/users`, tagg "Users") och `RoleController` (`/api/roles`):

| Anrop | Åtkomst | Beskrivning |
|---|---|---|
| `GET /api/roles` | `admin:rw`, `superadmin` | Rollkatalogen som går att tilldela (aldrig `superadmin`) |
| `GET /api/users` | `admin` (`r`/`rw`), `superadmin` | Lista med filter på roll och status. Konfigurerad `superadmin` som skrivskyddad rad |
| `GET /api/users/{id}` | samma | Ett konto |
| `POST /api/users` | `admin:rw`, `superadmin` | Skapa: e-post, rolltilldelningar, metodlista (standard `PASSWORD`), ev. person. `username` sätts från e-post |
| `PUT /api/users/{id}` | `admin:rw`, `superadmin` | Ändra e-post (uppdaterar `username`), roller, metodlista, person |
| `POST /api/users/{id}/disable`, `/enable` | `admin:rw`, `superadmin` | Inaktivera/återaktivera |

- DTO:er (`UserRequest`, `UserResponse`) exponerar aldrig `passwordHash`. `UserRequest` har inget `username`-fält.
- Sessioner: vid inaktivering, ändrade roller eller borttagen inloggningsmetod avslutas kontots sessioner via `FindByIndexNameSessionRepository` (index på användarnamn). Verifiera att indexet fungerar med `spring-session-jdbc`.
- Loggning via `AuditService` (skriver alltid): `USER_CREATED`, `USER_ROLES_CHANGED` (före och efter), `USER_METHODS_CHANGED`, `USER_DISABLED`, `USER_ENABLED`. Aldrig lösenord.
- Rolländringar gäller senast vid nästa anrop (RÄT-10). Att sessionerna avslutas vid ändring ger det direkt.
- `PersonService` nekar radering av person som har konto (KON-10).
- Ingen `@RequiresModule` på dessa controllers (RÄT-7): åtkomst styrs av `UserPolicy` och `@PreAuthorize`.

### Fas 6: Första lösenord och lösenordsbyte

Ingen e-post finns än (välkomstbrev kommer i en senare fas). Tillfällig lösning tills dess:

- Skapande av konto genererar ett kryptografiskt slumpat engångslösenord som hashas och returneras **en gång** till admin. Status `INVITED`, `must_change_password = true`.
- Så länge `must_change_password` är sant ger alla anrop utom `POST /api/auth/change-password` och `GET /api/auth/me` 403 `PASSWORD_CHANGE_REQUIRED`.
- `POST /api/auth/change-password` (inloggad): nuvarande + nytt lösenord, minst 12 tecken, får inte innehålla e-post; avslutar övriga sessioner; sätter `ACTIVE`.
- Byts mot välkomstbrev när e-post byggs.
- Avviker tillfälligt från KON-3 (admin ser aldrig lösenordet).

### Fas 7: Genomdriv roller i backend

Mål: konto med bara `member` får 403 på `GET /api/persons`.

- `SecurityConfig`: `/api/auth/**` öppen; `/api/me/**` för konton med `member`-tilldelning (eller kopplad person, se öppen punkt); `/api/modules/**` för alla inloggade; övrigt under `/api/**` kräver att kontot har någon annan roll än enbart `member`. Byggs som en gemensam rollkontroll, inte spridda `hasRole`-anrop (authorities är nu `ROLE_roll:nivå`).
- `@PreAuthorize` på `UserController` som andra försvarslinje.
- Inventera vilka anrop member-SPA:n gör utanför `/api/me` (t.ex. `/api/queue/{id}/accept-offer`). De flyttas till `/api/me/**` med ägarskapskontroll (MED-3, MED-5) innan regeln aktiveras.
- `/admin/**` följer samma regel.

### Fas 8: Frontend

Admin-SPA:
- `UsersView.vue`, route `/users`, menyval "Användare". Ingen modul, men bara för `superadmin` och `admin` (`r` för läsning, `rw` för ändring).
- Lista med e-post (som är användarnamn), roller (chips som `admin:rw`), metoder, status och senaste inloggning. Konfigurerad `superadmin` visas låst.
- Formulär: e-post, rolltilldelningar (rader med roll ur `GET /api/roles` och nivå `r`/`rw`), inloggningsmetoder (bara aktiverade), koppla person, inaktivera/återaktivera. Okända roller visas med varning.
- Visa engångslösenordet en gång efter skapande.
- `stores/auth.js`: håll `roles`, `email`, `mustChangePassword`; ersätt `isAdmin` med `hasRole(role, level)`.
- Vy för att byta eget lösenord.

Member-SPA:
- Omdirigering enligt LOG-9: konton vars enda roll är `member` stannar, övriga går till `/admin`.
- Byt lösenord vid `PASSWORD_CHANGE_REQUIRED`.

### Fas 9: Tester och verifiering

Enhetstester: nivåberäkning (specens tabeller), `UserPolicy` (alla regler), parsning av `roll:nivå`.

Integrationstester (MockMvc, `.with(user(...).authorities(...))`):
- Konto med bara `member:rw` får 403 på `GET /api/persons` och `GET /api/users`, men når `/api/me`. (Kriterium 1)
- `admin:rw` skapar konton med `admin:rw`, `admin:r`, `member:rw` och `audit_master:r`, men får 403 för `superadmin`, för den konfigurerade `superadmin` och för att ändra egna roller. `admin:r` läser listan men ändrar inte. (Kriterium 3)
- `audit.access=[admin:r, audit_master:r]`: `audit_master:rw` och `admin:rw` får båda `read`. (Kriterium 4)
- Ny roll i modulkonfigurationen syns i `GET /api/roles`. (Kriterium 5)
- Ändrade roller och inaktivering avslutar sessioner.
- Start misslyckas utan aktiverad `superadmin`; ingen `admin`/`admin123` i ny databas. (Kriterium 7)
- Konto- och rolländringar loggas även med `audit.enabled=false`. (Kriterium 8)
- `username` följer e-post och kan inte anges separat. (Kriterium 9)
- Konto utan `PASSWORD` i metodlistan kan inte logga in, och tom metodlista nekas. (Kriterium 10)
- `must_change_password` blockerar andra anrop.
- `passwordHash` förekommer aldrig i API-svar.
- Migreringstest: `ADMIN`/`HARBOUR_MASTER`/`MEMBER` → `admin:rw`/`admin:rw`/`member:rw`, alla får `PASSWORD`.

Manuellt: starta med konfigurerad `superadmin`, skapa konton med olika roller, logga in med varje och klicka igenom båda SPA:erna.

## 5. Ordning och beroenden

1. Fas 1 först: allt annat bygger på listformatet för roller och nivåberäkningen.
2. Fas 2 (konfigurerad `superadmin`) direkt efter, eftersom inloggning och tester annars saknar konto.
3. Fas 3 (kontomodell) och fas 4 (`UserPolicy`) kan göras parallellt.
4. Fas 5 (API) kräver fas 3 och 4. Fas 6 (lösenord) görs tillsammans med fas 5.
5. Fas 7 (genomdriva roller) efter fas 5, men först efter inventering av vad member-SPA:n anropar.
6. Fas 8 (frontend) efter fas 5 och 6. Fas 9 löper med varje fas.

Varje fas ska kunna committas för sig och lämna systemet i körbart skick.

## 6. Risker

- **Utelåsning:** utan seedat `admin`-konto är den konfigurerade `superadmin` enda vägen in. Mitigering: startkontroll (ROL-6), tydligt felmeddelande, dokumentation i README.
- **Befintliga databaser:** konton från tiden före ändringen (`admin` m.fl.) migreras till `admin:rw`, och får inte längre `superadmin`. Den konfigurerade `superadmin` behövs för att ändra dem.
- **Klartextlösenord i konfiguration:** rekommendera `{bcrypt}`-hash eller miljövariabel; varna vid klartext.
- **Fas 7 kan bryta medlemsportalen** om den anropar `/api/**`-endpoints utanför `/api/me`. Mitigering: inventera anrop först.
- **Sessioner:** `spring-session-jdbc` behöver ett principal-index för att hitta ett kontos sessioner. Verifiera innan fas 5 anses klar.
- **Rollkatalog från konfiguration:** ett stavfel i en modul-`access` skapar en ny roll tyst. Mitigering: visa katalogen i loggen vid start (NFR-3) och varna för roller som bara förekommer i en modul och saknar tilldelningar.
- **Bortfallna roller:** tilldelning på en roll som tagits ur konfigurationen ligger kvar utan verkan (ROL-3) och måste synas i UI.
- **Ändrade tak:** ändrat tak i konfigurationen kan över natt ge konton lägre nivå. Kräver omstart, men bör nämnas i driftdokumentationen.

## 7. Uppdateringar i övriga dokument när fasen är klar

- `module.md`/`module_spec.md`: ta bort noteringar om `HARBOUR_MASTER` som ersättare (redan borttagna i specen, kvar i koden tills fas 1).
- `iam_spec.md` avsnitt 2 (nuläge): uppdatera till faktiskt läge.
- README: beskriv roller, `roll:nivå`-formatet, hur första `superadmin` konfigureras och att `admin`/`admin123` inte längre skapas.
