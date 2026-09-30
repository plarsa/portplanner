# Funktionsspecifikation: IAM (inloggning, roller och rättigheter)

Status: utkast. IAM ingår i `base` och kan inte stängas av. Rollerna här ersätter rollerna i `module_spec.md` och `module.md` (se avsnitt 13).

Kravnyckelord: **ska** (obligatoriskt), **bör** (rekommenderat), **kan** (valfritt).

## 1. Omfattning

Ingår:
- inloggning med användarnamn eller e-post och lösenord
- glömt lösenord med återställningslänk
- inloggning med Google
- roller som lista per konto, där varje roll har en nivå (`admin:rw`, `admin:r`, `member:rw`), och en rättighetsmodell där modulernas konfiguration anger vilka roller och nivåer som ger åtkomst
- inloggningsmetoder som lista per konto (idag lösenord, senare t.ex. Google och BankID)
- en konfigurerad `superadmin` som lever i applikationskonfigurationen och inte i databasen
- hantering av användarkonton, inklusive välkomstbrev
- anmälan från medlemsportalen som admin godkänner

Ingår inte: tvåfaktorsautentisering, inloggning med andra leverantörer än Google (BankID och andra läggs till senare som nya inloggningsmetoder, se LOG-11), rättigheter per enskild modul och konto (nivån sätts per roll, se avsnitt 5).

## 2. Nuläge

- Kontot `AppUser` har `username`, `passwordHash`, `role` och valfri koppling till `Person`. Ingen e-postadress finns på kontot.
- Rollerna är `ADMIN`, `HARBOUR_MASTER` och `MEMBER`, en roll per konto, och `username` är ett fritt namn. Kontot `admin`/`admin123` skapas av `DataInitializer`. Inloggningsmetod finns inte som begrepp (bara lösenord).
- Inloggning sker via `POST /api/auth/login` med sessionscookie (session lagras i databasen, 8 timmar). `GET /api/auth/me` visar aktiv session.
- Det finns ingen registrering, ingen lösenordsåterställning, ingen Google-inloggning och inget e-postutskick.
- CSRF-skydd är avstängt och API:et saknar rollkontroll på operationsnivå (`@PreAuthorize` används inte). Detta åtgärdas i kraven nedan.

## 3. Begrepp

| Begrepp | Betydelse |
|---|---|
| Konto | Inloggningsidentitet (`AppUser`) i databasen. Har en lista av rolltilldelningar och en lista av tillåtna inloggningsmetoder. `username` är alltid kontots e-postadress |
| Person | Medlem eller kontakt i hamnregistret. Ett konto med rollen `member` är kopplat till exakt en person |
| Roll | Ett namn (`admin`, `member`, `audit_master`, ...) som **definieras implicit** av att det förekommer i en moduls `access`-konfiguration. Nya roller tillkommer utan kodändring |
| Nivå | `r` (läsa) eller `rw` (läsa och ändra). Frånvaro av roll betyder ingen åtkomst |
| Rolltilldelning | En roll och en nivå på ett konto, skrivet `roll:nivå`, t.ex. `admin:rw` |
| Inloggningsmetod | Ett sätt att bevisa identitet: `PASSWORD` idag, senare t.ex. `GOOGLE` och `BANKID`. Kontot har en lista av tillåtna metoder |
| Konfigurerad superadmin | Systemägarens konto som definieras i applikationskonfigurationen och aldrig lagras i databasen |
| Engångstoken | Slumpkod i en länk för att sätta lösenord, bekräfta e-post eller liknande |
| Ansökan | Anmälan om medlemskonto som en person gör i medlemsportalen och som admin godkänner eller avslår |
| Extern identitet | Koppling mellan ett konto och en extern leverantör, t.ex. ett Google-konto |

## 4. Roller

Ett konto har en **lista** av rolltilldelningar, t.ex. `admin:rw, member:rw` eller `admin:r`. Rollnamn definieras implicit av modulernas konfiguration (avsnitt 5). Tre namn är reserverade och har fasta regler i koden:

| Roll | Fast betydelse |
|---|---|
| `superadmin` | Systemansvarig. Finns bara som konfigurerat konto (ROL-11), aldrig i databasen. Hanterar konton och kan tilldela alla roller |
| `admin` | Hanterar konton när nivån är `rw` (ROL-4). Modulåtkomst styrs som för alla andra roller av konfigurationen |
| `member` | Medlemsportalen och endast egna data (ROL-8) |

Alla andra roller (t.ex. `audit_master`) har ingen fast betydelse i koden. De ger bara åtkomst till de moduler vars `access` nämner dem.

| Id | Krav |
|---|---|
| ROL-1 | Ett konto **ska** ha en lista av noll eller flera rolltilldelningar `roll:nivå`. Nivå är `r` eller `rw`. Samma roll får förekomma högst en gång per konto. Om samma roll ges som både `admin:r` och `admin:rw` **ska** den högsta gälla och listan normaliseras till `admin:rw`. |
| ROL-2 | Vilka roller som finns **ska** härledas från `access`-konfigurationen i alla moduler (aktiverade eller inte). En administratör kan bara tilldela roller som finns i den sammanställda listan (rollkatalogen), utöver de reserverade `admin` och `member`, som alltid finns. `superadmin` kan aldrig tilldelas i databasen. |
| ROL-3 | En rolltilldelning i ett konto vars roll inte längre finns i någon konfiguration **ska** ligga kvar men sakna verkan, och visas i kontovyn som "okänd roll". Den får inte stoppa start. |
| ROL-4 | Konton med `admin:rw` **ska** kunna skapa, ändra, inaktivera och skicka välkomstbrev till konton, och ändra rolltilldelningar enligt ROL-5 och ROL-10. `admin:r` ger läsrätt till kontolistan men inga ändringar. |
| ROL-5 | En `admin` **ska inte** kunna tilldela eller ta bort `superadmin`, inte ändra eller inaktivera den konfigurerade `superadmin`, inte ändra sina egna rolltilldelningar och inte ändra moduler eller åtkomst. Sådant görs bara genom konfiguration. |
| ROL-6 | Systemet **ska** vid start stoppa om ingen aktiverad `superadmin` är konfigurerad (ROL-11), så att systemet inte kan låsas ute. |
| ROL-7 | Ett nytt konto **ska** bara få de rolltilldelningar den som skapar kontot anger. Modulåtkomsten följer av rollerna och modulernas konfiguration. Ingen separat tilldelning per modul finns. |
| ROL-8 | Ett konto som bara har rollen `member` **ska** bara komma åt medlemsportalen och endast läsa och ändra sina egna data (avsnitt 9). |
| ROL-9 | Roll och nivå **ska** kontrolleras i backend för varje anrop. Att dölja menyval i frontend räcker aldrig. |
| ROL-10 | Ingen **får** ge en annan användare rollen `superadmin`, och ingen **får** tilldela sig själv fler roller eller högre nivåer. Alla skapande, ändringar av rolltilldelningar och inaktiveringar av konton **ska** alltid loggas, även när `audit` är avstängd. |
| ROL-11 | Konfigurerade `superadmin`-konton **ska** definieras under `portplanner.iam.superadmins` med `username`, `password` och `enabled` (avsnitt 14). De lagras aldrig i databasen, kan inte ändras eller tas bort i gränssnittet, och `enabled: false` stänger av kontot. Inget standardkonto får skapas av databasmigreringar eller kod. |
| ROL-12 | En konfigurerad `superadmin` **ska** ha nivån `rw` i modulerna som anger `superadmin` i sin `access`. Modulen `base` **ska** alltid ange `superadmin:rw`, annars stoppar start (RÄT-9). |

Behörighetsmatris (kontohantering):

| Funktion | superadmin | admin:rw | admin:r | member |
|---|---|---|---|---|
| Arbeta i moduler | enligt config | enligt config | enligt config | nej |
| Medlemsportal: egna data | ja (om kopplad till person, se öppna punkter) | ja (om kopplade till person) | ja (om kopplade till person) | ja |
| Se kontolistan | ja | ja | ja | nej |
| Skapa och hantera konton | ja | ja | nej | nej |
| Tilldela roller (utom `superadmin`) | ja | ja | nej | nej |
| Tilldela eller ta bort `superadmin` | nej (endast via konfiguration) | nej | nej | nej |
| Ändra egna rolltilldelningar | nej | nej | nej | nej |
| Godkänna och avslå ansökningar | ja | ja | nej | nej |
| Skicka välkomstbrev | ja | ja | nej | nej |
| Tvinga lösenordsåterställning | ja | ja (utom för konfigurerad `superadmin`) | nej | nej |
| Ändra moduler, åtkomst och IAM-inställningar | endast via konfiguration | endast via konfiguration | nej | nej |

## 5. Rättighetsmodell

Rättigheter bygger på rolltilldelningar på kontot (avsnitt 4) och en lista av roller med tak-nivå per modul i konfigurationen.

| Nivå | Betydelse |
|---|---|
| ingen | Modulen är osynlig och otillgänglig för kontot |
| `r` | Kontot kan läsa (`GET`) i modulen men inte ändra |
| `rw` | Kontot kan läsa och ändra (skapa, ändra, ta bort) i modulen |

**Beräkning av åtkomst.** För varje post `roll:nivå` i modulens `access` som kontot har en tilldelning för är modulnivån den **lägsta** av kontots nivå och konfigurationens nivå (`r` < `rw`). Om flera poster matchar gäller den **högsta** av dessa. Konfigurationen är alltså ett tak. Saknas matchning är åtkomsten ingen.

Exempel: `audit.access: [superadmin:r, admin:r, audit_master:r]`.

| Kontots roller | Nivå i `audit` |
|---|---|
| `admin:r` | `r` |
| `admin:rw` | `r` (taket är `r`) |
| `audit_master:rw` | `r` |
| `member:rw` | ingen |
| `admin:r`, `audit_master:r` | `r` |

Exempel: `reports.access: [admin:rw, member:r]` och kontot har `admin:r, member:rw` ger `r` (`min(r,rw)` för `admin`, `min(rw,r)` för `member`; högsta är `r`).

| Id | Krav |
|---|---|
| RÄT-1 | Åtkomsten för varje modul **ska** kunna sättas i konfigurationen under `portplanner.modules.<id>.access` som en lista av `roll:nivå`. |
| RÄT-2 | En roll som saknas i `access` **ska** ge ingen åtkomst till modulen. Det gäller även `superadmin`. |
| RÄT-3 | Nivån `r` **ska** i backend tillåta endast läsande anrop. Försök att ändra data **ska** nekas med 403 `MODULE_ROLE_FORBIDDEN`. |
| RÄT-4 | Ingen åtkomst **ska** ge 403 `MODULE_ROLE_FORBIDDEN` (modulen är på men kontot saknar matchande roll). Är modulen avstängd gäller `MODULE_DISABLED`. |
| RÄT-5 | Åtkomsten för `member` gäller enbart egna data och kan aldrig ge åtkomst till andras data (avsnitt 9). |
| RÄT-6 | Vissa regler **ska** vara fasta i koden och kan inte ändras av konfiguration: minst en aktiverad konfigurerad `superadmin` (ROL-6), `superadmin` kan inte tilldelas i databasen (ROL-5), ägarskap för `member` (MED-5). |
| RÄT-7 | Kontohantering (IAM) **ska** styras av rollreglerna i avsnitt 4 (`superadmin` eller `admin:rw`), inte av modulåtkomst. Den finns i `base` och är alltid aktiv. |
| RÄT-8 | Vid start **ska** systemet stoppa om `access` innehåller ogiltigt format eller ogiltig nivå (annat än `r` eller `rw`), eller ogiltigt rollnamn (tillåtna tecken: små bokstäver, siffror och understreck, börjar med bokstav), och **bör** varna om en modul ger `member` `rw` men saknar stöd för ägarskapskontroll. |
| RÄT-9 | Vid start **ska** systemet stoppa om `base.access` inte innehåller `superadmin:rw`. För övriga moduler **bör** systemet varna om `superadmin` har lägre nivå än `admin`. |
| RÄT-10 | Åtkomstberäkningen **ska** göras om vid varje anrop (eller cacheas kort) utifrån kontots aktuella tilldelningar, så att en ändring av roller gäller senast vid nästa anrop. |

Exempel på konfiguration:

```yaml
portplanner:
  modules:
    base:
      enabled: true
      access: [superadmin:rw, admin:rw, member:rw]   # member: egna data
    storage:
      enabled: true
      access: [superadmin:rw, admin:rw, member:rw]
    reports:
      enabled: true
      access: [superadmin:rw, admin:r]
    audit:
      enabled: true
      access: [superadmin:r, admin:r, audit_master:r]   # audit_master finns nu som roll att tilldela
    import-export:
      enabled: false
      access: [superadmin:rw, admin:rw]
```

## 6. Inloggning med användarnamn och lösenord

| Id | Krav |
|---|---|
| LOG-1 | Användaren **ska** kunna logga in med e-postadress och lösenord. Eftersom `username` alltid är e-postadressen (KON-1) finns ingen separat inloggning med användarnamn för databaskonton. Konfigurerade `superadmin` loggar in med sitt konfigurerade `username`. |
| LOG-2 | Lyckad inloggning **ska** skapa en serversession med cookie (`HttpOnly`, `SameSite=Strict`, `Secure` i produktion). Sessionen är kvar enligt konfigurerad tid. |
| LOG-3 | Sessions-id **ska** bytas vid inloggning för att förhindra session fixation. |
| LOG-4 | Felmeddelandet vid fel inloggning **ska** vara detsamma oavsett om användaren finns eller inte, och svarstiden **bör** vara lika. |
| LOG-5 | Efter 5 misslyckade försök i rad för samma konto inom 15 minuter **ska** kontot låsas tillfälligt (15 minuter) och försök loggas. Gränserna **ska** kunna konfigureras. |
| LOG-6 | Inaktiverade eller låsta konton **ska** inte kunna logga in. |
| LOG-7 | Utloggning **ska** ogiltigförklara sessionen. |
| LOG-8 | `GET /api/auth/me` **ska** returnera användarnamn, e-post, rolltilldelningar (lista av `roll:nivå`), tillåtna inloggningsmetoder och koppling till person för aktiv session, annars 401. |
| LOG-9 | Efter inloggning **ska** användaren skickas rätt: konton vars enda roll är `member` till medlemsportalen, övriga (som har någon annan roll, inklusive `superadmin`) till adminverktyget. |
| LOG-10 | Lyckade och misslyckade inloggningar, utloggning och lås **ska** alltid loggas i händelseloggen (loggningen är alltid på, `audit` styr bara visningen). |

### Inloggningsmetoder

| Id | Krav |
|---|---|
| LOG-11 | Ett konto **ska** ha en lista av tillåtna inloggningsmetoder. Kända metoder är `PASSWORD` (idag), `GOOGLE` (avsnitt 8) och `BANKID` (framtida). Nya metoder **ska** kunna läggas till utan att ändra befintliga konton eller tabeller (metoderna lagras som text/uppslagstabell, inte som kolumn per metod). |
| LOG-12 | Inloggning med en metod **ska** nekas (samma neutrala fel som LOG-4) om metoden inte finns i kontots lista, även om användaren kan bevisa sin identitet med den. |
| LOG-13 | Listan är en **policy** som en administratör anger per konto. Den är oberoende av vilka metoder kontot faktiskt har kopplade (t.ex. lösenord satt, Google-`sub`). En metod kan bara väljas om den är aktiverad i systemet (`portplanner.iam.<metod>.enabled`). |
| LOG-14 | Listan **får inte** vara tom för ett aktivt konto. Standard för nya konton är `PASSWORD`. |
| LOG-15 | Ändring av kontots metodlista **ska** loggas och avsluta kontots sessioner om en metod tas bort. |
| LOG-16 | Konfigurerade `superadmin` använder alltid `PASSWORD` med lösenordet i konfigurationen. |

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
| GOO-9 | Inloggning med Google ger samma session, roller och behörigheter som lösenordsinloggning. Metoden `GOOGLE` **ska** finnas i kontots lista av inloggningsmetoder (LOG-12). |
| GOO-10 | `state` och `nonce` **ska** användas och valideras. Omdirigeringsadresser **ska** vara vitlistade. |
| GOO-11 | En domänbegränsning **kan** konfigureras (`allowed-domains`) för att t.ex. bara tillåta hamnens egen domän för konton med rollen `admin`. |
| GOO-12 | Ett nytt konto (via ansökan eller välkomstbrev) **ska** kunna välja Google i stället för lösenord när kontot aktiveras, förutsatt att Google-inloggning är aktiv och e-posten matchar. |

## 9. Kontohantering

| Id | Krav |
|---|---|
| KON-1 | Ett konto har: `username` (unikt), e-postadress (unik, krävs), rolltilldelningar (lista av `roll:nivå`), tillåtna inloggningsmetoder (lista), status (`INVITED`, `ACTIVE`, `DISABLED`), valfri koppling till person, tidpunkt för senaste inloggning. **`username` fylls alltid automatiskt med e-postadressen** och kan inte anges eller ändras separat. Ändras e-postadressen (KON-9) ändras `username` samtidigt. |
| KON-2 | Konton skapas av behörig roll enligt avsnitt 4 eller genom godkänd ansökan (avsnitt 10). Ingen fri självregistrering. |
| KON-3 | När en admin lägger upp en ny användare **ska** kontot få status `INVITED` och inget lösenord. Administratören **ska inte** se eller sätta användarens lösenord. |
| KON-4 | I kontovyn **ska** finnas en knapp "Skicka välkomstbrev" som skickar ett mejl med länk för att välja lösenord (eller logga in med Google enligt GOO-12). Knappen **ska** kunna användas flera gånger och varje gång ogiltigförklara tidigare oanvända välkomsttoken. |
| KON-5 | Välkomsttoken **ska** följa reglerna för engångstoken i RST-4, men med längre giltighet (förslag 7 dagar, konfigurerbart). Kontot blir `ACTIVE` när lösenord är satt eller Google är kopplat. |
| KON-6 | Ett konto med rollen `member` **ska** vara kopplat till exakt en person. En person kan ha högst ett konto. |
| KON-7 | Konton **ska** kunna inaktiveras (och återaktiveras) i stället för att raderas. Inaktivering avslutar alla sessioner direkt. |
| KON-8 | Ändring av rolltilldelningar (lägga till, ta bort, byta nivå) **ska** kunna göras av `superadmin` och `admin:rw` enligt ROL-4, ROL-5 och ROL-10. Ändringen loggas och gäller senast vid nästa anrop (RÄT-10). |
| KON-9 | Användaren **ska** kunna ändra sin e-postadress. Ändringen **ska** bekräftas via länk till den nya adressen innan den gäller, och då ändras även `username` (KON-1). |
| KON-10 | Vid radering av en person **ska** kopplat konto inaktiveras eller nekas radering tills kopplingen tas bort. |
| KON-11 | Ett konto som har andra roller än `member` **kan** vara kopplat till person (för att kunna använda medlemsportalen) men behöver inte. |
| KON-12 | Konton **ska** kunna skapas utan att ange användarnamn: `username` sätts från e-postadressen. Konfigurerad `superadmin` (ROL-11) ingår inte i kontohanteringen och visas som skrivskyddad rad i kontolistan. |

### Vad `member` får göra med sina egna data

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
| ANM-7 | Bekräftade ansökningar **ska** listas för `superadmin` och `admin:rw` med status `PENDING`, `APPROVED` eller `REJECTED`, tidpunkt och uppgifterna från formuläret. |
| ANM-8 | Vid godkännande **ska** admin välja att koppla ansökan till en befintlig person eller skapa en ny person av uppgifterna. Systemet **ska** föreslå befintliga personer med samma e-post eller telefonnummer. |
| ANM-9 | Godkännande **ska** i ett steg skapa ett konto med rollen `member:rw`, status `INVITED` och inloggningsmetod `PASSWORD`, kopplat till personen, och **automatiskt** skicka välkomstbrev enligt KON-4. |
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
| SÄK-8 | H2-konsolen (`/h2-console`) och Swagger **ska** vara avstängda eller kräva `superadmin` i produktion. |

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
| `GET /api/applications` | `superadmin`, `admin:rw` | Lista ansökningar |
| `POST /api/applications/{id}/approve` | `superadmin`, `admin:rw` | Godkänn, skapar konto och skickar välkomstbrev |
| `POST /api/applications/{id}/reject` | `superadmin`, `admin:rw` | Avslå med valfritt meddelande |
| `GET /api/users` | `superadmin`, `admin` (`r` eller `rw`) | Lista konton (inklusive konfigurerad `superadmin`, skrivskyddad) |
| `POST/PUT /api/users` | `superadmin`, `admin:rw` enligt avsnitt 4 | Skapa och ändra konto, rolltilldelningar och inloggningsmetoder |
| `GET /api/roles` | `superadmin`, `admin:rw` | Rollkatalogen: roller härledda från modulernas `access` som går att tilldela |
| `POST /api/users/{id}/send-welcome` | `superadmin`, `admin:rw` | Skicka (eller skicka om) välkomstbrev |
| `POST /api/users/{id}/disable`, `/enable` | `superadmin`, `admin:rw` enligt avsnitt 4 | Inaktivera eller återaktivera konto |
| `GET/PUT /api/me/profile` | inloggad | Egen profil |

## 13. Konsekvenser för övriga dokument

- **Roller:** ett enda roll-fält byts mot en lista av rolltilldelningar. `HARBOUR_MASTER` tas bort. Befintliga konton migreras: `ADMIN` och `HARBOUR_MASTER` blir `admin:rw`, `MEMBER` blir `member:rw`. `superadmin` finns bara i konfigurationen.
- **Standardkonto:** `admin`/`admin123` (skapas i dag av `DataInitializer`) **tas bort**. Inga konton får skapas av migreringar eller startkod. Första `superadmin` definieras i konfigurationen (ROL-11).
- **Moduler:** `access` i `module_spec.md` och `module.md` blir en lista av `roll:nivå` (avsnitt 5) i stället för en tabell från roll till nivå. `GET /api/modules` returnerar den inloggades beräknade nivå per modul (`none`, `read` eller `write`). `superadmin` styrs som övriga roller (RÄT-2, RÄT-9).
- **Migrering:** konton saknar e-post idag och `username` är ett fritt namn. Migreringen måste lägga till e-post, status, rolltilldelningar (tabell), inloggningsmetoder (tabell), låsning samt tabeller för engångstoken, extern identitet och ansökningar. Befintliga konton behåller sitt `username` tills e-post har fyllts i; därefter sätts `username` = e-post (KON-1). Glömt lösenord och välkomstbrev kan inte användas förrän e-post finns.
- **E-post:** systemet saknar e-postutskick. Kräver SMTP-konfiguration (värd, användare, avsändare) som miljövariabler.
- **Modulsystemet:** `ai-mail` (Gmail) och Google-inloggning använder båda Google men är oberoende och har separata OAuth-klienter och inställningar.

## 14. Konfiguration (förslag)

```yaml
portplanner:
  iam:
    public-base-url: ${PUBLIC_BASE_URL:http://localhost:8080}
    superadmins:               # ROL-11: lever bara här, aldrig i databasen
      - username: ${SUPERADMIN_USERNAME:}
        password: ${SUPERADMIN_PASSWORD:}   # bcrypt-hash med {bcrypt}-prefix rekommenderas
        enabled: true
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

Vid start **ska** systemet stoppa om `google.enabled=true` saknar klient-id eller hemlighet, om ingen aktiverad `superadmin` med användarnamn och lösenord är konfigurerad (ROL-6), och varna om `public-base-url` pekar på `localhost` i produktionsprofil eller om ett `superadmin`-lösenord är angivet i klartext.

Åtkomst per modul och roll konfigureras enligt exemplet i avsnitt 5. Inloggningsmetoder aktiveras per metod (`portplanner.iam.google.enabled` m.fl.).

## 15. Godkännandekriterier (exempel)

1. Ett konto med bara `member:rw` kan logga in och nå medlemsportalen men får 403 på `GET /api/persons`.
2. En `member` som anropar `POST /api/queue/{id}/accept-offer` på någon annans köpost nekas.
3. Ett konto med `admin:rw` kan skapa konton med `admin:rw`, `admin:r`, `member:rw` och `audit_master:r`, men får 403 när hen försöker tilldela `superadmin`, ändra den konfigurerade `superadmin` eller ändra sina egna rolltilldelningar. Ett konto med `admin:r` får läsa kontolistan men inte ändra.
4. Med `audit.access: [admin:r, audit_master:r]` ser ett konto med `audit_master:rw` modulen med nivå `r` och får 403 `MODULE_ROLE_FORBIDDEN` vid ändring. Ett konto med `admin:rw` får också bara `r`.
5. En ny roll som läggs till i en moduls `access` visas i `GET /api/roles` efter omstart och kan tilldelas i kontovyn. Tas den bort ur konfigurationen ligger tilldelningen kvar utan verkan och visas som okänd roll.
6. En modul som saknar `superadmin` i `access` är otillgänglig för `superadmin`, men kontohantering fungerar fortfarande. Systemet startar inte om `base.access` saknar `superadmin:rw`.
7. Systemet startar inte utan minst en aktiverad konfigurerad `superadmin`. `enabled: false` på den enda konfigurerade `superadmin` stoppar start. Det finns inget `admin`/`admin123`-konto i en ny databas.
8. Skapande av konto och ändring av rolltilldelningar loggas även när `audit` är avstängd.
9. Ett konto skapat med e-post `anna@hamn.se` får `username = anna@hamn.se`. Ändras och bekräftas ny e-post ändras `username` med. Något annat användarnamn kan inte anges.
10. Ett konto vars metodlista saknar `PASSWORD` kan inte logga in med lösenord och får samma neutrala fel som vid fel lösenord. Ett konto kan inte spara en tom metodlista.
11. Ändras rolltilldelningar för ett inloggat konto gäller de senast vid nästa anrop.
12. "Glömt lösenord" ger samma svar för kända och okända adresser. Länken fungerar en gång och efter 60 minuter inte alls. Efter återställning är gamla sessioner ogiltiga.
13. Google-inloggning med en e-post som inte har något konto skapar inget konto och ger neutralt fel.
14. Efter fem felaktiga inloggningar låses kontot i 15 minuter, även med rätt lösenord.
15. Med `google.enabled=false` visas ingen Google-knapp och `/api/auth/google/start` svarar 404.
16. En ansökan syns för admin först efter e-postbekräftelse. Samma adress kan inte ha två öppna ansökningar. Efter gränsen för antal anmälningar avvisas nya med 429.
17. När admin godkänner en ansökan skapas ett konto med `member:rw`, status `INVITED` och ett välkomstmejl skickas automatiskt. Sökanden väljer lösenord via länken och kontot blir `ACTIVE`.
18. När admin avslår med meddelande får sökanden ett mejl med meddelandet.
19. Admin lägger upp en ny användare och trycker "Skicka välkomstbrev". Ett andra tryck ogiltigförklarar den första länken.

## 16. Öppna punkter

- **Rollkatalog och avstängda moduler:** rollkatalogen (ROL-2) hämtas nu från alla moduler, även avstängda. Alternativ: bara aktiverade moduler.
- **Vem får tilldela vilka roller:** `admin:rw` kan tilldela alla roller i katalogen utom `superadmin`, även sådana hen själv inte har (t.ex. `audit_master`). Ska en `admin` bara få tilldela roller hen själv har, eller är nuvarande regel (ROL-10) rätt?
- **Vem hanterar konton:** kontohantering är knuten till de reserverade rollnamnen `superadmin` och `admin:rw`. Behövs en separat roll (t.ex. `user_manager`) för det?
- **Lösenord för konfigurerad `superadmin`:** bcrypt-hash i konfigurationen rekommenderas. Ska klartext nekas vid start i produktionsprofil eller bara varnas för?
- **Migrering av befintliga konton:** de har inget e-postfält. Vad ska `username` bli tills e-post är ifyllt, och vem fyller i det?
- **Nivåer:** bara `r` och `rw`. Behövs ytterligare nivåer (t.ex. bara skapa, inte ta bort)?
- Ska `admin` och `superadmin` kunna använda medlemsportalen om de är kopplade till en person (KON-11)?
- Ska inloggningsmetodlistan (LOG-11) kunna avgränsas per roll, t.ex. tvinga `admin` till Google?
- Ska en ansökan kunna avvisas automatiskt om personen redan är medlem med konto (ANM-4)?
- Behövs tvåfaktorsautentisering för `admin` och `superadmin`?
- Ska välkomstlänken gälla 7 dagar (KON-5)? Ska gallringstiderna för ansökningar vara 90 och 7 dagar (ANM-12)?
- Gränser för spärr, lösenordslängd och antal anmälningar är förslag.
