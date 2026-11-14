# Genomförandeplan: modulen `rentals` (uthyrning)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Mål:** En modul där medlemmar bokar klubbens uthyrningsobjekt (upptagningstrailer, dagseglingsbåtar) i en kalender, rapporterar fel vid återlämning, och där administrationen lägger upp objekt med bild och regler.

**Arkitektur:** Modulen följer `storage`: en post i `PortModule`, egna tabeller i Flyway-schemat, lagrade entiteter i `model`, tjänster i `service`, controllers i `controller` med `@RequiresModule("rentals")`, och medlemsfunktionerna som nya metoder i `MeController`/`MeService`. Fillagringen är ny infrastruktur i `base`: ett `FileStore`-interface i `se.portplanner.file` med filsystemet som enda implementation i den här ändringen.

**Teknik:** Java 25, Spring Boot 4.1, Spring Data JPA, Flyway, H2. Vue 3 + Vite i båda SPA:erna, utan nya beroenden.

**Spec:** `internal/module_spec.md` avsnitt 2.8 (REN-1 till REN-14) och `internal/module.md` avsnitt 11.

## Globala förutsättningar

- `spring.jpa.hibernate.ddl-auto: validate`. Varje ny entitet kräver en Flyway-migrering i samma uppgift, annars startar inte ens testerna.
- All kod, alla kommentarer, loggar, felmeddelanden, testnamn och commit-meddelanden på engelska. Text i gränssnittet på svenska, som i övriga vyer.
- Inga nya beroenden i något av SPA:erna (fyra vardera i dag) och ingen AWS SDK i den här ändringen.
- Modul-id är `rentals`. `enabled: false` i `application.yml`. I `application-demo.yml` krävs `enabled: true` och `access: [superadmin:rw, admin:rw, member:rw]`, eftersom `DemoProfileTest` går igenom alla värden i `PortModule` och kräver skrivnivå för `admin:rw`.
- Inga belopp, avgifter eller depositioner någonstans i modulen.
- En bokning är ett halvöppet intervall `[start_at, end_at)` av typen `LocalDateTime` på timnivå, som `HaulOutBooking` använder väggklocka utan tidszon.
- Adminytan är `/api/rentals/**` och kräver `ROLE_ADMIN`. Medlemsytan är `/api/me/rentals/**` och kontrollerar ägarskap i tjänsten.
- Bilder: jpeg, png eller webp, högst 5 MB.
- Det finns ingen frontend-testuppsättning. Frontend verifieras med `npm run build` och genomklickning i demo-profilen.
- Backend-test körs med `cd portplanner-backend && mvn test -Dtest=<Klass>`. Det bygger även båda SPA:erna, vilket tar tid; `mvn surefire:test -Dtest=<Klass>` kör om testerna när koden redan är kompilerad.

## Review Focus

Fem indata som specen förutsätter men inte beskriver, och som slår mot en användare först. Varje rad har ett test i den uppgift som äger koden.

1. Bokning som slutar före den börjar, eller är noll timmar lång, ska avvisas med 409 och inte sparas (uppgift 5).
2. Två medlemmar som bokar samma timme samtidigt: exakt en ska lyckas, den andra få 409 (uppgift 6).
3. En medlem som avbokar eller återlämnar någon annans bokning ska få 404, aldrig komma åt den (uppgift 6 och 8).
4. En uppladdning som inte är en bild, eller är över 5 MB, ska ge 400, och ett byte av bild ska inte lämna den gamla filen kvar i lagret (uppgift 4).
5. En bokning på ett objekt med `bookable: false`, eller som startar längre fram än `max_days_ahead`, ska ge 409 (uppgift 5).

---

### Task 1: Fillagring i `base`

**Files:**
- Create: `portplanner-backend/src/main/java/se/portplanner/file/FileStore.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/file/LocalFileStore.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/file/FileStoreProperties.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/file/FileStoreConfiguration.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/file/StoredFile.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/file/StoredFileRepository.java`
- Create: `portplanner-backend/src/main/resources/db/migration/V2__stored_files.sql`
- Modify: `portplanner-backend/src/main/resources/application.yml` (nytt `portplanner.file-store`-block, utanför `modules`)
- Test: `portplanner-backend/src/test/java/se/portplanner/file/LocalFileStoreTest.java`

**Interfaces:**
- Consumes: inget.
- Produces:
  - `interface FileStore { String put(InputStream data, String contentType, long size); InputStream get(String key); void delete(String key); String name(); }`
  - `@Entity @Table(name = "stored_files") StoredFile` med `Long id, String store, String storageKey, String originalFilename, String contentType, long sizeBytes, LocalDateTime uploadedAt` och getters/setters via Lombok `@Getter @Setter`, som övriga entiteter.
  - `interface StoredFileRepository extends JpaRepository<StoredFile, Long> {}`

- [ ] **Step 1: Write the failing test**

```java
class LocalFileStoreTest {
    @TempDir Path dir;
    private LocalFileStore store() { /* new LocalFileStore(dir.toString()) */ }

    @Test
    void putThenGetReturnsTheSameBytes() throws Exception {
        var key = store().put(new ByteArrayInputStream("hello".getBytes(UTF_8)), "image/png", 5);
        assertEquals("hello", new String(store().get(key).readAllBytes(), UTF_8));
    }

    @Test
    void deleteRemovesTheFile() {
        var s = store();
        var key = s.put(new ByteArrayInputStream("hello".getBytes(UTF_8)), "image/png", 5);
        s.delete(key);
        assertThrows(ResourceNotFoundException.class, () -> s.get(key));
    }

    @Test
    void getUnknownKeyThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> store().get("00000000000000000000000000000000"));
    }

    @Test
    void nameIsLocal() {
        assertEquals("local", store().name());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd portplanner-backend && mvn test -Dtest=LocalFileStoreTest`
Expected: FAIL, `LocalFileStore` finns inte.

- [ ] **Step 3: Write `V2__stored_files.sql`**

```sql
CREATE TABLE stored_files (
    id                BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store             VARCHAR(20)  NOT NULL,
    storage_key       VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    uploaded_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 4: Implement the store**

- `FileStoreProperties` är `@ConfigurationProperties("portplanner.file-store")` med `String type = "local"` och en nästlad `Local { String path; }`.
- `LocalFileStore` tar katalogen i konstruktorn. Nyckeln är en UUID utan bindestreck. Filen läggs under `<path>/<de två första tecknen>/<de två följande>/<nyckeln>` så att en katalog inte fylls, och katalogerna skapas vid behov. `get` på en nyckel som inte finns kastar `ResourceNotFoundException`. `delete` på en nyckel som inte finns är tyst.
- `FileStoreConfiguration` har `@EnableConfigurationProperties(FileStoreProperties.class)` och en böna `@ConditionalOnProperty(name = "portplanner.file-store.type", havingValue = "local", matchIfMissing = true)`.
- `application.yml` får, på samma nivå som `portplanner.modules`:

```yaml
  file-store:
    type: local
    local:
      path: ${DATA_DIR:./data}/files
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest=LocalFileStoreTest`
Expected: PASS

- [ ] **Step 6: Run the whole suite so the new migration and entity validate**

Run: `cd portplanner-backend && mvn test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add portplanner-backend/src/main/java/se/portplanner/file portplanner-backend/src/main/resources/db/migration/V2__stored_files.sql portplanner-backend/src/main/resources/application.yml portplanner-backend/src/test/java/se/portplanner/file
git commit -m "feat: add a pluggable file store with a local filesystem implementation"
```

---

### Task 2: Registrera modulen

**Files:**
- Modify: `portplanner-backend/src/main/java/se/portplanner/module/PortModule.java`
- Modify: `portplanner-backend/src/main/resources/application.yml`
- Modify: `portplanner-backend/src/main/resources/application-demo.yml`
- Test: `portplanner-backend/src/test/java/se/portplanner/module/RentalsModuleTest.java`

**Interfaces:**
- Consumes: inget.
- Produces: `PortModule.RENTALS` med id `rentals`, visningsnamn `Uthyrning`, `dependsOn` `List.of("base")` och standardposterna `superadmin:rw, admin:rw, member:rw`.

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RentalsModuleTest {
    @Autowired MockMvc mvc;

    @Test
    void rentalsIsListedAndOffByDefault() throws Exception {
        mvc.perform(get("/api/modules").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.modules[?(@.id=='rentals')].enabled").value(false));
    }

    @Test
    void rentalsIsFetchableById() throws Exception {
        mvc.perform(get("/api/modules/rentals").with(asUser("member:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Uthyrning"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd portplanner-backend && mvn test -Dtest=RentalsModuleTest`
Expected: FAIL, `/api/modules/rentals` ger 404.

- [ ] **Step 3: Add the enum entry and the configuration**

`application.yml` får `rentals: { enabled: false }` under `portplanner.modules`. `application-demo.yml` får `rentals:` med `enabled: true` och `access: [superadmin:rw, admin:rw, member:rw]`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest='RentalsModuleTest,DemoProfileTest,ModuleServiceTest'`
Expected: PASS. `DemoProfileTest` går igenom alla moduler och faller om demo-profilen saknar `rentals`.

- [ ] **Step 5: Commit**

```bash
git add portplanner-backend/src/main/java/se/portplanner/module/PortModule.java portplanner-backend/src/main/resources/application.yml portplanner-backend/src/main/resources/application-demo.yml portplanner-backend/src/test/java/se/portplanner/module/RentalsModuleTest.java
git commit -m "feat: register the rentals module"
```

---

### Task 3: Uthyrningsobjekt med regler, och rollspärren

**Files:**
- Create: `portplanner-backend/src/main/resources/db/migration/V3__rentals.sql`
- Create: `portplanner-backend/src/main/java/se/portplanner/model/RentalObject.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/repository/RentalObjectRepository.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalObjectRequest.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalObjectResponse.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/service/RentalObjectService.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/controller/RentalObjectController.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/config/SecurityConfig.java`
- Modify: `portplanner-backend/src/test/java/se/portplanner/module/TestUsers.java`
- Test: `portplanner-backend/src/test/java/se/portplanner/controller/RentalObjectApiTest.java`
- Test: `portplanner-backend/src/test/java/se/portplanner/controller/RentalsDisabledTest.java`

**Interfaces:**
- Consumes: `StoredFile`, `StoredFileRepository` (uppgift 1), `PortModule.RENTALS` (uppgift 2).
- Produces:
  - `RentalObject` med `Long id, String name, String description, boolean bookable, int maxDurationHours, boolean requiresApproval, int maxDaysAhead, StoredFile image`.
  - `record RentalObjectRequest(@NotBlank String name, String description, boolean bookable, @Min(1) int maxDurationHours, boolean requiresApproval, @Min(1) int maxDaysAhead)`
  - `record RentalObjectResponse(Long id, String name, String description, boolean bookable, int maxDurationHours, boolean requiresApproval, int maxDaysAhead, boolean hasImage)` med `static RentalObjectResponse from(RentalObject)`.
  - `RentalObjectService` med `List<RentalObjectResponse> findAll()`, `RentalObjectResponse findById(Long)`, `RentalObjectResponse create(RentalObjectRequest)`, `RentalObjectResponse update(Long, RentalObjectRequest)`, `void delete(Long)`.
  - `RentalObjectRepository extends JpaRepository<RentalObject, Long>` med `List<RentalObject> findByBookableTrueOrderByNameAsc()`.
  - `TestUsers.asUser` lägger till de gamla myndigheterna: `ROLE_ADMIN` när någon roll inte är `member`, och `ROLE_MEMBER` när `member` finns, likt `LegacyAuthorities.of`.

- [ ] **Step 1: Write the failing tests**

```java
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestPropertySource(properties = "portplanner.modules.rentals.enabled=true")
class RentalObjectApiTest {
    @Autowired MockMvc mvc;

    private static final String TRAILER = """
        {"name":"Upptagningstrailer","description":"Kräver dragkrok","bookable":true,
         "maxDurationHours":24,"requiresApproval":true,"maxDaysAhead":30}""";

    @Test
    void adminCreatesAndListsAnObject() throws Exception {
        mvc.perform(post("/api/rentals/objects").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content(TRAILER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Upptagningstrailer"))
                .andExpect(jsonPath("$.hasImage").value(false));
        mvc.perform(get("/api/rentals/objects").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void memberCannotReachTheAdminApi() throws Exception {
        mvc.perform(get("/api/rentals/objects").with(asUser("member:rw")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/rentals/objects").with(asUser("member:rw"))
                        .contentType(APPLICATION_JSON).content(TRAILER))
                .andExpect(status().isForbidden());
    }

    @Test
    void zeroMaxDurationIsRejected() throws Exception {
        mvc.perform(post("/api/rentals/objects").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON)
                        .content(TRAILER.replace("\"maxDurationHours\":24", "\"maxDurationHours\":0")))
                .andExpect(status().isBadRequest());
    }
}

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RentalsDisabledTest {
    @Autowired MockMvc mvc;

    @Test
    void disabledModuleGivesModuleDisabled() throws Exception {
        mvc.perform(get("/api/rentals/objects").with(asUser("admin:rw")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.module").value("rentals"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd portplanner-backend && mvn test -Dtest='RentalObjectApiTest,RentalsDisabledTest'`
Expected: FAIL, endpointen finns inte.

- [ ] **Step 3: Write `V3__rentals.sql`**

Tre tabeller skapas här, även om entiteterna för bokning och felrapport kommer i senare uppgifter.

```sql
CREATE TABLE rental_objects (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    description        CLOB,
    bookable           BOOLEAN      NOT NULL DEFAULT TRUE,
    max_duration_hours INTEGER      NOT NULL DEFAULT 24,
    requires_approval  BOOLEAN      NOT NULL DEFAULT FALSE,
    max_days_ahead     INTEGER      NOT NULL DEFAULT 30,
    image_file_id      BIGINT       REFERENCES stored_files (id)
);

CREATE TABLE rental_bookings (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    object_id   BIGINT      NOT NULL REFERENCES rental_objects (id),
    person_id   BIGINT      NOT NULL REFERENCES persons (id),
    start_at    TIMESTAMP   NOT NULL,
    end_at      TIMESTAMP   NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    returned_at TIMESTAMP
);

CREATE INDEX idx_rental_bookings_object_start ON rental_bookings (object_id, start_at);

CREATE TABLE rental_fault_reports (
    id          BIGINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id  BIGINT    NOT NULL REFERENCES rental_bookings (id),
    description CLOB      NOT NULL,
    reported_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100)
);
```

- [ ] **Step 4: Implement the entity, repository, DTOs, service and controller**

`RentalObjectController` är `@RestController @RequiresModule("rentals") @RequestMapping("/api/rentals/objects") @Tag(name = "Rentals – objects")` med `GET`, `GET /{id}`, `POST` (201), `PUT /{id}` och `DELETE /{id}` (204), precis som `HaulOutBookingController` är byggd. Tjänsten loggar via `AuditService.log` med entitetstypen `RENTAL_OBJECT`. `delete` får sin fulla innebörd i uppgift 9; här räcker `repository.delete`.

- [ ] **Step 5: Add the admin matcher to `SecurityConfig`**

Raden `.requestMatchers("/api/rentals/**").hasRole("ADMIN")` läggs före `.requestMatchers("/api/me/**")`. `ROLE_ADMIN` sätts redan av `LegacyAuthorities` för varje konto som har någon annan roll än `member`. Kommentera raden med att den är modulens egen rollspärr enligt REN-14 och inte breddas till övriga moduler här.

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest='RentalObjectApiTest,RentalsDisabledTest'`
Expected: PASS

- [ ] **Step 7: Run the whole suite**

Run: `cd portplanner-backend && mvn test`
Expected: PASS. Faller något befintligt test på de nya myndigheterna i `TestUsers`, rätta testet, inte `TestUsers`.

- [ ] **Step 8: Commit**

```bash
git add portplanner-backend/src
git commit -m "feat: manage rental objects and their rules"
```

---

### Task 4: Bild på objektet

**Files:**
- Modify: `portplanner-backend/src/main/java/se/portplanner/service/RentalObjectService.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/controller/RentalObjectController.java`
- Modify: `portplanner-backend/src/main/resources/application.yml` (multipart-gränser)
- Test: `portplanner-backend/src/test/java/se/portplanner/controller/RentalObjectImageApiTest.java`

**Interfaces:**
- Consumes: `FileStore`, `StoredFileRepository` (uppgift 1), `RentalObjectService` (uppgift 3).
- Produces: `RentalObjectService` får `void uploadImage(Long objectId, MultipartFile file)`, `StoredFileContent readImage(Long objectId)` och `void deleteImage(Long objectId)`, där `record StoredFileContent(String contentType, long size, InputStream data)` ligger i `se.portplanner.file`.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void uploadThenFetchReturnsTheBytes() throws Exception {
    long id = createTrailer();
    var png = new MockMultipartFile("file", "trailer.png", "image/png", new byte[]{1, 2, 3});
    mvc.perform(multipart("/api/rentals/objects/" + id + "/image").file(png).with(asUser("admin:rw")))
            .andExpect(status().isNoContent());
    mvc.perform(get("/api/rentals/objects/" + id + "/image").with(asUser("admin:rw")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/png"));
    mvc.perform(get("/api/rentals/objects/" + id).with(asUser("admin:rw")))
            .andExpect(jsonPath("$.hasImage").value(true));
}

@Test
void anUnsupportedTypeIsRejected() throws Exception { /* text/plain -> 400 */ }

@Test
void aFileOverFiveMegabytesIsRejected() throws Exception { /* 5 * 1024 * 1024 + 1 bytes -> 400 */ }

@Test
void replacingTheImageRemovesTheOldFile() throws Exception {
    // upload twice, then assert storedFileRepository.count() == 1
    // and that the first key is gone from the FileStore
}

@Test
void fetchingAMissingImageGives404() throws Exception { /* object without image -> 404 */ }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd portplanner-backend && mvn test -Dtest=RentalObjectImageApiTest`
Expected: FAIL

- [ ] **Step 3: Implement upload, fetch and delete**

Tillåtna typer är `image/jpeg`, `image/png` och `image/webp`; allt annat och allt över `5 * 1024 * 1024` byte ger `IllegalArgumentException`, som `GlobalExceptionHandler` svarar 400 på. Uppladdningen skriver till `FileStore`, sparar en `StoredFile` med `store = fileStore.name()` och sätter den på objektet. Finns redan en bild tas den gamla bort ur lagret och raden ur `stored_files` i samma transaktion. Hämtningen svarar med `Content-Type` ur `StoredFile` och strömmar innehållet; saknas bild kastas `ResourceNotFoundException`. `application.yml` får `spring.servlet.multipart.max-file-size: 5MB` och `max-request-size: 10MB`, med en kommentar om att gränsen också gäller importfilerna i `import-export`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest=RentalObjectImageApiTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add portplanner-backend/src portplanner-backend/src/main/resources/application.yml
git commit -m "feat: upload and serve an image for a rental object"
```

---

### Task 5: Regelvalidering

**Files:**
- Create: `portplanner-backend/src/main/java/se/portplanner/service/RentalRuleValidator.java`
- Modify: `internal/module.md` (avsnitt 11.3)
- Test: `portplanner-backend/src/test/java/se/portplanner/service/RentalRuleValidatorTest.java`

**Interfaces:**
- Consumes: `RentalObject` (uppgift 3).
- Produces: `@Component RentalRuleValidator` med `void check(RentalObject object, LocalDateTime start, LocalDateTime end, LocalDateTime now)`, som kastar `ConflictException` vid regelbrott.

Beslut: regelbrott använder det befintliga `ConflictException` (409) i stället för en egen `RentalRuleException`. Statusen blir densamma som designen anger och ingen ny exception-typ behövs. `module.md` avsnitt 11.3 rättas i steg 4.

- [ ] **Step 1: Write the failing test**

```java
class RentalRuleValidatorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 9, 0);
    private RentalObject object() { /* bookable, maxDurationHours=8, maxDaysAhead=30, requiresApproval=false */ }

    @Test void aValidBookingPasses() { /* 2026-05-02 08:00 - 12:00, no throw */ }
    @Test void endBeforeStartIsRejected() { /* 12:00 - 08:00 -> ConflictException */ }
    @Test void aZeroLengthBookingIsRejected() { /* 08:00 - 08:00 -> ConflictException */ }
    @Test void longerThanMaxDurationIsRejected() { /* 08:00 - 20:00 with max 8 -> ConflictException */ }
    @Test void aStartInThePastIsRejected() { /* 2026-04-30 08:00 -> ConflictException */ }
    @Test void aStartBeyondMaxDaysAheadIsRejected() { /* NOW plus 31 days -> ConflictException */ }
    @Test void anObjectThatIsNotBookableIsRejected() { /* bookable=false -> ConflictException */ }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd portplanner-backend && mvn test -Dtest=RentalRuleValidatorTest`
Expected: FAIL

- [ ] **Step 3: Implement `RentalRuleValidator`**

Varje kontroll får ett eget meddelande på engelska. Ordningen är: objektet bokningsbart, start före slut, längden mot `maxDurationHours`, start inte före `now`, start inom `maxDaysAhead` dagar från `now`.

- [ ] **Step 4: Correct `module.md` 11.3**

Byt stycket om `RentalRuleException` mot att regelbrott kastar `ConflictException`, som redan svarar 409, och att `PolicyViolationException` inte används eftersom den ger 403.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd portplanner-backend && mvn test -Dtest=RentalRuleValidatorTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add portplanner-backend/src internal/module.md
git commit -m "feat: validate a rental booking against the object's rules"
```

---

### Task 6: Medlemmens objektlista, kalender och bokning

**Files:**
- Create: `portplanner-backend/src/main/java/se/portplanner/model/RentalBooking.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/model/RentalBookingStatus.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/repository/RentalBookingRepository.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalBookingRequest.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalBookingResponse.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalBusyIntervalResponse.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/service/RentalBookingService.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/repository/RentalObjectRepository.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/service/MeService.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/controller/MeController.java`
- Test: `portplanner-backend/src/test/java/se/portplanner/controller/MeRentalsApiTest.java`

**Interfaces:**
- Consumes: `RentalObject`, `RentalObjectRepository` (uppgift 3), `RentalObjectService.readImage` (uppgift 4), `RentalRuleValidator.check` (uppgift 5).
- Produces:
  - `enum RentalBookingStatus { PENDING, CONFIRMED, REJECTED, CANCELLED, RETURNED }` med `static Set<RentalBookingStatus> holding()` som ger `PENDING, CONFIRMED, RETURNED`.
  - `RentalBooking` med `Long id, RentalObject object, Person person, LocalDateTime startAt, LocalDateTime endAt, RentalBookingStatus status, LocalDateTime createdAt, LocalDateTime returnedAt`.
  - `record RentalBookingRequest(@NotNull Long objectId, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt)`
  - `record RentalBookingResponse(Long id, Long objectId, String objectName, LocalDateTime startAt, LocalDateTime endAt, String status, LocalDateTime returnedAt)` med `static from(RentalBooking)`.
  - `record RentalBusyIntervalResponse(LocalDateTime startAt, LocalDateTime endAt)` — innehåller aldrig person eller status.
  - `RentalBookingRepository` med `List<RentalBooking> findByObjectIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(Long objectId, Collection<RentalBookingStatus> statuses, LocalDateTime before, LocalDateTime after)`, `List<RentalBooking> findByPersonIdOrderByStartAtDesc(Long personId)` och `boolean existsByPersonId(Long personId)`.
  - `RentalObjectRepository` får `@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from RentalObject o where o.id = :id") Optional<RentalObject> findByIdForUpdate(@Param("id") Long id)`.
  - `RentalBookingService` med `List<RentalBusyIntervalResponse> busyIntervals(Long objectId, LocalDateTime from, LocalDateTime to)`, `RentalBookingResponse book(Long objectId, Long personId, LocalDateTime start, LocalDateTime end)` och `void cancel(Long bookingId, Long personIdOrNullForAdmin)`.

- [ ] **Step 1: Write the failing tests**

```java
@Test void memberBooksAFreeInterval() throws Exception {
    // POST /api/me/rentals/bookings -> 201, status CONFIRMED on an object without approval
}

@Test void anOverlappingBookingIsRejected() throws Exception {
    // same interval twice -> second gives 409
}

@Test void availabilityDoesNotRevealWhoBooked() throws Exception {
    mvc.perform(get("/api/me/rentals/objects/" + id + "/availability")
                    .param("from", "2026-05-01T00:00:00").param("to", "2026-05-31T00:00:00")
                    .with(asMember()))
            .andExpect(jsonPath("$[0].startAt").exists())
            .andExpect(jsonPath("$[0].personName").doesNotExist())
            .andExpect(jsonPath("$[0].status").doesNotExist());
}

@Test void aMemberCannotCancelAnotherMembersBooking() throws Exception { /* 404 */ }

@Test void cancellingBeforeTheStartFreesTheTime() throws Exception { /* cancel, then book the same interval -> 201 */ }

@Test void cancellingAfterTheStartIsRejected() throws Exception { /* 409 */ }

@Test void twoSimultaneousBookingsOnlyOneSucceeds() throws Exception {
    // two threads calling the service's book() for the same interval through an ExecutorService,
    // assert exactly one returns a booking and the other throws ConflictException
}
```

Testklassen behöver en medlem med en `Person` och ett `AppUser` kopplat till den, eftersom `MeService.currentUser()` slår upp kontot på användarnamnet. Lägg en `@BeforeEach` som sparar person och konto, och en hjälpmetod `asMember()` som returnerar `TestUsers.asUserNamed(<kontots användarnamn>, "member:rw")`. En andra medlem behövs för ägarskapstestet. Följ uppsättningen i `ImportExportIntegrationTest`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd portplanner-backend && mvn test -Dtest=MeRentalsApiTest`
Expected: FAIL

- [ ] **Step 3: Implement the booking service**

`book` tar först `findByIdForUpdate` på objektet, så att två samtidiga anrop serialiseras, anropar sedan `RentalRuleValidator.check`, och letar överlapp bland `RentalBookingStatus.holding()`. Finns överlapp kastas `ConflictException`. Statusen blir `PENDING` när objektet har `requiresApproval`, annars `CONFIRMED`. `cancel` kräver att bokningen tillhör personen när ett person-id skickas med, att starttiden inte passerat, och sätter `CANCELLED`. Allt loggas via `AuditService` med entitetstypen `RENTAL_BOOKING`.

- [ ] **Step 4: Add the member endpoints**

I `MeController`, med `@RequiresModule("rentals")` på varje metod (klassen har redan `member-portal`), som den befintliga `getAvailableHaulOutSlots` gör:
`GET /rentals/objects`, `GET /rentals/objects/{id}/image`, `GET /rentals/objects/{id}/availability`, `GET /rentals/bookings`, `POST /rentals/bookings` (201), `DELETE /rentals/bookings/{id}` (204). Objektlistan returnerar bara objekt med `bookable = true`. `MeService` löser personen som i övriga metoder.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest=MeRentalsApiTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add portplanner-backend/src
git commit -m "feat: let members see availability and book a rental object"
```

---

### Task 7: Godkännande och bokning åt en medlem

**Files:**
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalAdminBookingRequest.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/controller/RentalBookingController.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/service/RentalBookingService.java`
- Test: `portplanner-backend/src/test/java/se/portplanner/controller/RentalBookingApiTest.java`

**Interfaces:**
- Consumes: `RentalBookingService.book/cancel` (uppgift 6).
- Produces:
  - `record RentalAdminBookingRequest(@NotNull Long objectId, @NotNull Long personId, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt)`
  - `RentalBookingService` får `List<RentalBookingResponse> findByObject(Long objectId, LocalDateTime from, LocalDateTime to)`, `RentalBookingResponse bookForPerson(RentalAdminBookingRequest req)`, `RentalBookingResponse approve(Long id)` och `RentalBookingResponse reject(Long id)`.

- [ ] **Step 1: Write the failing tests**

```java
@Test void anObjectWithApprovalCreatesAPendingBooking() throws Exception { /* member POST -> status PENDING */ }
@Test void aPendingBookingHoldsTheTime() throws Exception { /* another member, same interval -> 409 */ }
@Test void rejectingFreesTheTime() throws Exception { /* reject, then book again -> 201 */ }
@Test void approvingMakesItConfirmed() throws Exception { /* status CONFIRMED */ }
@Test void anAdminBookingIsConfirmedEvenWhenApprovalIsRequired() throws Exception { /* REN-12 */ }
@Test void anAdminCancelsAMembersBooking() throws Exception { /* 204, status CANCELLED */ }
@Test void approvingAnAlreadyRejectedBookingIsRejected() throws Exception { /* 409 */ }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd portplanner-backend && mvn test -Dtest=RentalBookingApiTest`
Expected: FAIL

- [ ] **Step 3: Implement the service methods and the controller**

`RentalBookingController` är `@RestController @RequiresModule("rentals") @RequestMapping("/api/rentals/bookings")` med `GET` (parametrarna `objectId`, `from`, `to`), `POST` (201), `POST /{id}/approve`, `POST /{id}/reject` och `DELETE /{id}` (204). `approve` och `reject` kräver status `PENDING`, annars `ConflictException`. `bookForPerson` hoppar över godkännandesteget men går genom samma lås, regelvalidering och överlappskontroll.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest=RentalBookingApiTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add portplanner-backend/src
git commit -m "feat: approve rental bookings and book on behalf of a member"
```

---

### Task 8: Återlämning och felrapporter

**Files:**
- Create: `portplanner-backend/src/main/java/se/portplanner/model/RentalFaultReport.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/repository/RentalFaultReportRepository.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalReturnRequest.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/dto/RentalFaultReportResponse.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/controller/RentalFaultReportController.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/service/RentalBookingService.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/controller/MeController.java`, `service/MeService.java`
- Test: `portplanner-backend/src/test/java/se/portplanner/controller/RentalReturnApiTest.java`

**Interfaces:**
- Consumes: `RentalBooking`, `RentalBookingService` (uppgift 6 och 7).
- Produces:
  - `RentalFaultReport` med `Long id, RentalBooking booking, String description, LocalDateTime reportedAt, LocalDateTime resolvedAt, String resolvedBy`.
  - `record RentalReturnRequest(String faultDescription)` — tom eller `null` betyder inget fel.
  - `record RentalFaultReportResponse(Long id, Long bookingId, Long objectId, String objectName, String reportedBy, LocalDateTime reportedAt, String description, boolean resolved)` med `static from(RentalFaultReport)`.
  - `RentalFaultReportRepository` med `List<RentalFaultReport> findByResolvedAtIsNullOrderByReportedAtDesc()`, `List<RentalFaultReport> findByBookingObjectIdOrderByReportedAtDesc(Long objectId)`.
  - `RentalBookingService` får `RentalBookingResponse markReturned(Long bookingId, Long personIdOrNullForAdmin, RentalReturnRequest req)`, `List<RentalFaultReportResponse> openFaults()`, `List<RentalFaultReportResponse> faultsForObject(Long objectId)` och `void resolveFault(Long faultId)`.

- [ ] **Step 1: Write the failing tests**

```java
@Test void aMemberReturnsWithAFaultReport() throws Exception {
    // POST /api/me/rentals/bookings/{id}/return with {"faultDescription":"Storseglet är trasigt"}
    // -> booking status RETURNED, and the report shows up in GET /api/rentals/fault-reports
}

@Test void returningWithoutAFaultReportIsAllowed() throws Exception { /* empty body -> RETURNED, no report */ }
@Test void anAdminReturnsOnBehalfOfAMember() throws Exception { /* REN-13 */ }
@Test void aMemberCannotReturnAnotherMembersBooking() throws Exception { /* 404 */ }
@Test void aFaultReportDoesNotBlockLaterBookings() throws Exception { /* REN-8: book a later interval -> 201 */ }
@Test void resolvingAFaultReportRemovesItFromTheOpenList() throws Exception { /* list is empty afterwards */ }
@Test void returningTwiceIsRejected() throws Exception { /* 409 */ }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd portplanner-backend && mvn test -Dtest=RentalReturnApiTest`
Expected: FAIL

- [ ] **Step 3: Implement the return flow and the fault reports**

`markReturned` kräver status `CONFIRMED`, sätter `RETURNED` och `returnedAt`, och skapar en `RentalFaultReport` när beskrivningen inte är tom. Återlämning är tillåten även efter starttiden; det är undantaget från REN-7, och det ska framgå av en kommentar i koden. `RentalFaultReportController` är `@RestController @RequiresModule("rentals") @RequestMapping("/api/rentals/fault-reports")` med `GET` (parametrarna `resolved` och `objectId`) och `POST /{id}/resolve`. `resolvedBy` sätts från `Authentication.getName()`, som `AuditService` gör.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest=RentalReturnApiTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add portplanner-backend/src
git commit -m "feat: report faults when a rental object is returned"
```

---

### Task 9: Radering av objekt och av personer med bokningar

**Files:**
- Create: `portplanner-backend/src/main/java/se/portplanner/service/PersonDeletionCheck.java`
- Create: `portplanner-backend/src/main/java/se/portplanner/service/RentalPersonDeletionCheck.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/service/PersonService.java`
- Modify: `portplanner-backend/src/main/java/se/portplanner/service/RentalObjectService.java`
- Test: `portplanner-backend/src/test/java/se/portplanner/controller/RentalDeletionApiTest.java`

**Interfaces:**
- Consumes: `RentalBookingRepository.existsByPersonId` (uppgift 6).
- Produces: `public interface PersonDeletionCheck { void check(Long personId); }`. `PersonService` får `List<PersonDeletionCheck>` injicerad i konstruktorn och anropar varje kontroll innan `personRepository.delete`. Spring injicerar en tom lista när ingen implementation finns, så `base` får ingen referens till modulen (DEP-4).

- [ ] **Step 1: Write the failing tests**

```java
@Test void deletingAPersonWithRentalBookingsGivesConflict() throws Exception {
    mvc.perform(delete("/api/persons/" + personId).with(asUser("admin:rw")))
            .andExpect(status().isConflict());
}

@Test void deletingAnObjectWithBookingsGivesConflict() throws Exception { /* 409 */ }

@Test void deletingAnObjectWithoutBookingsRemovesTheImage() throws Exception {
    // upload an image, delete the object, assert storedFileRepository.count() == 0
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd portplanner-backend && mvn test -Dtest=RentalDeletionApiTest`
Expected: FAIL, raderingen ger 500 på främmande nyckel.

- [ ] **Step 3: Implement the checks**

`RentalPersonDeletionCheck` kastar `ConflictException("The person has rental bookings. Cancel them first")` när `existsByPersonId` är sant. `RentalObjectService.delete` kastar `ConflictException` när objektet har bokningar, och tar annars bort bilden ur `FileStore` och raden ur `stored_files` innan objektet raderas.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd portplanner-backend && mvn test -Dtest=RentalDeletionApiTest`
Expected: PASS

- [ ] **Step 5: Run the whole suite**

Run: `cd portplanner-backend && mvn test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add portplanner-backend/src
git commit -m "feat: refuse deleting a person or an object with rental bookings"
```

---

### Task 10: Admin-vy

**Files:**
- Create: `portplanner-frontend/src/api/rentals.js`
- Create: `portplanner-frontend/src/views/RentalObjectsView.vue`
- Modify: `portplanner-frontend/src/router/index.js`
- Modify: `portplanner-frontend/src/components/AppLayout.vue`

**Interfaces:**
- Consumes: adminendpointerna under `/api/rentals` (uppgift 3, 4, 7, 8).
- Produces: `api/rentals.js` exporterar `getObjects`, `getObject`, `createObject`, `updateObject`, `deleteObject`, `uploadImage`, `deleteImage`, `getBookings`, `createBooking`, `approveBooking`, `rejectBooking`, `cancelBooking`, `getFaultReports`, `resolveFaultReport`, i samma stil som `api/winterHaulOut.js`.

- [ ] **Step 1: Write the API module and the view**

`RentalObjectsView.vue` har en lista med objekt, ett formulär för namn, beskrivning, bokningsbar, längsta bokningslängd, kräver godkännande och dagar framåt, en bilduppladdning med `FormData`, en bokningslista per objekt med knappar för godkänn, avslå, återlämna och avboka, ett formulär för att boka åt en medlem, och en flik med öppna felrapporter och knappen åtgärdad. Följ formulär- och modalmönstret i `WinterSeasonsView.vue` och `BaseModal.vue`. All text på svenska.

- [ ] **Step 2: Add the route and the menu entry**

Route `{ path: '/rentals', meta: { module: 'rentals' }, component: () => import('../views/RentalObjectsView.vue') }` och en `router-link` i `AppLayout.vue` med `v-if="modules.isAccessible('rentals')"` och texten Uthyrning.

- [ ] **Step 3: Verify the build**

Run: `cd portplanner-frontend && npm run build`
Expected: bygget går igenom utan fel.

- [ ] **Step 4: Commit**

```bash
git add portplanner-frontend/src
git commit -m "feat: add the rentals admin view"
```

---

### Task 11: Medlemsvy med kalender

**Files:**
- Create: `portplanner-member/src/api/rentals.js`
- Create: `portplanner-member/src/views/RentalsView.vue`
- Create: `portplanner-member/src/components/BookingCalendar.vue`
- Modify: `portplanner-member/src/router/index.js`
- Modify: `portplanner-member/src/App.vue`

**Interfaces:**
- Consumes: `/api/me/rentals/**` (uppgift 6 och 8).
- Produces: `api/rentals.js` exporterar `getObjects`, `getAvailability(objectId, from, to)`, `getMyBookings`, `createBooking`, `cancelBooking`, `returnBooking`.
  `BookingCalendar.vue` tar `props: { busy: Array, maxDurationHours: Number, maxDaysAhead: Number }` och sänder `select` med `{ startAt, endAt }`.

- [ ] **Step 1: Write the calendar component**

Månadsvy byggd för hand med `Date` och ett rutnät av dagar, utan nytt beroende. En dag som har något i `busy` markeras. Klick på en dag öppnar dygnets timmar, där timmar som täcks av `busy` är utgråade och inte valbara, och man väljer start och slut. Dagar bortom `maxDaysAhead` och dagar som passerat är inte valbara.

- [ ] **Step 2: Write the view**

`RentalsView.vue` listar objekten med bild (`<img :src="'/api/me/rentals/objects/' + o.id + '/image'">` när `hasImage`), namn och beskrivning. Val av objekt visar kalendern och objektets regler i klartext. Egna bokningar listas överst med knapparna Avboka, när starttiden inte passerat, och Återlämna, som öppnar ett fritextfält för fel och brister som får lämnas tomt. Fel från backend visas som i `HaulOutView.vue`, via `e.response?.data?.error`.

- [ ] **Step 3: Add the route and the menu entry**

Route `{ path: '/rentals', meta: { module: 'rentals' }, component: () => import('../views/RentalsView.vue') }` och en `router-link` i `App.vue` med `v-if="modules.isAccessible('rentals')"` och texten Hyra.

- [ ] **Step 4: Verify the build**

Run: `cd portplanner-member && npm run build`
Expected: bygget går igenom utan fel.

- [ ] **Step 5: Verify in the running app**

Run: `cd portplanner-backend && mvn spring-boot:run -Dspring-boot.run.profiles=demo`
Logga in som `demo`, lägg upp ett objekt med bild i admin, och boka det i medlemsportalen. Kontrollera att en bokad tid är upptagen i kalendern, att felrapporten syns i admin, och att menyvalet försvinner när `rentals.enabled` sätts till `false`.

- [ ] **Step 6: Commit**

```bash
git add portplanner-member/src
git commit -m "feat: add the rentals calendar to the member portal"
```

---

### Task 12: Dokumentation

**Files:**
- Modify: `docs/release-notes.md`

- [ ] **Step 1: Add the release notes**

Två rader under Unreleased, i samma korta stil som de befintliga:

```markdown
- Rentals: members book the club's trailer and day-sailing boats in a calendar and report faults on return.
- Uploaded files are stored through a pluggable file store, with the local filesystem as the first implementation.
```

- [ ] **Step 2: Run the whole suite one last time**

Run: `cd portplanner-backend && mvn test`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add docs/release-notes.md
git commit -m "docs: add release notes for the rentals module"
```
