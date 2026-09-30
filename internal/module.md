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
