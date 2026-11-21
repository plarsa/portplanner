# Portplanner — implementera designbeskrivningen

Vad som behöver göras för att `design.md` ska gå att genomföra i admin-portalen,
medlemsportalen och landningssidan. Nuläget mätt 2026-11-19.

## 1. Blockerare att lösa först

| Problem | Detalj |
| --- | --- |
| Designen är inte tillgänglig för appen | Tokens finns bara i www-repot (`/Users/pepl/src/portplanner-www/www/css/style.css`). Ingen av de tre ytorna i det här repot kan bygga mot dem. |
| Kopian är trasig | `app-yta.html` länkar `../www/css/style.css`, som inte finns under `internal/`. Referenssidan renderas ostajlad här — den fungerar bara i www-repot. |
| Guiden är otrackad | `internal/designguide/` är inte commitad. |
| Ingen delningsmekanism | Två fristående Vite-appar, ingen workspace, inget delat paket. `lib/markdown.js` är redan identiskt duplicerad i båda — ett delat stillager får samma problem om inget sätts upp. |

## 2. Nuläget i siffror

| | Admin | Medlem |
| --- | --- | --- |
| Vue-filer med egen `<style>` | 19 | 9 |
| Rader scoped CSS | 928 | 356 |
| Distinkta hårdkodade hex-färger | 98 | 49 |
| CSS-variabler | 0 | 0 |
| Typsnitt | `system-ui` | `system-ui` |

Dessutom: 11 filer definierar egna tabellstilar, 23 egna knappstilar, 20 egna fältstilar.
Statusmarkörer finns i fyra parallella varianter (`.status`, `.status-chip`,
`.status-badge`, `.badge`, 108 förekomster).

## 3. Vad som måste byggas

**Ett gemensamt stillager** (finns inte alls idag) med guidens `:root`-tokens, plus det
kapitel 11 definierar men som inte finns i `style.css`: de 15 statusfärgerna, fält,
tabellkort och sekundärknapp. `app-yta.html` är prototypen — den koden behöver bli en
riktig stilmall som alla tre ytor laddar.

**Typsnitten.** Fraunces, Instrument Sans och IBM Plex Mono, idag noll. Mono på alla
siffror är guidens huvudpoäng och träffar platsnummer, mått, datum och tabellrubriker i i
stort sett varje vy. Fraunces ska enligt kapitel 11 inte med till portalerna, bara
ordmärket.

**Navigationen byggs om i båda apparna.** Guiden föreskriver vit topprad med
`--shadow-header`, märke till vänster, flikar till höger, aktiv flik på `--sea-pale`.
Idag: admin har en 220px mörk sidomeny (`#1a3a5c`), medlemsportalen en mörk topprad med
understruken aktiv länk. Ingendera går att tokenisera — de måste ersättas.

**Mobilmönstret** (hamburgare, skärmsläck, Escape, fokus tillbaka, 760px) finns på sajten
men inte i någon app. Admin har ingen mobilhantering alls.

**Globala regler** som saknas helt: fokusring `2px --sea` på `:focus-visible`,
`prefers-reduced-motion`, ikonregeln 24×24 / stroke 1.6. Emoji ⚓ används som märke i båda
apparna och ersätts av `portplanner-mark.svg`.

## 4. Färgöversättning

Den mörkblå `#1a3a5c` används 66 gånger i admin och 16 i medlemsportalen som både menyfärg
och primärknapp. Guiden har ingen sådan färg: knappar blir `--sea` `#146bb0`, menyn blir
vit. Det är ingen ren utbytestabell utan en omritning av skalet.

De ad hoc-grå nyanserna är ett AA-problem som guiden löser: `#888` (37 förekomster i
admin) ger 3,25:1 mot `#f5f5f5` och `#999` 2,61:1 — båda underkända för brödtext.
`--text-muted` ger 5,6:1.

Statusfärgerna (`#c0392b`, `#27ae60`, `#e67e22`, `#fff3cd` m.fl.) mappas mot guidens fem
roller: klar / pågår / väntar / fel / ledig. Regeln "färg aldrig ensam" kräver en
genomgång av att varje markör har text, inte bara kulör.

## 5. Beslut som krävs innan arbetet startar

1. **Admins sidomeny.** Guiden beskriver bara en topprad. Admin har 11 navlänkar — de ryms
   inte i en rad. Antingen tokeniseras sidomenyn (avsteg som guiden inte täcker) eller så
   flyttas navigationen upp med överflöde. Största öppna frågan.
2. **Var stillagret bor.** Kopiera `style.css` hit, göra www-repot till beroendekälla,
   eller lyfta ut ett delat paket. Påverkar även landningssidan i backend.
3. **Typsnittsleverans.** Google Fonts (guidens egen invändning i kapitel 12: extern kedja,
   några hundra kB) eller självhostat i `static/`.
4. **Mörkt läge** finns inte i paletten. Ska portalerna ha det måste en egen uppsättning
   ytfärger definieras.

## 6. Ytor som nämns men inte finns

Guidens ingress lovar att designen ska gå att återskapa på admin-portalen,
medlemsportalen, e-post och PDF. Backend har varken mall-katalog eller någon mail- eller
PDF-rendering — de två ytorna är tom mark.

Landningssidan `portplanner-backend/src/main/resources/static/index.html` är handskriven
med egen palett (`#f4f7fb`, `system-ui`) och ligger närmast sajten i uttryck — billigaste
ytan att ta först.

## 7. Föreslagen ordning

1. Flytta in guiden och tokens i repot, laga länken i `app-yta.html`, bestäm
   delningsmekanismen.
2. Stillagret: tokens, statusfärger, fält, tabellkort, knappar, fokusring, reduced motion.
3. Landningssidan — liten yta, bevisar lagret.
4. Skalet i medlemsportalen (topprad och mobilmönster), mindre än admins.
5. Admin: beslut om sidomeny, sedan skalet.
6. Vy för vy: ersätt scoped CSS med tokens, mono på siffror, statusmarkörer till de fem
   rollerna.

Steg 6 är den stora posten: 1 284 rader scoped CSS över 28 filer.

## Genomfört

Stillagret ligger i `shared/styles/portplanner.css` och laddas av landningssidan,
medlemsportalen och adminportalen. Två beslut togs under arbetet som guiden inte
täcker, och som behöver en rad i `design.md` om de ska gälla:

**Admin har en sidomeny, inte en topprad.** Kapitel 6 säger att radmönstret gäller
rakt av i portalerna, men elva destinationer plus undersidor ryms inte på 1 180px —
mätt behövs ungefär 1 480px. Raden fungerar bara med basmodulerna påslagna och går
sönder när en modul aktiveras. Kolumnen följer samma regler som raden: pappersyta,
hårfin kant istället för ram, aktivt val på `--sea-pale`, ingen skugga. Under 980px
viks den ihop till en rad med hamburgare, låda och skärmsläck.

**Kartpaletten är ett förslag.** Bryggkartan, layouteditorn och vinterplanen målar
betydelse med ytor, och guidens fem statusfärger är textfärger på mjuk botten. De
sex kartvariablerna i stillagret är härledda ur samma roller: mjuka fyllningar med
hårlinje och etiketten i rollens egen textfärg. Terräng är ingen status, så vatten,
land och brygga har tre egna neutrala toner.

En följd av att följa guiden: "tilldelad" är grön och "ledig" är neutralt grå, vilket
är tvärtom mot vad portalen visade förut, där ledig plats var grön och tilldelad röd.
