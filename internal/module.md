# Plan: modulsystem (aktivera funktioner efter vad kunden betalar för)

## 1. Mål

PortPlanner ska vara uppdelat i **moduler** som kan slås på och av per installation/kund.

- Varje modul konfigureras i `application.yml` och har alltid `enabled: true|false`, plus egna inställningar.
- `base` ingår alltid (båtar, bryggor, tilldelning m.m.).
- Backend exponerar ett REST-API där frontend frågar vilka moduler som är aktiva och anpassar meny, routes och vyer.
- Åtkomst styrs av **rolltilldelningar** på kontot (`admin:rw`, `admin:r`, `member:rw`, ...) och en lista `access` av `roll:nivå` per modul. Nivån i `access` är ett tak. Kontots beräknade nivå i modulen är `none`, `read` eller `write`, och modulen är tillgänglig bara om den är aktiverad **och** nivån är minst `read`. Roller definieras implicit av vad som står i modulernas `access`. Se `iam_spec.md` avsnitt 4 och 5. Menyer, routes och endpoints följer den inloggades beräknade nivå.
- Avstängd modul ger inga endpoints, ingen meny, inga dashboard-badges och inga bakgrundsjobb.
- Undantag: **händelseloggningen skrivs alltid**. Modulen `audit` styr bara visning och hantering av loggen.

## 2. Nuläge (kort)

- Det finns **inget modulbegrepp** i koden. `application.yml` har inga modulinställningar och `SecurityConfig` skiljer bara på roller.
- All funktionalitet är hårdkopplad:
  - Admin-menyn och routes i `portplanner-frontend/src/router/index.js` är statiska.
  - Medlemsmenyn i `portplanner-member/src/App.vue` är statisk (Hem, Min plats, Mina båtar, Kö, Vinterupptagning, Profil).
  - Dashboard-badges (`dashboard/*Badge`) och `AuditService` anropas oavsett kund (vilket nu är önskat beteende för loggningen).
- Hantering av uppläggningsplatser ("Module 10", i koden kallad winter/haul-out) är redan tydligt avgränsad. AI-mail är bara en stub (`MailView.vue`).
- Tidigare statusgenomgång av implementationen: se git-historiken för denna fil.

## 3. Förslag: indelning i moduler

| Modul-id | Namn | Innehåll (befintligt → ny) | Beror på |
|---|---|---|---|
| `base` | Bas (alltid på) | **Befintligt:** personer, båtar (+skrovtyp), bryggor och platser, layout-editor, tilldelning, kö (inkl. erbjudanden), taxor, dashboard (grundbadges), inloggning/roller | – |
| `storage` | Uppläggningsplatser | **Befintligt:** Module 10: säsonger, slots, bokningar, uppställningsyta (SVG), packningsgrupper, prisregler, medlemsbokning | `base` |
| `member-portal` | Medlemssidor | **Befintligt:** `/api/me/**`, `portplanner-member` (min plats, mina båtar, kö, profil, kopplingen användare↔person) | `base` |
| `audit` | Händelseloggning | **Befintligt:** `AuditLog`, `AuditService`, `AuditView` | `base` |
| `reports` | Rapporter | **Ny:** beläggning, intäkter (flyttas från revenue-badges), kölängd/väntetid, exempelvis export till PDF/Excel | `base` |
| `ai-mail` | AI-mailhantering | **Ny (modul 6 i gamla planen):** Gmail-integration, Claude sammanfattar/klassar/utkast till svar | `base` |
| `import-export` | Import/export | **Befintligt, egen modul (ingår inte i bas):** `ImportExportController/Service`, `ImportExportView` | `base` |
| `rentals` | Uthyrning | **Ny:** uthyrningsobjekt med bild och regler, kalenderbokning i medlemsportalen, felrapport vid återlämning | `base` (frivillig koppling till `member-portal`) |

### Förslag på ytterligare moduler (nya, kan säljas separat)

| Modul-id | Idé |
|---|---|
| `billing` | Fakturaunderlag och fakturering utifrån taxor, tilldelningar och bokningar av uppläggningsplats; export till bokföring (t.ex. SIE/CSV) |
| `notifications` | Utskick via e-post/SMS: erbjudande i kö, påminnelser inför upptagning/sjösättning, avgiftspåminnelser |
| `guest-harbour` | Gästhamn: korttidsbokning av lediga platser, gästtaxor, betalning |
| `waitlist-rules` | Utökade köregler: poängsystem, köavgift, årlig bekräftelse av köplats, automatiska platsförslag |
| `maintenance` | Arbetsordrar och underhåll av bryggor, el/vattenposter, kranar och uppställningsplats |
| `metering` | El- och vattenmätning per plats, förbrukningsdebitering |
| `documents` | Dokumentarkiv och avtal per medlem/båt (försäkringsbevis, hyresavtal), digital signering |
| `integrations` | Publikt/partner-API med API-nycklar, webhooks, kartor/AIS |

Rekommendation: implementera modulramverket och flytta först `storage`, `member-portal`, `audit` och `import-export` (finns redan). Därefter `reports` och `ai-mail`. Övriga är backlog.

Beslut från produktägaren:
- **Taxor** ligger kvar i `base` (tolkat från svaret "ja"). `billing` bygger ovanpå taxorna men äger dem inte.
- **`import-export`** är en egen modul (inte i bas).
- **`storage` och `member-portal`** har en frivillig koppling: `storage` beror bara på `base`. Medlemsbokning av uppläggningsplats visas i medlemsportalen bara när båda modulerna är på.
- **Licens:** modulerna styrs enbart via konfiguration (yaml). Ingen licensnyckel i v1.

## 4. Konfiguration i application.yml

Alla moduler ligger under `portplanner.modules.<id>`. `enabled` finns alltid.

```yaml
portplanner:
  modules:
    base:
      enabled: true            # måste vara true, appen vägrar starta annars
      access: [superadmin:rw, admin:rw, member:rw]   # member: egna data
    storage:                   # Uppläggningsplatser (tidigare "winter")
      enabled: false
      access: [superadmin:rw, admin:rw, member:rw]   # medlemmar bokar via medlemsportalen
      # modulspecifikt:
      default-season-start: 09-15
      max-boats-per-packing-group: 6
    member-portal:
      enabled: true
      access: [superadmin:rw, member:rw]
      allow-member-boat-edit: false
    audit:
      enabled: true
      access: [superadmin:r, admin:r]
      retention-days: 730
    reports:
      enabled: false
      access: [superadmin:rw, admin:r]
    ai-mail:
      enabled: false
      access: [superadmin:rw, admin:rw]
      gmail:
        credentials-file: ${GMAIL_CREDENTIALS:}
      claude:
        model: claude-sonnet-5-5
        api-key: ${ANTHROPIC_API_KEY:}
    import-export:
      enabled: true
      access: [superadmin:rw, admin:rw]
    rentals:
      enabled: false
      access: [superadmin:rw, admin:rw, member:rw]   # member: egna bokningar
```

Fillagringen är gemensam och ligger därför utanför modulerna, i `base`:

```yaml
portplanner:
  file-store:
    type: local                          # local | s3 | database
    local:
      path: ${DATA_DIR:./data}/files
```

- Modulinställningar kan överstyras per kund via profilfil (`application-kundx.yml`) eller miljövariabler (`PORTPLANNER_MODULES_STORAGE_ENABLED=true`).
- En modul som saknas i yaml räknas som `enabled: false` (utom `base`).
- `access` är en lista av `roll:nivå` (nivå `r` eller `rw`) och valfri. Saknas den används modulens standardposter ur `PortModule` (samma som i exemplet ovan). Ett konto vars roller inte matchar någon post har `none` (gäller även `superadmin`; `base.access` måste innehålla `superadmin:rw`). Nivån i `access` är ett tak: kontots nivå är `min(kontots nivå, postens nivå)`, och vid flera matchande poster gäller den högsta. `access` kan aldrig ge ett konto mer än vad kontots egen tilldelning och modulens endpoints medger.
- Nya roller skapas genom att skriva dem i en moduls `access`, t.ex. `audit.access: [admin:r, audit_master:r]`. Rollen blir då tillgänglig att tilldela konton i kontohanteringen (`GET /api/roles`).
- Taket kan variera per kund: t.ex. `reports.access: [superadmin:rw, admin:rw]` om administratören även ska få ändra rapportinställningar. Enbart den som konfigurerar systemet kan ändra `access`, inte en `admin` i gränssnittet.
- Hemligheter (API-nycklar, Gmail-uppgifter) läses via miljövariabler, aldrig incheckade.
- Ändring kräver omstart av backend (ingen körtidsväxling i första versionen).

## 5. Backend-design

Paket: `se.portplanner.module`.

1. **`PortModule`-enum/register.** Statisk lista över kända moduler med id, visningsnamn, beskrivning och `dependsOn`. Enums ger kontrollerade id:n; okänd yaml-nyckel loggas som varning.
2. **`ModulesProperties`** (`@ConfigurationProperties("portplanner.modules")`).
   - Bas: `Map<String, ModuleConfig>` med `boolean enabled` och `List<RoleGrant> access` där `RoleGrant` är `(String role, Level level)` och `Level` är `R` eller `RW`. Rollnamn valideras mot mönstret `[a-z][a-z0-9_]*`.
   - Modulspecifika inställningar som egna typade klasser (`StorageProperties`, `AiMailProperties`, ...) bundna till samma prefix och med `@Validated`.
3. **`ModuleService`** (`isEnabled(id)`, `accessLevel(id, role)`, `accessibleModules(role)`, `requireAccess(id, level, authentication)`). Kontots rolltilldelningar hämtas från Spring Security-`Authentication` (en authority per tilldelning, t.ex. `ROLE_admin:rw`, tolkas till `RoleGrant`). Nivån beräknas som `max` över matchande poster av `min(kontots nivå, postens nivå)`. `ModuleService` exponerar även rollkatalogen (alla rollnamn i alla moduler), som kontohanteringen använder.
4. **Start-validering** (fail fast):
   - `base.enabled` måste vara true.
   - Om modul A är aktiv och `dependsOn` B är avstängd stoppas start med tydligt felmeddelande.
   - `access` får bara innehålla poster på formen `roll:nivå` med nivå `r` eller `rw` och giltigt rollnamn; ogiltig post stoppar start. En ny, okänd roll är inte ett fel (den skapar rollen).
   - `base.access` måste innehålla `superadmin:rw`.
   - Varning i loggen om en roll finns i en moduls `access` men saknas i `access` för en modul den beror på (t.ex. `storage` för `member` men `member-portal` av eller utan `member`).
   - Obligatoriska modulinställningar kontrolleras (t.ex. `ai-mail` kräver API-nyckel).
5. **Åtkomstkontroll på HTTP-nivå.**
   - Åtkomstnivån räknas ut automatiskt: `GET` kräver minst `read`, alla andra metoder kräver `write`. Annotationen kan höja kravet för en särskild endpoint: `@RequiresModule(value = "storage", level = WRITE)`.
   - Annotation `@RequiresModule("storage")` på controller-klasser (`WinterSeasonController`, `HaulOutSlotController`, `HaulOutBookingController`, `StorageYardController`, `StoragePackingGroupController`, `StoragePlacementController`, `PricingRuleController`, `AuditController`, `ImportExportController`, `MeController`, ...).
   - En `HandlerInterceptor` läser annotationen och svarar 403 med JSON i två varianter så frontend kan skilja dem åt från "saknas" (404):
     - `{"error":"MODULE_DISABLED","module":"storage"}` när modulen är avstängd.
     - `{"error":"MODULE_ROLE_FORBIDDEN","module":"storage"}` när modulen är på men rollens nivå är `none`, eller `read` vid försök att ändra data.
   - Skäl att inte använda `@ConditionalOnProperty` på controllers/entiteter: Flyway-migreringar och `ddl-auto: validate` kräver att alla tabeller och entiteter alltid finns, och beroenden mellan tjänster (t.ex. `AuditService` som anropas från många services) blir enklare. Databasschemat är alltså **alltid** fullständigt, bara funktionaliteten stängs av.
   - `@ConditionalOnProperty` används däremot för **tunga/externa beans** som inte får starta utan konfiguration: Gmail-klient, Claude-klient, schemalagda jobb i `ai-mail`/`notifications`.
6. **Tvärgående beroenden.**
   - `AuditService.log(...)` skriver **alltid**, oavsett om `audit` är på. Modulen `audit` gatear bara `AuditController` (visning) och `AuditView`. Retention-rensning är ett bakgrundsjobb i modulen och körs bara när den är på.
   - Dashboard: `DashboardBadge` får metoden `String module()` (default `base`). `DashboardService` filtrerar bort badges från avstängda moduler **och** moduler som den inloggades roll inte har åtkomst till.
   - Kopplingar från `base` till `storage` (t.ex. personradering som kontrollerar bokningar) ska gå via en händelse/`Optional`-injektion, aldrig direktreferens till modulens service.
7. **SecurityConfig.** `/api/modules` läggs till som `authenticated()` (redan default). `/api/me/**` är oförändrad men skyddas av `@RequiresModule("member-portal")`. Åtkomstnivåerna per modul ger en grov rollkontroll och täcker därmed delvis den kända bristen att t.ex. `member` kan anropa admin-API:er (för endpoints i annoterade controllers). Kontohantering (IAM) styrs av rollreglerna i `iam_spec.md`, inte av modulnivåer. Finare regler per operation (`@PreAuthorize`) är ett separat spår.

## 6. REST-API

`GET /api/modules` (kräver inloggning, alla roller). Svaret är **personligt**: det speglar den inloggades roll och åtkomstnivå.

```json
{
  "modules": [
    { "id": "base", "name": "Bas", "enabled": true, "accessible": true, "access": "write" },
    { "id": "storage", "name": "Uppläggningsplatser", "enabled": true, "accessible": true, "access": "write",
      "settings": { "defaultSeasonStart": "09-15" } },
    { "id": "reports", "name": "Rapporter", "enabled": true, "accessible": false, "access": "none" }
  ]
}
```

- Alla kända moduler returneras med `enabled`, så UI kan även visa "ej inkluderad".
- `enabled` = modulen ingår i installationen. `accessible` = `enabled` **och** den inloggades beräknade nivå är minst `read`. `access` är den beräknade nivån: `none`, `read` eller `write`. Frontend styr meny och routes med `accessible`, och döljer ändringsknappar när `access` är `read`. Modulernas `access`-listor och andra kontons roller skickas inte med.
- `settings` innehåller endast en **whitelistad delmängd** av inställningarna som frontend behöver. Aldrig hemligheter eller nycklar.
- `GET /api/modules/{id}` för en enskild modul (404 om okänd). Inloggningssvaret från `/api/auth` och `/api/me` behöver inte ändras; frontend hämtar alltid modullistan efter inloggning.
- Dokumenteras i OpenAPI (`OpenApiConfig`), tag "Modules".
- Övervägande: gör en avskalad `GET /api/modules/public` öppen för inloggningssidan (varumärke/vilka portaler finns). Ej nödvändigt i v1.

## 7. Frontend

Båda SPA:erna (`portplanner-frontend`, `portplanner-member`):

1. `src/api/modules.js` → `GET /api/modules`.
2. Pinia-store `stores/modules.js` med `load()`, `isAccessible(id)`, `settings(id)`. Laddas efter lyckad inloggning/`checkAuth()`, före första routing, och **töms och laddas om vid utloggning/ny inloggning** så att en annan roll ger rätt meny.
3. **Menyer:** varje nav-länk får ett modulattribut, t.ex. `v-if="modules.isAccessible('storage')"` i admin-nav och i `portplanner-member/src/App.vue`.
4. **Routes:** `meta: { module: 'storage' }` på `/winter`, `/winter/yard/:seasonId`, `/audit`, `/mail`, `/import-export`, `/haul-out` m.fl. I `router.beforeEach` omdirigeras till `/dashboard` med ett meddelande om modulen är avstängd eller inte tillåten för den inloggades roll.
5. **Dashboard och vyer:** delar som tillhör en modul döljs (t.ex. länkar till uppläggningsplatser på personsidan, revenue-kort om `reports` är av).
6. **Felhantering:** en axios-interceptor fångar `MODULE_DISABLED` och `MODULE_ROLE_FORBIDDEN` och laddar om modullistan.
7. **Medlemsportalen:** om `member-portal` är av ska `/` inte servera medlems-SPA:n (backend returnerar 404 eller en enkel "ej aktiverad"-sida via `SpaController`).

## 8. Genomförande (steg)

1. **Ramverk:** `PortModule`, `ModulesProperties`, `ModuleService`, start-validering, `@RequiresModule` + interceptor, `GET /api/modules`. Lägg `portplanner.modules` i `application.yml` (alla nuvarande moduler `enabled: true` så inget beteende ändras).
2. **Backend-gating:** annotera controllers enligt tabellen i avsnitt 3; låt `AuditService` skriva alltid (endast `AuditController` gateas); filtrera dashboard-badges.
3. **Frontend:** modul-store, meny/route-villkor i båda SPA:erna.
4. **Extrahera befintliga moduler** i ordning: `storage`, `audit`, `member-portal`, `import-export`. Kontrollera att stäng av ⇒ inget läckage (endpoints, meny, badges).
5. **`reports`:** flytta intäktsbadges, lägg till rapportendpoints och vy.
6. **`ai-mail`:** bygg enligt gamla modul 6 bakom `enabled` och `@ConditionalOnProperty`.
7. **Dokumentation:** README-avsnitt "Moduler och konfiguration", exempel-yaml per kundtyp, uppdatera `internal/plan.md`.
8. Backlog-moduler (avsnitt 3) planeras separat.

## 9. Test och verifiering

- **Enhetstester:** `ModuleService` (default av, `base` alltid på), beroendevalidering (start misslyckas när `storage` är på men `base` av eller beroende saknas).
- **Integrationstester (`@SpringBootTest` + `@TestPropertySource`):**
  - `storage.enabled=false` ⇒ `GET /api/winter-seasons` → 403 `MODULE_DISABLED`; `true` ⇒ 200.
  - `GET /api/modules` returnerar rätt `enabled`-flaggor och inga hemliga inställningar.
  - Dashboard-svaret saknar badges från avstängda moduler.
  - Audit av ⇒ nya rader skrivs fortfarande i `audit_log`, men `GET /api/audit` → 403 `MODULE_DISABLED`.
- **Frontend (Vitest, kräver att testuppsättning skapas):** nav döljer länkar; route-guard omdirigerar.
- **Roller (integrationstester med konton som har `SimpleGrantedAuthority("ROLE_admin:rw")` osv.):**
  - `reports.access=[admin:r]`: konto med `admin:rw` `GET` → 200, ändrande anrop → 403 `MODULE_ROLE_FORBIDDEN` (taket). Konto med bara `member:rw` → 403 `MODULE_ROLE_FORBIDDEN`. `superadmin` utan post i `access` → 403 `MODULE_ROLE_FORBIDDEN`.
  - `audit.access=[admin:r, audit_master:r]`: konto med `audit_master:rw` → `GET` 200, ändring 403. Roll utan match → 403.
  - Flera roller: `admin:r` + `member:rw` mot `reports.access=[admin:rw, member:r]` ger `read`.
  - `GET /api/modules` för konto med bara `member:rw` har `accessible=false` för `audit`, `reports`, `ai-mail`, `import-export`, och `true` för `member-portal`.
  - Modul avstängd + matchande roll ⇒ `MODULE_DISABLED` (inte rollfelet).
  - Start misslyckas med ogiltig post i `access`, ogiltig nivå, ogiltigt rollnamn eller `base.access` utan `superadmin:rw`. Start lyckas med en ny okänd roll.
  - `GET /api/roles` innehåller roller från alla moduler, även avstängda.
- **Manuellt:** starta med profilerna `bas-only` och `alla` och klicka igenom admin- och medlems-SPA:erna. Kontrollera Swagger-UI.
- **Regression:** med alla nuvarande moduler `enabled: true` ska systemet bete sig exakt som idag.

## 10. Risker och öppna frågor

- **Databasschema:** modulernas tabeller finns alltid (även avstängda). Data kan bli liggande om en modul stängs av. Det är önskat (ingen dataförlust), men ska dokumenteras.
- **Betalning/licens:** beslutat att moduler styrs enbart via config. Yaml hindrar inte en kund från att slå på en modul själv, vilket accepteras. Kan kompletteras med signerad licensnyckel senare om behovet uppstår.
- **Standardposter per kund:** standardposterna per modul måste beslutas med produktägaren. Ett konto har flera rolltilldelningar; nivån beräknas som högsta av tak-nivåerna över matchande poster (beslutat).
- **Körtidsväxling** utan omstart (admin-UI eller databas) är utanför v1.
- **Auktorisering:** modulgating ersätter inte rollkontroller. Nuvarande brist (inga `@PreAuthorize`, `member` kan anropa admin-API) bör åtgärdas parallellt.

## 11. Modulen `rentals` (uthyrning)

Funktionskraven (`REN-…`) finns i `module_spec.md` avsnitt 2.8. Modulen följer mönstret från `storage`: egen post i `PortModule`, egna controllers med `@RequiresModule("rentals")`, egna tabeller i samma Flyway-schema och egna vyer i båda SPA:erna. De första objekten är en upptagningstrailer och ett antal båtar för dagsegling.

### 11.1 Vägar som övervägdes

**Vad ett uthyrningsobjekt är.** Valt: ett eget `RentalObject` utan koppling till båtregistret. Alternativen var en valfri främmande nyckel till `Boat` för att återanvända modell och längd för klubbens båtar, och en flagga `rentable` på `Boat`. Trailern är ingen båt, beskrivningen och bilden är uthyrningsspecifika, och modulen håller sig för sig själv (DEP-4). En nullbar `boat_id` kan läggas till senare utan att något annat ändras.

**Var reglerna bor.** Valt: egna kolumner på objektet. Alternativet var en generisk regeltabell eller en JSON-kolumn, som ger en ny regel utan migrering. Varje ny regel behöver ändå kod som tillämpar den och ett fält i admin-gränssnittet, så migreringen är den billigaste delen. Kolumner ger dessutom validering vid start och läsbara data i databasen.

**Filerna.** Valt: ett `FileStore`-interface med filsystemet som första implementation. Alternativen var S3 direkt och blobbar i databasen. Installationen kör en EC2-instans med H2 som fil på disk, så uppladdade filer får samma säkerhetskopieringsbehov som databasen redan har. S3 blir intressant först vid fler instanser eller när disken inte ska hålla data.

### 11.2 Datamodell

| Tabell | Innehåll |
|---|---|
| `rental_objects` | `id`, `name`, `description`, `bookable`, reglerna (11.3), `image_file_id` |
| `rental_bookings` | `id`, `object_id`, `person_id`, `start_at`, `end_at`, `status`, `created_at`, `returned_at` |
| `rental_fault_reports` | `id`, `booking_id`, `description`, `reported_at`, `resolved_at`, `resolved_by` |
| `stored_files` | `id`, `store`, `storage_key`, `original_filename`, `content_type`, `size_bytes`, `uploaded_at` |

- `RentalBookingStatus`: `PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `RETURNED`. Objekt utan krav på godkännande skapas direkt som `CONFIRMED`.
- `PENDING`, `CONFIRMED` och `RETURNED` håller tiden. `REJECTED` och `CANCELLED` frigör den. En preliminär bokning håller alltså tiden medan den väntar på godkännande, så först till kvarn gäller. En återlämning före sluttiden frigör inte den återstående tiden.
- Krockkontrollen är ett överlappningsvillkor (`start_at < :end AND end_at > :start`) mot hållande statusar. H2 kör `READ_COMMITTED` och har inget uniktvång för intervall, så en kontroll följd av en insert i samma transaktion hindrar inte två samtidiga bokningar. Tjänsten tar därför ett radlås på objektet (`SELECT ... FOR UPDATE` via `@Lock(PESSIMISTIC_WRITE)`) innan kontrollen. Det gör samtidighetstestet meningsfullt.
- Bokningen kopplas till `Person`, precis som `HaulOutBooking` redan gör.
- Felrapporten hänger på bokningen, så vem och när framgår utan egna kolumner. En administratör som återlämnar åt en medlem (REN-13) skriver rapporten på medlemmens bokning.
- `stored_files` ligger i `base` (paket `se.portplanner.file`) så att andra moduler kan använda samma lager senare. Bara `rentals` skriver i den i v1.
- Nytt schema läggs i `V2__rentals.sql`. Den samlade `V1__baseline.sql` rörs inte.

### 11.3 Regler per objekt

| Kolumn | Betydelse |
|---|---|
| `max_duration_hours` | Längsta tillåtna bokningsintervall |
| `requires_approval` | Bokningen skapas som `PENDING` och måste godkännas |
| `max_days_ahead` | Hur många dagar fram i tiden en bokning får starta |

Reglerna tillämpas i `RentalRuleValidator`, som får objektet och det önskade intervallet. Vid brott kastas det befintliga `ConflictException`, som `GlobalExceptionHandler` redan svarar 409 på; ingen egen exception-typ behövs. `PolicyViolationException` används inte: den ger 403, vilket är reserverat för behörighet och modulfel (FE-5) och skulle blandas ihop med dem i frontend. En ny regel blir en kolumn, en kontroll i validatorn och ett fält i admin-formuläret.

### 11.4 Fillagring

```java
public interface FileStore {
    /** Stores the bytes and returns an opaque key. */
    String put(InputStream data, String contentType, long size);

    InputStream get(String key);

    void delete(String key);
}
```

- `LocalFileStore` (v1) skriver under `portplanner.file-store.local.path`, som standard `${DATA_DIR:./data}/files`. Nyckeln är en UUID och läggs i två nivåer av underkataloger så att en katalog inte fylls. Lagret hör till `base`, inte till `rentals`, så att konfigurationsnyckeln inte behöver döpas om när nästa modul laddar upp filer. En avstängd modul får då inte heller bönan att försvinna.
- `S3FileStore` (senare) använder AWS SDK v2 med bucket och prefix ur konfigurationen och rättigheter på instansens IAM-roll. Bucketen är aldrig publik.
- `DatabaseFileStore` om behovet uppstår: bytes i en egen tabell.
- Implementationen väljs med `@ConditionalOnProperty` på `portplanner.file-store.type`. En är aktiv i taget. Kolumnen `store` i `stored_files` säger vilken implementation som skrev filen, så ett byte av lager kan göras med ett migreringsjobb i stället för en gissning.
- Uppladdning sker som `multipart/form-data`. Tillåtna typer är jpeg, png och webp och gränsen är 5 MB per fil, kontrollerad i modulen. `spring.servlet.multipart.max-file-size` sätts till samma värde, vilket även gäller importfilerna i `import-export` och därför ska nämnas i release-noteringen.
- Bilden lagras i original. Ingen nedskalning eller miniatyrbild i v1; känns medlemmens objektlista trög får det bli en egen ändring (11.9).
- Bilden hämtas med `GET /api/rentals/objects/{id}/image` för admin och `GET /api/me/rentals/objects/{id}/image` för medlem. Båda går genom `@RequiresModule` och strömmar från lagret. Ingen publik URL och inga signerade S3-länkar i v1.

### 11.5 REST-API

Admin (`@RequiresModule("rentals")`):

| Anrop | Gör |
|---|---|
| `GET/POST /api/rentals/objects`, `PUT/DELETE /api/rentals/objects/{id}` | Objekt och deras regler |
| `GET/POST/DELETE /api/rentals/objects/{id}/image` | Visa, ladda upp eller ta bort bild |
| `GET /api/rentals/objects/{id}/bookings?from&to` | Alla bokningar för objektet |
| `POST /api/rentals/bookings` | Boka åt en medlem (`personId`, REN-12). Skapas som `CONFIRMED` oavsett `requires_approval` |
| `POST /api/rentals/bookings/{id}/approve`, `.../reject` | Hantera preliminära bokningar |
| `POST /api/rentals/bookings/{id}/return` | Markera som återlämnad åt en medlem, med valfri felrapport (REN-13) |
| `DELETE /api/rentals/bookings/{id}` | Avboka åt en medlem |
| `GET /api/rentals/fault-reports?resolved=false` | Felrapporter |
| `POST /api/rentals/fault-reports/{id}/resolve` | Markera som åtgärdad |

Medlem (`@RequiresModule("rentals")` och `@RequiresModule("member-portal")`):

| Anrop | Gör |
|---|---|
| `GET /api/me/rentals/objects` | Bokningsbara objekt med beskrivning och regler |
| `GET /api/me/rentals/objects/{id}/image` | Objektets bild |
| `GET /api/me/rentals/objects/{id}/availability?from&to` | Upptagna intervall, utan uppgift om vem som bokat |
| `GET/POST /api/me/rentals/bookings` | Egna bokningar, boka |
| `DELETE /api/me/rentals/bookings/{id}` | Avboka egen bokning före starttid |
| `POST /api/me/rentals/bookings/{id}/return` | Återlämna, med valfri felrapport |

### 11.6 Åtkomst inom modulen

REN-5 kräver att `rentals.access` ger `member` nivån `rw`, och modulåtkomst är i dag den enda kontrollen på HTTP-nivå i systemet (`SecurityConfig` har bara `authenticated()`, och ingen `@PreAuthorize` finns). Utan en spärr skulle en medlem kunna lägga upp objekt, godkänna bokningar, avboka åt andra och läsa vem som bokat vad — det sista i direkt strid med REN-4 och REN-14.

Lösningen hålls så liten som möjligt och gäller bara den här modulen: adressrymderna är redan delade, så `SecurityConfig` får en matchare som kräver en annan roll än `member` för `/api/rentals/**`, medan medlemmens funktioner ligger under `/api/me/rentals/**` och där kontrolleras ägarskap i tjänsten som i `MeService`. Bilden finns därför på båda adresserna. Detta är projektets första rollkontroll per endpoint och ersätter inte IAM-spåret i avsnitt 10; det breddas inte till övriga moduler i den här ändringen.

### 11.7 Frontend

- Admin: `RentalObjectsView.vue` med lista, formulär för objekt och regler, bilduppladdning, bokningar per objekt (med boka åt medlem, godkänn, avslå, återlämna, avboka) och en flik med felrapporter. Route `/rentals` med `meta: { module: 'rentals' }`.
- Medlem: `RentalsView.vue` med objektlista (bild, namn, kort beskrivning), och per objekt en månadskalender där dagar med bokningar är markerade. Klick på en dag ger dygnets timmar där upptagna intervall är utgråade och man väljer start och slut. Egna bokningar listas ovanför med knapp för avboka respektive återlämna.
- Kalendern skrivs för hand med samma beroendefria stil som övriga vyer. Inget kalender- eller datumbibliotek läggs till; båda SPA:erna har i dag fyra beroenden och det ska de fortsätta ha.
- Återlämningen öppnar ett formulär med fritext för fel och brister, som får hoppas över.
- Vitest finns inte i något av SPA:erna. Att sätta upp frontend-tester ligger kvar som eget spår (avsnitt 9) och ingår inte här.

### 11.8 Test

- Enhetstest för `RentalRuleValidator`: för långt intervall, för långt fram i tiden, start i förflutet.
- Krocktest: två bokningar som överlappar ger konflikt; bokning mot avbokad tid går igenom.
- Godkännande: objekt med `requires_approval` ger `PENDING` som håller tiden; avslag frigör den.
- Åtkomst: `rentals.enabled=false` ger 403 `MODULE_DISABLED` på både admin- och medlemsanropen. En medlem kommer inte åt en annan medlems bokning.
- `FileStore`: `LocalFileStore` sparar, läser och tar bort; bildanropet kräver åtkomst till modulen.
- Tillgänglighetsanropet läcker inte vem som bokat.
- Ett konto med bara `member:rw` får 403 på `/api/rentals/**` trots att modulen är tillgänglig för `member`.
- Admin bokar åt en medlem på ett objekt med `requires_approval` och bokningen blir `CONFIRMED` direkt.
- Admin återlämnar en bokning som medlemmen inte återlämnat, med felrapport.
- Radering av en person med bokningar ger 409, inte 500. Radering av ett objekt med bokningar ger 409, och raderas ett objekt utan bokningar försvinner bilden ur lagret.

### 11.9 Radering och beroenden till `base`

- `DELETE /api/rentals/objects/{id}` nekas med 409 om objektet har bokningar, på samma sätt som `PersonService` nekar radering av en person med konto. Att ta ett objekt ur bruk görs normalt med `bookable: false`. Raderas ett objekt utan bokningar tas bilden bort ur lagret och raden ur `stored_files`.
- `rental_bookings.person_id` är en vanlig främmande nyckel utan `ON DELETE`, som övriga tabeller. Utan åtgärd skulle radering av en person som någon gång hyrt ge ett databasfel och 500. `base` får därför inte referera modulen (DEP-4), så `PersonService` frågar en injicerad lista av `PersonDeletionCheck` innan den raderar, och `rentals` bidrar med en implementation som nekar när personen har bokningar. Detta är den första sådana kopplingen i projektet och är det som avsnitt 5.6 föreskriver.

### 11.10 Öppna frågor

- Behörighetskrav per objekt (kurs, körkort) är en kommande regel, inte med i v1.
- Bokningsbara timmar på dygnet (t.ex. 08–18) är ingen regel i v1; en bokning kan läggas på vilken timme som helst.
- Ett objekt har en bild i v1. Flera bilder kräver att `image_file_id` byts mot en kopplingstabell.
- Miniatyrbilder för medlemmens objektlista är inte med. Blir listan trög på mobil är det en egen ändring.
- En bokning som ingen återlämnar städas inte automatiskt. Administratören stänger den (REN-13); något bakgrundsjobb finns inte.
- Avisering vid godkänd eller avslagen bokning kräver `notifications`.
