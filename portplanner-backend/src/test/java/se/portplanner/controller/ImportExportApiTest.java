package se.portplanner.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUser;

/** One file carries every area, each as its own section. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:importexport;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class ImportExportApiTest {

    /** A person, a boat that belongs to them, and a dock with one slip. */
    private static final String FILE = """
            {
              "persons": [
                {"firstName":"Karin","lastName":"Lind","email":"karin@example.org","phone":"070-1"}
              ],
              "boats": [
                {"model":"Najad 331","lengthM":9.8,"widthM":3.2,"ownerEmail":"karin@example.org"}
              ],
              "docks": [
                {"name":"Brygga X","description":"Testbrygga",
                 "slips":[{"slipNumber":"X 1","maxWidthM":3.5,"maxLengthM":10.0}]}
              ]
            }
            """;

    @Autowired MockMvc mvc;

    private void importFile(String body) throws Exception {
        mvc.perform(post("/api/import").with(asUser("admin:rw")).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void theExportHasASectionPerArea() throws Exception {
        mvc.perform(get("/api/export").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persons").isArray())
                .andExpect(jsonPath("$.boats").isArray())
                .andExpect(jsonPath("$.docks").isArray());
    }

    @Test
    void oneFileImportsEveryArea() throws Exception {
        mvc.perform(post("/api/import").with(asUser("admin:rw")).contentType(APPLICATION_JSON).content(FILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persons.personsCreated").value(1))
                .andExpect(jsonPath("$.boats.boatsCreated").value(1))
                .andExpect(jsonPath("$.docks.docksCreated").value(1))
                .andExpect(jsonPath("$.docks.slipsCreated").value(1));

        mvc.perform(get("/api/export").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.persons[?(@.email=='karin@example.org')]").exists())
                .andExpect(jsonPath("$.boats[?(@.model=='Najad 331')].ownerEmail").value("karin@example.org"))
                .andExpect(jsonPath("$.docks[?(@.name=='Brygga X')].slips[0].slipNumber").value("X 1"));
    }

    @Test
    void aSectionMayBeLeftOut() throws Exception {
        mvc.perform(post("/api/import").with(asUser("admin:rw")).contentType(APPLICATION_JSON)
                        .content("{\"docks\":[{\"name\":\"Ensam brygga\",\"slips\":[]}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docks.docksCreated").value(1))
                .andExpect(jsonPath("$.persons.personsCreated").value(0))
                .andExpect(jsonPath("$.boats.boatsCreated").value(0));
    }

    @Test
    void thePreviewChangesNothing() throws Exception {
        mvc.perform(post("/api/import/preview").with(asUser("admin:rw")).contentType(APPLICATION_JSON)
                        .content("""
                            {"persons":[{"firstName":"Per","lastName":"Vik","email":"per@example.org"}],
                             "docks":[{"name":"Previewbrygga","slips":[]}]}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persons.personsNew").value(1))
                .andExpect(jsonPath("$.docks.docksNew").value(1));

        mvc.perform(get("/api/export").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.persons[?(@.email=='per@example.org')]").doesNotExist())
                .andExpect(jsonPath("$.docks[?(@.name=='Previewbrygga')]").doesNotExist());
    }

    @Test
    void importingTheSameFileTwiceAddsNothing() throws Exception {
        String file = FILE.replace("karin@example.org", "idem@example.org")
                .replace("Brygga X", "Brygga Idem").replace("X 1", "I 1");
        importFile(file);
        mvc.perform(post("/api/import").with(asUser("admin:rw")).contentType(APPLICATION_JSON).content(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persons.personsCreated").value(0))
                .andExpect(jsonPath("$.boats.boatsCreated").value(0))
                .andExpect(jsonPath("$.docks.docksCreated").value(0))
                .andExpect(jsonPath("$.docks.slipsSkipped").value(1));
    }
}
