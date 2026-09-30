# Plan: modulsystem (aktivera funktioner efter vad kunden betalar för)

## 1. Mål

PortPlanner ska vara uppdelat i **moduler** som kan slås på och av per installation/kund.

- Varje modul konfigureras i `application.yml` och har alltid `enabled: true|false`, plus egna inställningar.
- `base` ingår alltid (båtar, bryggor, tilldelning m.m.).
- Backend exponerar ett REST-API där frontend frågar vilka moduler som är aktiva och anpassar meny, routes och vyer.
- Åtkomst styrs per **roll** (`SUPERADMIN`, `ADMIN`, `MEMBER`) med en **åtkomstnivå** per modul: `none`, `read` eller `write`. En modul är tillgänglig för en användare bara om den är aktiverad **och** rollens nivå är minst `read`. `SUPERADMIN` tilldelas nivå per modul som övriga roller. Se `iam_spec.md` avsnitt 5. Menyer, routes och endpoints följer den inloggades roll och nivå.
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
      access: { SUPERADMIN: write, ADMIN: write, MEMBER: write }   # MEMBER: egna data
    storage:                   # Uppläggningsplatser (tidigare "winter")
      enabled: false
      access: { SUPERADMIN: write, ADMIN: write, MEMBER: write }   # medlemmar bokar via medlemsportalen
      # modulspecifikt:
      default-season-start: 09-15
      max-boats-per-packing-group: 6
    member-portal:
      enabled: true
      access: { SUPERADMIN: write, MEMBER: write }
      allow-member-boat-edit: false
    audit:
      enabled: true
      access: { SUPERADMIN: read, ADMIN: read }
      retention-days: 730
    reports:
      enabled: false
      access: { SUPERADMIN: write, ADMIN: read }
    ai-mail:
      enabled: false
      access: { SUPERADMIN: write, ADMIN: write }
      gmail:
        credentials-file: ${GMAIL_CREDENTIALS:}
      claude:
        model: claude-sonnet-5-5
        api-key: ${ANTHROPIC_API_KEY:}
    import-export:
      enabled: true
      access: { SUPERADMIN: write, ADMIN: write }
```

- Modulinställningar kan överstyras per kund via profilfil (`application-kundx.yml`) eller miljövariabler (`PORTPLANNER_MODULES_STORAGE_ENABLED=true`).
- En modul som saknas i yaml räknas som `enabled: false` (utom `base`).
- `access` är valfritt. Saknas det används modulens standardnivåer ur `PortModule` (samma som i exemplet ovan). En roll som saknas i `access` har `none` (gäller även `SUPERADMIN`; `base.access` måste ge `SUPERADMIN` `write`). `access` kan aldrig ge en roll mer än vad modulens endpoints medger.
- Nivåerna kan variera per kund: t.ex. `reports.access: { SUPERADMIN: write, ADMIN: write }` om administratören även ska få ändra rapportinställningar. Enbart den som konfigurerar systemet kan ändra nivåer, inte en `ADMIN` i gränssnittet.
- Hemligheter (API-nycklar, Gmail-uppgifter) läses via miljövariabler, aldrig incheckade.
- Ändring kräver omstart av backend (ingen körtidsväxling i första versionen).

## 5. Backend-design

Paket: `se.portplanner.module`.

1. **`PortModule`-enum/register.** Statisk lista över kända moduler med id, visningsnamn, beskrivning och `dependsOn`. Enums ger kontrollerade id:n; okänd yaml-nyckel loggas som varning.
2. **`ModulesProperties`** (`@ConfigurationProperties("portplanner.modules")`).
   - Bas: `Map<String, ModuleConfig>` med `boolean enabled` och `Map<Role, AccessLevel> access` (`NONE`, `READ`, `WRITE`).
   - Modulspecifika inställningar som egna typade klasser (`StorageProperties`, `AiMailProperties`, ...) bundna till samma prefix och med `@Validated`.
3. **`ModuleService`** (`isEnabled(id)`, `accessLevel(id, role)`, `accessibleModules(role)`, `requireAccess(id, level, authentication)`). Rollen hämtas från Spring Security-`Authentication` (befintlig `Role`-enum, `ROLE_`-prefix).
4. **Start-validering** (fail fast):
   - `base.enabled` måste vara true.
   - Om modul A är aktiv och `dependsOn` B är avstängd stoppas start med tydligt felmeddelande.
   - `access` får bara innehålla kända roller och nivåer (`none`, `read`, `write`); okänd roll eller nivå stoppar start.
   - Varning i loggen om en modul är tillgänglig för en roll som saknar åtkomst till en modul den beror på (t.ex. `storage` för `MEMBER` men `member-portal` av eller utan `MEMBER`-nivå).
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
7. **SecurityConfig.** `/api/modules` läggs till som `authenticated()` (redan default). `/api/me/**` är oförändrad men skyddas av `@RequiresModule("member-portal")`. Åtkomstnivåerna per modul ger en grov rollkontroll och täcker därmed delvis den kända bristen att t.ex. MEMBER kan anropa admin-API:er (för endpoints i annoterade controllers). Kontohantering (IAM) styrs av rollreglerna i `iam_spec.md`, inte av modulnivåer. Finare regler per operation (`@PreAuthorize`) är ett separat spår.

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
- `enabled` = modulen ingår i installationen. `accessible` = `enabled` **och** den inloggades nivå är minst `read`. `access` är den inloggades nivå: `none`, `read` eller `write`. Frontend styr meny och routes med `accessible`, och döljer ändringsknappar när `access` är `read`. Övriga rollers nivåer skickas inte med.
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
- **Roller (integrationstester med `@WithMockUser(roles = ...)`):**
  - `reports.access={ADMIN: read}`: ADMIN `GET` → 200, ADMIN ändrande anrop → 403 `MODULE_ROLE_FORBIDDEN`, MEMBER → 403 `MODULE_ROLE_FORBIDDEN`. SUPERADMIN utan post i `access` → 403 `MODULE_ROLE_FORBIDDEN`.
  - `GET /api/modules` som MEMBER har `accessible=false` för `audit`, `reports`, `ai-mail`, `import-export`, och `true` för `member-portal`.
  - Modul avstängd + tillåten roll ⇒ `MODULE_DISABLED` (inte rollfelet).
  - Start misslyckas med okänt rollnamn eller okänd nivå i `access`.
- **Manuellt:** starta med profilerna `bas-only` och `alla` och klicka igenom admin- och medlems-SPA:erna. Kontrollera Swagger-UI.
- **Regression:** med alla nuvarande moduler `enabled: true` ska systemet bete sig exakt som idag.

## 10. Risker och öppna frågor

- **Databasschema:** modulernas tabeller finns alltid (även avstängda). Data kan bli liggande om en modul stängs av. Det är önskat (ingen dataförlust), men ska dokumenteras.
- **Betalning/licens:** beslutat att moduler styrs enbart via config. Yaml hindrar inte en kund från att slå på en modul själv, vilket accepteras. Kan kompletteras med signerad licensnyckel senare om behovet uppstår.
- **Nivåer per kund:** standardnivåer per modul måste beslutas med produktägaren. Användare har en roll var; om flera roller per användare införs måste nivån beräknas som den högsta av rollernas nivåer.
- **Körtidsväxling** utan omstart (admin-UI eller databas) är utanför v1.
- **Auktorisering:** modulgating ersätter inte rollkontroller. Nuvarande brist (inga `@PreAuthorize`, MEMBER kan anropa admin-API) bör åtgärdas parallellt.
