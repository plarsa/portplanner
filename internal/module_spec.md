# Funktionsspecifikation: modulsystem

Roller, rolltilldelningar och åtkomstnivåer definieras i `iam_spec.md`. Status: utkast. Planen för genomförandet finns i `module.md`. Denna fil beskriver **vad** systemet ska göra, inte hur det byggs.

Krav har id (`MOD-…`) så att de kan refereras i tester och ärenden. Nyckelorden **ska**, **bör** och **kan** anger obligatoriskt, rekommenderat respektive valfritt.

## 1. Begrepp

| Begrepp | Betydelse |
|---|---|
| Modul | En avgränsad uppsättning funktioner som kan aktiveras separat |
| Aktiverad (`enabled`) | Modulen ingår i installationen enligt konfigurationen |
| Tillgänglig (`accessible`) | Modulen är aktiverad **och** den inloggade användarens beräknade nivå är `read` eller `write` |
| Roll | Ett namn (`admin`, `member`, `audit_master`, ...) som definieras implicit av att det står i en moduls `access`. Ett konto har en lista av rolltilldelningar `roll:nivå`, t.ex. `admin:rw` (se `iam_spec.md`) |
| Åtkomstnivå | Modulens `access` är en lista av `roll:nivå` där nivå är `r` eller `rw` och fungerar som tak. Kontots beräknade nivå i modulen är `none`, `read` (`r`) eller `write` (`rw`), se avsnitt 3 |
| Installation | En körande instans av systemet för en kund, med egen konfiguration |

## 2. Moduler och deras funktioner

### 2.1 `base` – Bas (alltid aktiv)

| Id | Funktion |
|---|---|
| BAS-1 | Hantera personer (skapa, ändra, sök, ta bort) |
| BAS-2 | Hantera båtar med skrovtyp och koppla dem till ägare |
| BAS-3 | Hantera bryggor och platser, inklusive layout-editor |
| BAS-4 | Tilldela och avsluta tilldelning av plats till båt |
| BAS-5 | Hantera kö: köposter, platsförslag, erbjudande, accept och avböj |
| BAS-6 | Hantera taxor |
| BAS-7 | Dashboard med grundläggande nyckeltal (antal personer, båtar, bryggor, lediga och upptagna platser, kölängd) |
| BAS-8 | Inloggning, utloggning, roller och användarhantering |

### 2.2 `storage` – Uppläggningsplatser

| Id | Funktion |
|---|---|
| STO-1 | Hantera säsonger och tidsluckor (slots) för upptagning och sjösättning |
| STO-2 | Hantera bokningar av uppläggningsplats: skapa, bekräfta, avboka |
| STO-3 | Hantera uppställningsytor, packningsgrupper och placering av båtar (visuell yta med dra och släpp) |
| STO-4 | Validera placering (kollision, packning) och ordning för sjösättning |
| STO-5 | Prisregler och prisberäkning för uppläggning |
| STO-6 | Medlemsbokning av uppläggningsplats i medlemsportalen (kräver även `member-portal`) |

### 2.3 `member-portal` – Medlemssidor

| Id | Funktion |
|---|---|
| MEM-1 | Medlemmen ser sin egen plats, sina båtar och sin köposition |
| MEM-2 | Medlemmen accepterar eller avböjer erbjuden plats |
| MEM-3 | Medlemmen ser och ändrar sin profil |
| MEM-4 | Koppling mellan användarkonto och person |

### 2.4 `audit` – Händelseloggning

Loggningen är en grundfunktion som **alltid** är på och inte kan stängas av. Modulen `audit` styr bara **visning och hantering** av loggen (sök, filtrera, rensa).

| Id | Funktion |
|---|---|
| AUD-1 | Ändringar i systemet loggas alltid med tidpunkt, användare, typ, objekt och beskrivning, oavsett om modulen `audit` är aktiverad (ingår i `base`) |
| AUD-2 | Administratör kan se, söka och filtrera i loggen (kräver att modulen är aktiverad och åtkomstnivå `read`) |
| AUD-3 | Loggar äldre än `retention-days` kan rensas. Rensning är en funktion i modulen och sker inte när modulen är avstängd, så loggen växer då tills modulen aktiveras |

### 2.5 `reports` – Rapporter (ny)

| Id | Funktion |
|---|---|
| REP-1 | Beläggningsrapport per brygga och period |
| REP-2 | Intäktsrapport per taxa och period (intäktsmåtten flyttas hit från dashboarden) |
| REP-3 | Köstatistik: kölängd och väntetid |
| REP-4 | Export av rapporter till CSV och PDF |

### 2.6 `ai-mail` – AI-mailhantering (ny)

| Id | Funktion |
|---|---|
| AIM-1 | Hämta inkommande e-post från konfigurerad Gmail-låda |
| AIM-2 | AI sammanfattar och klassificerar tråd (t.ex. köfråga, felanmälan, uppsägning) |
| AIM-3 | AI föreslår svarsutkast. Ett utkast skickas aldrig utan att en användare godkänner det |
| AIM-4 | Koppla tråd till person, båt eller köpost |

### 2.7 `import-export` – Import och export

| Id | Funktion |
|---|---|
| IMP-1 | Importera bryggor med platser, personer och båtar från fil |
| IMP-2 | Exportera samma data till fil |
| IMP-3 | Importen validerar och rapporterar fel per rad utan att spara delvis |

### 2.8 `rentals` – Uthyrning (ny)

Uthyrning av hamnens eller klubbens egna objekt, t.ex. upptagningstrailer och båtar för dagsegling. Modulen hanterar inga kostnader: ingen avgift, ingen deposition och ingen debitering vid sen återlämning. Medlemsbokning kräver även `member-portal` (frivillig koppling, DEP-3). Genomförandet beskrivs i `module.md` avsnitt 11.

| Id | Funktion |
|---|---|
| REN-1 | Hantera uthyrningsobjekt: namn, beskrivning och om objektet är öppet för bokning |
| REN-2 | Ladda upp och byta objektets bild. Bilden **ska** hämtas genom modulens API och **får inte** ligga på en publik adress |
| REN-3 | Sätta regler per objekt: längsta bokningslängd i timmar, om bokning kräver godkännande, och hur många dagar fram i tiden objektet får bokas. Fler regler tillkommer |
| REN-4 | Medlemmen väljer objekt ur en lista med bild och beskrivning och ser objektets kalender med upptagna tider. Kalendern visar att en tid är upptagen, aldrig vem som bokat den |
| REN-5 | Medlemmen bokar ett tidsintervall på timnivå. Bokningen **ska** avvisas om den bryter mot objektets regler eller överlappar en befintlig bokning |
| REN-6 | Kräver objektet godkännande skapas bokningen som preliminär och håller tiden tills en administratör godkänner eller avslår den |
| REN-7 | Medlemmen **kan** avboka sin egen bokning fram till starttiden. Efter starttiden kan medlemmen inte ändra bokningen |
| REN-8 | Vid återlämning **kan** låntagaren rapportera fel och brister på objektet. Rapporten spärrar inte objektet och tiden efter fortsätter vara bokningsbar |
| REN-9 | Administratör ser felrapporter på objektet och i en samlad lista, och **kan** markera en rapport som åtgärdad |
| REN-10 | Administratör ser och hanterar alla bokningar för ett objekt och **kan** avboka åt en medlem |
| REN-11 | Uppladdade filer **ska** lagras genom ett utbytbart lager så att installationen kan välja filsystem, S3 eller databas utan att modulens API ändras |
| REN-12 | Administratör **kan** boka ett objekt åt en medlem. En sådan bokning kräver inget godkännande |
| REN-13 | Administratör **kan** markera en bokning som återlämnad och skriva felrapporten åt medlemmen, när medlemmen inte gör det själv |
| REN-14 | `member` **ska** bara komma åt sina egna bokningar och medlemsfunktionerna, aldrig objekthantering, godkännande, andras bokningar eller felrapportlistan, även om `rentals.access` ger `member` nivån `rw` (vilket REN-5 kräver) |

### 2.9 Föreslagna framtida moduler (ej specificerade)

`billing`, `notifications`, `guest-harbour`, `waitlist-rules`, `maintenance`, `metering`, `documents`, `integrations`. Se `module.md`. Varje ska specificeras i eget avsnitt innan utveckling börjar.

## 3. Åtkomst per modul och roll

Varje modul har en lista `access` av poster `roll:nivå`, där nivå är `r` (läsa) eller `rw` (läsa och ändra). Ett konto har en lista av rolltilldelningar av samma form (se `iam_spec.md` avsnitt 4).

**Beräkning.** För varje post i modulens `access` som kontot har en tilldelning för är nivån den lägsta av kontots och postens nivå. Om flera poster matchar gäller den högsta. Ingen matchning ger `none`. Konfigurationen är alltså ett tak.

| `audit.access` | Kontots roller | Nivå |
|---|---|---|
| `[superadmin:r, admin:r, audit_master:r]` | `admin:r` | `read` |
| samma | `admin:rw` | `read` |
| samma | `audit_master:rw` | `read` |
| samma | `member:rw` | `none` |
| `[admin:rw]` | `admin:rw`, `member:r` | `write` |

Standardposter per modul (förslag, ska bekräftas av produktägaren). De används när `access` saknas i konfigurationen. `superadmin` är en vanlig post här och styrs som övriga roller.

| Modul | Standard `access` |
|---|---|
| `base` | `[superadmin:rw, admin:rw, member:rw]` (`member`: endast egna data via medlemsportalen) |
| `storage` | `[superadmin:rw, admin:rw, member:rw]` (STO-6, `member`: endast egna data) |
| `member-portal` | `[superadmin:rw, member:rw]` (egna data) |
| `audit` | `[superadmin:r, admin:r]` |
| `reports` | `[superadmin:rw, admin:r]` |
| `ai-mail` | `[superadmin:rw, admin:rw]` |
| `import-export` | `[superadmin:rw, admin:rw]` |
| `rentals` | `[superadmin:rw, admin:rw, member:rw]` (REN-5, `member`: endast egna bokningar) |

## 4. Krav på konfiguration

| Id | Krav |
|---|---|
| MOD-1 | Alla moduler konfigureras under `portplanner.modules.<id>` i `application.yml`. |
| MOD-2 | Varje modul **ska** kunna konfigureras med `enabled: true` eller `false`. |
| MOD-3 | En modul som saknas i konfigurationen **ska** räknas som avstängd, förutom `base`. |
| MOD-4 | `base` **ska** alltid vara aktiv. Om `base.enabled` är `false` **ska** systemet vägra starta med ett tydligt felmeddelande. |
| MOD-5 | Varje modul **kan** ha egna inställningar under sitt id. Modulspecifika inställningar valideras vid start. |
| MOD-6 | Varje modul **kan** ha `access` som en lista av `roll:nivå` (nivå `r` eller `rw`). Om `access` saknas används modulens standardposter (avsnitt 3). Roller som nämns i någon moduls `access` blir tillgängliga att tilldela konton (rollkatalogen i `iam_spec.md`, ROL-2). |
| MOD-7 | Ett konto vars roller inte matchar någon post i `access` **ska** ha `none`. Det gäller även `superadmin`. `base.access` **ska** innehålla `superadmin:rw`, annars stoppar start. Ogiltigt format, ogiltig nivå (annat än `r` eller `rw`) eller ogiltigt rollnamn **ska** stoppa start. En ny roll i `access` är inte ett fel. |
| MOD-8 | `access` **ska** bara kunna begränsa åtkomst inom det modulen stödjer. Den ger aldrig ett konto fler rättigheter än kontots egen tilldelning (taket i avsnitt 3). Modulernas `access` kan bara ändras via konfiguration, aldrig av en `admin` i gränssnittet. |
| MOD-9 | Inställningar **ska** kunna överstyras per kund med profilfil eller miljövariabler. |
| MOD-10 | Hemligheter (API-nycklar, inloggningsuppgifter) **ska** läsas från miljövariabler och **får inte** checkas in. |
| MOD-11 | Ändring av moduler kräver omstart. Körtidsändring stöds inte. |
| MOD-12 | Modulen styrs enbart av konfiguration. Ingen licensnyckel krävs. |

## 5. Krav på beroenden

| Id | Krav |
|---|---|
| DEP-1 | Varje modul deklarerar vilka moduler den beror på. Alla moduler utom `base` beror på `base`. |
| DEP-2 | Om en modul är aktiv och en modul den beror på är avstängd **ska** systemet vägra starta och ange vilken. |
| DEP-3 | Koppling mellan moduler som inte är ett hårt beroende (t.ex. `storage` och `member-portal`) **ska** vara frivillig: funktioner som kräver båda visas bara när båda är tillgängliga. |
| DEP-4 | Kod i `base` **får inte** ha direkt beroende till valfria moduler. Samverkan sker via händelser eller valfri injektion. |
| DEP-5 | Systemet **bör** logga en varning om en modul har en roll i `access` som saknas i `access` för en modul den beror på. |

## 6. Krav på beteende när en modul inte är tillgänglig

| Id | Krav |
|---|---|
| BEH-1 | Modulens API-endpoints **ska** svara 403 med `{"error":"MODULE_DISABLED","module":"<id>"}` när modulen är avstängd. |
| BEH-2 | Modulens API-endpoints **ska** svara 403 med `{"error":"MODULE_ROLE_FORBIDDEN","module":"<id>"}` när modulen är aktiv men användarens beräknade nivå är `none`, eller `read` vid försök att ändra data. Är modulen avstängd gäller BEH-1. |
| BEH-3 | En obehörig anropare **ska** inte få veta något om modulens data eller inställningar. |
| BEH-4 | Modulens menyval, sidor och dashboard-delar **ska** döljas för användare där modulen inte är tillgänglig. |
| BEH-5 | Om en användare öppnar en modulsida direkt via adress **ska** hen skickas till startsidan med ett förklarande meddelande. |
| BEH-6 | Bakgrundsjobb, mailhämtning och externa klienter i en avstängd modul **ska** inte starta. |
| BEH-7 | Händelseloggning **ska** fortsätta skrivas även när `audit` är avstängd. Avstängd modul döljer bara visning och hantering av loggen (endpoints, meny, sida). |
| BEH-8 | Data i en avstängd moduls tabeller **ska** bevaras och bli synlig igen när modulen aktiveras. Databasschemat är alltid fullständigt. |
| BEH-9 | Om `member-portal` är avstängd **ska** medlemssidorna inte serveras. |

## 7. Krav på API

### 7.1 `GET /api/modules`

| Id | Krav |
|---|---|
| API-1 | Kräver inloggning, alla roller. |
| API-2 | Svaret är personligt och speglar den inloggades roll och åtkomstnivå. |
| API-3 | Svaret innehåller alla kända moduler med `id`, `name`, `enabled`, `accessible` och `access` (den inloggades nivå: `none`, `read`, `write`). |
| API-4 | `accessible` är `true` bara när `enabled` är `true` och kontots beräknade nivå är minst `read`. |
| API-5 | `settings` innehåller bara en vitlistad delmängd av modulens inställningar som frontend behöver. Aldrig hemligheter. |
| API-6 | `settings` utelämnas för moduler som inte är tillgängliga. |
| API-7 | Modulernas `access`-listor och andra kontons roller skickas inte med. |

Exempel:

```json
{
  "modules": [
    { "id": "base", "name": "Bas", "enabled": true, "accessible": true },
    { "id": "storage", "name": "Uppläggningsplatser", "enabled": true, "accessible": true,
      "settings": { "defaultSeasonStart": "09-15" } },
    { "id": "reports", "name": "Rapporter", "enabled": true, "accessible": false }
  ]
}
```

### 7.2 `GET /api/modules/{id}`

| Id | Krav |
|---|---|
| API-8 | Returnerar samma objekt som i listan för en modul. |
| API-9 | Okänt modul-id ger 404. |
| API-10 | Båda anropen dokumenteras i OpenAPI under taggen "Modules". |

## 8. Krav på frontend

| Id | Krav |
|---|---|
| FE-1 | Båda applikationerna (admin och medlem) hämtar modullistan efter lyckad inloggning, innan första sidvisning. |
| FE-2 | Modullistan **ska** tömmas vid utloggning och hämtas på nytt vid nästa inloggning. |
| FE-3 | Menyval och sidor visas bara för moduler med `accessible: true`. |
| FE-4 | Varje route kan märkas med den modul den tillhör. Att öppna en route för en otillgänglig modul omdirigerar till startsidan med ett meddelande. |
| FE-5 | Om backend svarar `MODULE_DISABLED` eller `MODULE_ROLE_FORBIDDEN` **ska** frontend hämta om modullistan och uppdatera menyn. |
| FE-6 | Frontend **ska** aldrig ensam anses skydda en funktion. Kontrollen i backend är avgörande. |
| FE-7 | Ändringsknappar och formulär **ska** döljas eller inaktiveras när `access` är `read`. |

## 9. Övriga krav

| Id | Krav |
|---|---|
| NFR-1 | Med alla befintliga moduler aktiverade **ska** systemet bete sig som före införandet av modulsystemet. |
| NFR-2 | Modulkontrollen får inte märkbart fördröja anrop. Modulstatus hålls i minnet. |
| NFR-3 | Vid start **bör** loggen visa vilka moduler som är aktiva och vilka nivåer varje roll har. |
| NFR-4 | Ett nytt modul-id **ska** kunna läggas till utan att ändra befintliga moduler. |

## 10. Godkännandekriterier (exempel)

1. Med `storage.enabled=false` svarar `GET /api/winter-seasons` med 403 `MODULE_DISABLED`, och menyn saknar uppläggningsplatser i båda applikationerna.
2. Med `reports.access=[admin:r]` kan ett konto med `admin:rw` läsa rapporter men får 403 `MODULE_ROLE_FORBIDDEN` vid försök att ändra, och `GET /api/modules` visar `access: "read"` för kontot. Ett konto med bara `member:rw` visas `accessible=false`.
3. Systemet startar inte med `base.enabled=false`, med ogiltig nivå eller ogiltigt rollnamn i `access`, utan `superadmin:rw` i `base.access`, eller med `storage` aktiv men `base` avstängd.
4. Efter att `storage` stängts av och slagits på igen finns tidigare bokningar kvar.
5. En medlem ser aldrig inställningar eller nycklar för `ai-mail` i `GET /api/modules`.
6. Stängs `audit` av skrivs nya rader i händelseloggen fortfarande, men `GET /api/audit` svarar 403 `MODULE_DISABLED` och menyvalet saknas. När modulen slås på syns raderna som skrevs under tiden.

## 11. Öppna punkter

- Standardnivåer per modul (avsnitt 3) är inte godkända.
- Ska `/api/me/**` vara öppet för konton med andra roller än `member` (om kopplade till en person), eller bara för `member`?
- Funktionslistorna för `reports`, `ai-mail` och de framtida modulerna är förslag och behöver detaljeras.
- Behörighetskrav per uthyrningsobjekt (t.ex. genomgången kurs eller körkort) är inte specificerade och ingår inte i första versionen av `rentals`.
- Avisering till medlemmen när en preliminär bokning godkänns eller avslås kräver `notifications` och ingår inte i `rentals`.
- Beslutat: ett konto har flera rolltilldelningar och nivån beräknas som i avsnitt 3 (tak per post, högsta över matchande poster).
