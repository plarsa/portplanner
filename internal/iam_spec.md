# Funktionsspecifikation: IAM (inloggning, roller och rättigheter)

Status: utkast. IAM ingår i `base` och kan inte stängas av. Rollerna här ersätter rollerna i `module_spec.md` och `module.md` (se avsnitt 13).

Kravnyckelord: **ska** (obligatoriskt), **bör** (rekommenderat), **kan** (valfritt).

## 1. Omfattning

Ingår:
- inloggning med användarnamn eller e-post och lösenord
- glömt lösenord med återställningslänk
- inloggning med Google
- roller `SUPERADMIN`, `ADMIN` och `MEMBER` och en rättighetsmodell med åtkomstnivåer per modul
- hantering av användarkonton, inklusive välkomstbrev
- anmälan från medlemsportalen som admin godkänner

Ingår inte: tvåfaktorsautentisering, inloggning med andra leverantörer än Google, rättigheter per enskilt konto (se 14).

## 2. Nuläge

- Kontot `AppUser` har `username`, `passwordHash`, `role` och valfri koppling till `Person`. Ingen e-postadress finns på kontot.
- Rollerna är `ADMIN`, `HARBOUR_MASTER` och `MEMBER`, en roll per konto.
- Inloggning sker via `POST /api/auth/login` med sessionscookie (session lagras i databasen, 8 timmar). `GET /api/auth/me` visar aktiv session.
- Det finns ingen registrering, ingen lösenordsåterställning, ingen Google-inloggning och inget e-postutskick.
- CSRF-skydd är avstängt och API:et saknar rollkontroll på operationsnivå (`@PreAuthorize` används inte). Detta åtgärdas i kraven nedan.

## 3. Begrepp

| Begrepp | Betydelse |
|---|---|
| Konto | Inloggningsidentitet (`AppUser`) med exakt en roll |
| Person | Medlem eller kontakt i hamnregistret. Ett `MEMBER`-konto är kopplat till exakt en person |
| Roll | Fast del av koden: `SUPERADMIN`, `ADMIN`, `MEMBER` |
| Åtkomstnivå | `none`, `read` eller `write`, satt per modul och roll i konfigurationen |
| Engångstoken | Slumpkod i en länk för att sätta lösenord, bekräfta e-post eller liknande |
| Ansökan | Anmälan om medlemskonto som en person gör i medlemsportalen och som admin godkänner eller avslår |
| Extern identitet | Koppling mellan ett konto och ett Google-konto |

## 4. Roller

| Roll | Syfte |
|---|---|
| `SUPERADMIN` | Systemansvarig. Kan göra allt. Används i första hand för att skapa och peka ut första `ADMIN` |
| `ADMIN` | Arbetar i systemet och administrerar det inom de moduler och den åtkomstnivå som konfigurationen ger rollen |
| `MEMBER` | Loggar in i medlemsportalen och hanterar sina egna data |

| Id | Krav |
|---|---|
| ROL-1 | Ett konto **ska** ha exakt en roll. Rollerna är fasta i koden och kan inte skapas eller ändras av kunden. |
| ROL-2 | `SUPERADMIN` **ska**, precis som `ADMIN` och `MEMBER`, tilldelas åtkomstnivå per modul i konfigurationen. En modul som saknar `SUPERADMIN` i `access` är otillgänglig för `SUPERADMIN`. Kontohantering och IAM-inställningar (avsnitt 4 och RÄT-7) är undantagna och alltid tillgängliga för `SUPERADMIN`, så att systemet inte kan låsas ute. |
| ROL-3 | `ADMIN` **ska** ha den åtkomstnivå (`none`, `read`, `write`) per modul som konfigurationen anger för rollen `ADMIN` (avsnitt 5). |
| ROL-4 | `ADMIN` **ska** kunna skapa, ändra, inaktivera och skicka välkomstbrev till konton med rollerna `ADMIN` och `MEMBER`. |
| ROL-5 | `ADMIN` **ska inte** kunna skapa, ändra, inaktivera eller ta bort `SUPERADMIN`-konton, inte ändra sin egen roll och inte ändra moduler eller åtkomstnivåer. Sådana ändringar görs bara genom konfiguration. |
| ROL-6 | Det **ska** alltid finnas minst ett aktivt `SUPERADMIN`-konto. Att ta bort, inaktivera eller nedgradera det sista **ska** nekas. |
| ROL-7 | Ett nytt `ADMIN`-konto **ska** automatiskt få de moduler och den åtkomstnivå som konfigurationen anger för rollen `ADMIN`. Ingen tilldelning per konto finns. |
| ROL-8 | `MEMBER` **ska** bara komma åt medlemsportalen och endast läsa och ändra sina egna data (avsnitt 8). |
| ROL-9 | Roll och åtkomstnivå **ska** kontrolleras i backend för varje anrop. Att dölja menyval i frontend räcker aldrig. |
| ROL-10 | Ingen **får** ge en annan användare mer behörighet än hen själv har. Alla skapande, rollbyten och inaktiveringar av `ADMIN`- och `SUPERADMIN`-konton **ska** alltid loggas, även när `audit` är avstängd. |

Behörighetsmatris (kontohantering):

| Funktion | SUPERADMIN | ADMIN | MEMBER |
|---|---|---|---|
| Arbeta i moduler (enligt åtkomstnivå i config) | enligt config | enligt config | nej |
| Medlemsportal: egna data | ja (om kopplad till person) | ja (om kopplad till person) | ja |
| Skapa och hantera `MEMBER`-konton | ja | ja | nej |
| Skapa och hantera `ADMIN`-konton | ja | ja | nej |
| Skapa och hantera `SUPERADMIN`-konton | ja | nej | nej |
| Ändra egen roll | nej (någon annan `SUPERADMIN` gör det) | nej | nej |
| Godkänna och avslå ansökningar | ja | ja | nej |
| Skicka välkomstbrev | ja | ja | nej |
| Tvinga lösenordsåterställning för `MEMBER` eller `ADMIN` | ja | ja | nej |
| Tvinga lösenordsåterställning för `SUPERADMIN` | ja | nej | nej |
| Ändra moduler, åtkomstnivåer och IAM-inställningar | endast via konfiguration (kontohantering alltid tillgänglig) | endast via konfiguration | nej |

## 5. Rättighetsmodell

Rättigheter bygger på fasta roller (avsnitt 4) och en åtkomstnivå per modul och roll som sätts i konfigurationen.

| Nivå | Betydelse |
|---|---|
| `none` | Modulen är osynlig och otillgänglig för rollen |
| `read` | Rollen kan läsa (`GET`) i modulen men inte ändra |
| `write` | Rollen kan läsa och ändra (skapa, ändra, ta bort) i modulen |

| Id | Krav |
|---|---|
| RÄT-1 | Åtkomstnivån för varje modul och roll **ska** kunna sättas i konfigurationen under `portplanner.modules.<id>.access`. |
| RÄT-2 | En modul eller roll som saknas i `access` **ska** ha nivå `none`. Det gäller även `SUPERADMIN`. |
| RÄT-3 | Nivån `read` **ska** i backend tillåta endast läsande anrop. Försök att ändra data **ska** nekas med 403 `MODULE_ROLE_FORBIDDEN`. |
| RÄT-4 | Nivån `none` **ska** ge 403 `MODULE_ROLE_FORBIDDEN` (modulen är på men rollen har ingen åtkomst). Är modulen avstängd gäller `MODULE_DISABLED`. |
| RÄT-5 | Åtkomstnivån för `MEMBER` gäller enbart egna data och kan aldrig ge åtkomst till andras data (avsnitt 8). |
| RÄT-6 | Vissa regler **ska** vara fasta i koden och kan inte ändras av konfiguration: minst en `SUPERADMIN` (ROL-6), `ADMIN` kan inte röra `SUPERADMIN` (ROL-5), ägarskap för `MEMBER` (MED-5). |
| RÄT-7 | Kontohantering (IAM) **ska** styras av rollreglerna i avsnitt 4, inte av modulnivåer. Den finns i `base` och är alltid aktiv. |
| RÄT-8 | Vid start **ska** systemet stoppa om `access` innehåller okänd roll eller okänd nivå, och **bör** varna om en modul ger `MEMBER` `write` men saknar stöd för ägarskapskontroll. |
| RÄT-9 | Vid start **ska** systemet stoppa om `base.access` inte ger `SUPERADMIN` nivån `write`, eftersom `base` innehåller kärnfunktioner och `SUPERADMIN` annars saknar arbetsyta. För övriga moduler **bör** systemet varna om `SUPERADMIN` har lägre nivå än `ADMIN`. |

Exempel på konfiguration:

```yaml
portplanner:
  modules:
    base:
      enabled: true
      access: { SUPERADMIN: write, ADMIN: write, MEMBER: write }   # MEMBER: egna data
    storage:
      enabled: true
      access: { SUPERADMIN: write, ADMIN: write, MEMBER: write }
    reports:
      enabled: true
      access: { SUPERADMIN: write, ADMIN: read }
    audit:
      enabled: true
      access: { SUPERADMIN: read, ADMIN: read }
    import-export:
      enabled: false
      access: { SUPERADMIN: write, ADMIN: write }
```

## 6. Inloggning med användarnamn och lösenord

| Id | Krav |
|---|---|
| LOG-1 | Användaren **ska** kunna logga in med användarnamn (eller e-postadress) och lösenord. |
| LOG-2 | Lyckad inloggning **ska** skapa en serversession med cookie (`HttpOnly`, `SameSite=Strict`, `Secure` i produktion). Sessionen är kvar enligt konfigurerad tid. |
| LOG-3 | Sessions-id **ska** bytas vid inloggning för att förhindra session fixation. |
| LOG-4 | Felmeddelandet vid fel inloggning **ska** vara detsamma oavsett om användaren finns eller inte, och svarstiden **bör** vara lika. |
| LOG-5 | Efter 5 misslyckade försök i rad för samma konto inom 15 minuter **ska** kontot låsas tillfälligt (15 minuter) och försök loggas. Gränserna **ska** kunna konfigureras. |
| LOG-6 | Inaktiverade eller låsta konton **ska** inte kunna logga in. |
| LOG-7 | Utloggning **ska** ogiltigförklara sessionen. |
| LOG-8 | `GET /api/auth/me` **ska** returnera användarnamn, e-post, roll och koppling till person för aktiv session, annars 401. |
| LOG-9 | Efter inloggning **ska** användaren skickas rätt: `MEMBER` till medlemsportalen, `ADMIN` och `SUPERADMIN` till adminverktyget. |
| LOG-10 | Lyckade och misslyckade inloggningar, utloggning och lås **ska** alltid loggas i händelseloggen (loggningen är alltid på, `audit` styr bara visningen). |

### Lösenordskrav

| Id | Krav |
|---|---|
| PWD-1 | Lösenord **ska** vara minst 12 tecken. Inga krav på teckenklasser. |
| PWD-2 | Lösenord **bör** nekas om det finns på en lista över vanliga lösenord, eller innehåller användarnamn eller e-post. |
| PWD-3 | Lösenord **ska** lagras som saltad hash (bcrypt eller starkare) och aldrig i klartext eller i loggar. |
| PWD-4 | Användare som är inloggad **ska** kunna byta lösenord genom att ange nuvarande lösenord. Övriga sessioner för kontot avslutas. |

## 7. Glömt lösenord

| Id | Krav |
|---|---|
| RST-1 | Inloggningssidan **ska** ha länken "Glömt lösenord". Användaren anger e-postadress. |
| RST-2 | Svaret **ska** alltid vara detsamma ("Om adressen finns har ett mejl skickats"), oavsett om kontot finns. |
| RST-3 | Om ett aktivt konto med adressen finns **ska** ett mejl skickas med en länk med engångstoken. |
| RST-4 | Token **ska** vara slumpmässig (minst 256 bitar), lagras som hash i databasen, gälla i 60 minuter (konfigurerbart) och bara kunna användas en gång. |
| RST-5 | En ny begäran **ska** ogiltigförklara tidigare oanvända token för kontot. |
| RST-6 | Länken öppnar en sida där användaren anger nytt lösenord (två gånger). Lösenordet valideras enligt PWD-1 och PWD-2. |
| RST-7 | Vid lyckad återställning **ska** token förbrukas, alla befintliga sessioner för kontot avslutas, ett bekräftelsemejl skickas och händelsen loggas. Användaren loggas **inte** in automatiskt. |
| RST-8 | Ogiltig, utgången eller använd token **ska** ge ett tydligt fel och en väg att begära ny länk. |
| RST-9 | Begäran om återställning **ska** begränsas (t.ex. max 5 per timme per adress och per IP) för att förhindra missbruk. |
| RST-10 | Konton som bara använder Google-inloggning och saknar lösenord **ska** kunna sätta ett lösenord via samma flöde. |
| RST-11 | Länken **ska** peka på den konfigurerade publika adressen (`portplanner.iam.public-base-url`), aldrig på värdet i inkommande HTTP-huvuden. |

## 8. Inloggning med Google

| Id | Krav |
|---|---|
| GOO-1 | Google-inloggning **ska** vara valfri och styrs av `portplanner.iam.google.enabled` (standard `false`). |
| GOO-2 | Inloggningssidan **ska** visa knappen "Logga in med Google" bara när funktionen är aktiv. |
| GOO-3 | Flödet **ska** använda OpenID Connect (authorization code med PKCE). Klient-id och hemlighet läses från miljövariabler. |
| GOO-4 | Google-inloggning **ska** bara lyckas för ett **redan skapat** konto vars e-postadress matchar Googles verifierade e-postadress (`email_verified = true`). Konton skapas inte automatiskt. |
| GOO-5 | Vid första lyckade inloggning **ska** kontot kopplas till Googles unika användar-id (`sub`). Därefter identifieras kontot med `sub`, inte med e-post. |
| GOO-6 | Om Googles konto inte matchar något konto **ska** användaren få ett neutralt felmeddelande ("Inget konto hittades, kontakta hamnen") och försöket loggas. |
| GOO-7 | Ett Google-konto **ska** bara kunna kopplas till ett konto. Ett konto **ska** kunna ha lösenord och Google samtidigt. |
| GOO-8 | Användaren **ska** kunna koppla från Google i sin profil, förutsatt att kontot har ett lösenord. |
| GOO-9 | Inloggning med Google ger samma session, roll och behörigheter som lösenordsinloggning. |
| GOO-10 | `state` och `nonce` **ska** användas och valideras. Omdirigeringsadresser **ska** vara vitlistade. |
| GOO-11 | En domänbegränsning **kan** konfigureras (`allowed-domains`) för att t.ex. bara tillåta hamnens egen domän för `ADMIN`. |
| GOO-12 | Ett nytt konto (via ansökan eller välkomstbrev) **ska** kunna välja Google i stället för lösenord när kontot aktiveras, förutsatt att Google-inloggning är aktiv och e-posten matchar. |

## 9. Kontohantering

| Id | Krav |
|---|---|
| KON-1 | Ett konto har: användarnamn (unikt), e-postadress (unik, krävs), roll, status (`INVITED`, `ACTIVE`, `DISABLED`), valfri koppling till person, tidpunkt för senaste inloggning. |
| KON-2 | Konton skapas av behörig roll enligt avsnitt 4 eller genom godkänd ansökan (avsnitt 10). Ingen fri självregistrering. |
| KON-3 | När en admin lägger upp en ny användare **ska** kontot få status `INVITED` och inget lösenord. Administratören **ska inte** se eller sätta användarens lösenord. |
| KON-4 | I kontovyn **ska** finnas en knapp "Skicka välkomstbrev" som skickar ett mejl med länk för att välja lösenord (eller logga in med Google enligt GOO-12). Knappen **ska** kunna användas flera gånger och varje gång ogiltigförklara tidigare oanvända välkomsttoken. |
| KON-5 | Välkomsttoken **ska** följa reglerna för engångstoken i RST-4, men med längre giltighet (förslag 7 dagar, konfigurerbart). Kontot blir `ACTIVE` när lösenord är satt eller Google är kopplat. |
| KON-6 | Ett `MEMBER`-konto **ska** vara kopplat till exakt en person. En person kan ha högst ett konto. |
| KON-7 | Konton **ska** kunna inaktiveras (och återaktiveras) i stället för att raderas. Inaktivering avslutar alla sessioner direkt. |
| KON-8 | Rollbyte **ska** kunna göras av `SUPERADMIN`, och av `ADMIN` mellan `MEMBER` och `ADMIN`, enligt ROL-4 och ROL-5. Rollbyte loggas och pågående sessioner får den nya rollen senast vid nästa anrop. |
| KON-9 | Användaren **ska** kunna ändra sin e-postadress. Ändringen **ska** bekräftas via länk till den nya adressen innan den gäller. |
| KON-10 | Vid radering av en person **ska** kopplat konto inaktiveras eller nekas radering tills kopplingen tas bort. |
| KON-11 | Ett `ADMIN`- eller `SUPERADMIN`-konto **kan** vara kopplat till person (för att kunna använda medlemsportalen) men behöver inte. |

### Vad `MEMBER` får göra med sina egna data

| Id | Krav |
|---|---|
| MED-1 | Läsa sin egen person, sina båtar, sin plats, sin köpost och sina bokningar. |
| MED-2 | Ändra sina kontaktuppgifter och sitt lösenord. |
| MED-3 | Acceptera eller avböja erbjuden plats **endast på sin egen köpost**. |
| MED-4 | Boka och avboka uppläggningsplats för sina egna båtar (om modulen är aktiv). |
| MED-5 | **Aldrig** läsa eller ändra andra personers data, även om identifierare gissas i anrop. Backend **ska** kontrollera ägarskap på varje anrop och svara 403 eller 404. |

## 10. Anmälan från medlemsportalen

En person som inte har konto kan göra en anmälan i medlemsportalen. Admin granskar och godkänner eller avslår.

| Id | Krav |
|---|---|
| ANM-1 | Medlemsportalens inloggningssida **ska** ha länken "Anmäl dig som medlem" till en öppen sida som inte kräver inloggning. |
| ANM-2 | Formuläret **ska** samla in namn, e-postadress, telefonnummer, valfria uppgifter om båt (modell, längd, bredd) och ett medgivande till behandling av personuppgifter. Inget lösenord anges. |
| ANM-3 | Efter inskickning **ska** ett mejl med bekräftelselänk (engångstoken, giltig i 24 timmar) skickas till adressen. Ansökan syns för admin först när adressen är bekräftad. |
| ANM-4 | Svaret på inskickning **ska** vara detsamma oavsett om e-postadressen redan har konto eller ansökan, så att befintliga konton inte röjs. Om adressen redan har konto **bör** ett mejl skickas dit om att konto finns. |
| ANM-5 | Samma e-postadress **ska** bara kunna ha en öppen (obehandlad) ansökan åt gången. |
| ANM-6 | Antalet anmälningar **ska** begränsas per IP-adress och per e-postadress (t.ex. 5 per timme per IP, 3 per dygn per adress). Gränserna **ska** kunna konfigureras. |
| ANM-7 | Bekräftade ansökningar **ska** listas för `ADMIN` och `SUPERADMIN` med status `PENDING`, `APPROVED` eller `REJECTED`, tidpunkt och uppgifterna från formuläret. |
| ANM-8 | Vid godkännande **ska** admin välja att koppla ansökan till en befintlig person eller skapa en ny person av uppgifterna. Systemet **ska** föreslå befintliga personer med samma e-post eller telefonnummer. |
| ANM-9 | Godkännande **ska** i ett steg skapa ett `MEMBER`-konto med status `INVITED` kopplat till personen och **automatiskt** skicka välkomstbrev enligt KON-4. |
| ANM-10 | Vid avslag **ska** admin kunna ange ett valfritt meddelande. Sökanden **ska** få ett mejl om avslaget (med meddelandet om det finns). |
| ANM-11 | Båtuppgifter i ansökan **bör** kunna föras över till nya båtposter vid godkännande. |
| ANM-12 | Avslagna och obekräftade ansökningar **ska** gallras efter en konfigurerad tid (förslag 90 dagar, obekräftade 7 dagar). |
| ANM-13 | Godkännande, avslag och gallring **ska** loggas. |
| ANM-14 | Bekräftelsetoken **ska** följa reglerna för engångstoken i RST-4. |

## 11. Sessioner och säkerhet

| Id | Krav |
|---|---|
| SÄK-1 | Alla API-anrop utom inloggning, glömt lösenord, återställning, välkomstlänk, Google-flödet, anmälan och statiska filer **ska** kräva giltig session. |
| SÄK-2 | CSRF-skydd **ska** aktiveras för sessionsbaserade anrop som ändrar data. |
| SÄK-3 | All trafik **ska** gå över HTTPS i produktion. |
| SÄK-4 | Sessionens längd (idag 8 timmar) och tid för inaktivitet **ska** kunna konfigureras. |
| SÄK-5 | Användaren **bör** kunna se sina aktiva sessioner och avsluta dem. |
| SÄK-6 | Lösenord, token och sessions-id **får inte** finnas i loggar. |
| SÄK-7 | Inloggnings-, återställnings- och anmälningsanrop **ska** ha begränsning av antal anrop per IP. |
| SÄK-8 | H2-konsolen (`/h2-console`) och Swagger **ska** vara avstängda eller kräva `SUPERADMIN` i produktion. |

## 12. API (översikt)

| Anrop | Åtkomst | Syfte |
|---|---|---|
| `POST /api/auth/login` | öppen | Logga in med lösenord |
| `POST /api/auth/logout` | inloggad | Logga ut |
| `GET /api/auth/me` | inloggad | Aktiv användare och roll |
| `POST /api/auth/forgot-password` | öppen | Begär återställningslänk |
| `GET /api/auth/reset-password/{token}` | öppen | Kontrollera att token är giltig |
| `POST /api/auth/reset-password` | öppen | Sätt nytt lösenord med token (även för välkomstlänk) |
| `POST /api/auth/change-password` | inloggad | Byt lösenord |
| `GET /api/auth/google/start` | öppen (om aktiv) | Starta Google-inloggning |
| `GET /api/auth/google/callback` | öppen (om aktiv) | Google skickar tillbaka användaren hit |
| `DELETE /api/auth/google` | inloggad | Koppla från Google |
| `POST /api/applications` | öppen | Skicka in ansökan om medlemskonto |
| `GET /api/applications/confirm/{token}` | öppen | Bekräfta e-postadress för ansökan |
| `GET /api/applications` | `ADMIN`, `SUPERADMIN` | Lista ansökningar |
| `POST /api/applications/{id}/approve` | `ADMIN`, `SUPERADMIN` | Godkänn, skapar konto och skickar välkomstbrev |
| `POST /api/applications/{id}/reject` | `ADMIN`, `SUPERADMIN` | Avslå med valfritt meddelande |
| `GET/POST/PUT /api/users` | `ADMIN`, `SUPERADMIN` enligt avsnitt 4 | Kontohantering |
| `POST /api/users/{id}/send-welcome` | `ADMIN`, `SUPERADMIN` | Skicka (eller skicka om) välkomstbrev |
| `POST /api/users/{id}/disable`, `/enable` | `ADMIN`, `SUPERADMIN` enligt avsnitt 4 | Inaktivera eller återaktivera konto |
| `GET/PUT /api/me/profile` | inloggad | Egen profil |

## 13. Konsekvenser för övriga dokument

- **Roller:** `HARBOUR_MASTER` tas bort. Befintliga konton med rollen migreras till `ADMIN`. `SUPERADMIN` är ny.
- **Moduler:** `roles: [ ... ]` i `module_spec.md` och `module.md` ersätts av `access: { ROLL: nivå }` (avsnitt 5). `GET /api/modules` returnerar den inloggades nivå per modul. `SUPERADMIN` tilldelas nivå per modul som övriga roller (RÄT-2, RÄT-9).
- **Migrering:** konton saknar e-post idag. En migrering måste lägga till e-post, status, låsning samt tabeller för engångstoken, extern identitet och ansökningar. Befintliga konton måste få e-post innan glömt lösenord och välkomstbrev kan användas.
- **E-post:** systemet saknar e-postutskick. Kräver SMTP-konfiguration (värd, användare, avsändare) som miljövariabler.
- **Modulsystemet:** `ai-mail` (Gmail) och Google-inloggning använder båda Google men är oberoende och har separata OAuth-klienter och inställningar.

## 14. Konfiguration (förslag)

```yaml
portplanner:
  iam:
    public-base-url: ${PUBLIC_BASE_URL:http://localhost:8080}
    session-timeout: 8h
    password-min-length: 12
    lockout:
      max-attempts: 5
      duration: 15m
    reset-token-ttl: 60m
    welcome-token-ttl: 7d
    application:
      confirm-ttl: 24h
      rate-limit-per-ip-per-hour: 5
      rate-limit-per-email-per-day: 3
      retention-rejected: 90d
      retention-unconfirmed: 7d
    google:
      enabled: false
      client-id: ${GOOGLE_CLIENT_ID:}
      client-secret: ${GOOGLE_CLIENT_SECRET:}
      allowed-domains: []      # tom lista = alla
    mail:
      host: ${SMTP_HOST:}
      from: ${SMTP_FROM:}
```

Vid start **ska** systemet stoppa om `google.enabled=true` saknar klient-id eller hemlighet, och varna om `public-base-url` pekar på `localhost` i produktionsprofil.

Rättigheter per modul och roll konfigureras enligt exemplet i avsnitt 5.

## 15. Godkännandekriterier (exempel)

1. Ett `MEMBER`-konto kan logga in och nå medlemsportalen men får 403 på `GET /api/persons`.
2. En `MEMBER` som anropar `POST /api/queue/{id}/accept-offer` på någon annans köpost nekas.
3. En `ADMIN` kan skapa ett `ADMIN`- och ett `MEMBER`-konto men får 403 när hen försöker skapa, ändra eller inaktivera ett `SUPERADMIN`-konto, eller ändra sin egen roll.
4. Ett nytt `ADMIN`-konto ser exakt de moduler som konfigurationen ger rollen `ADMIN`, och kan inte ändra det.
5. Med `reports: { ADMIN: read }` kan en `ADMIN` läsa rapporter men får 403 `MODULE_ROLE_FORBIDDEN` vid försök att ändra.
6. En modul som saknar `SUPERADMIN` i `access` är otillgänglig för `SUPERADMIN`, men kontohantering fungerar fortfarande. Systemet startar inte om `base.access` saknar `SUPERADMIN: write`.
7. Det går inte att nedgradera eller ta bort det sista `SUPERADMIN`-kontot.
8. Skapande av `ADMIN`-konto loggas även när `audit` är avstängd.
9. "Glömt lösenord" ger samma svar för kända och okända adresser. Länken fungerar en gång och efter 60 minuter inte alls. Efter återställning är gamla sessioner ogiltiga.
10. Google-inloggning med en e-post som inte har något konto skapar inget konto och ger neutralt fel.
11. Efter fem felaktiga inloggningar låses kontot i 15 minuter, även med rätt lösenord.
12. Med `google.enabled=false` visas ingen Google-knapp och `/api/auth/google/start` svarar 404.
13. En ansökan syns för admin först efter e-postbekräftelse. Samma adress kan inte ha två öppna ansökningar. Efter gränsen för antal anmälningar avvisas nya med 429.
14. När admin godkänner en ansökan skapas ett `MEMBER`-konto med status `INVITED` och ett välkomstmejl skickas automatiskt. Sökanden väljer lösenord via länken och kontot blir `ACTIVE`.
15. När admin avslår med meddelande får sökanden ett mejl med meddelandet.
16. Admin lägger upp en ny användare och trycker "Skicka välkomstbrev". Ett andra tryck ogiltigförklarar den första länken.

## 16. Öppna punkter

- Ska välkomstlänken gälla 7 dagar (KON-5)? Ska gallringstiderna för ansökningar vara 90 och 7 dagar (ANM-12)?
- Sätter konfigurationen åtkomstnivån per modul och roll (beslutat), men ska `SUPERADMIN` senare kunna snäva in ett enskilt `ADMIN`-konto? Nu: nej.
- Ska `ADMIN` och `SUPERADMIN` kunna använda medlemsportalen om de är kopplade till en person (KON-11)?
- Ska inloggning vara med användarnamn, e-post eller båda (LOG-1)?
- Behövs tvåfaktorsautentisering för `ADMIN` och `SUPERADMIN`?
- Ska en ansökan kunna avvisas automatiskt om personen redan är medlem med konto (ANM-4)?
- Gränser för spärr, lösenordslängd och antal anmälningar är förslag.
