package se.portplanner.controller;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUser;

/** The painted layout is its own resource, so listing docks no longer carries it. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:docklayout;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class DockLayoutApiTest {

    private static final String LAYOUT = "{\"v\":2,\"w\":12,\"h\":6,\"g\":[\"2L10.\"],\"s\":[]}";

    @Autowired MockMvc mvc;

    private long createDock(String name) throws Exception {
        var body = mvc.perform(post("/api/docks").with(asUser("admin:rw")).contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"Yttre\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.id", Integer.class).longValue();
    }

    private void saveLayout(long id) throws Exception {
        mvc.perform(put("/api/docks/" + id + "/layout").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"layoutJson\":\"" + LAYOUT.replace("\"", "\\\"") + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void aNewDockHasNoLayout() throws Exception {
        long id = createDock("Tom brygga");
        mvc.perform(get("/api/docks/" + id + "/layout").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layoutJson").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void theLayoutIsSavedAndReadBack() throws Exception {
        long id = createDock("Ritad brygga");
        saveLayout(id);
        mvc.perform(get("/api/docks/" + id + "/layout").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layoutJson").value(LAYOUT));
    }

    @Test
    void listingDocksDoesNotCarryTheLayout() throws Exception {
        long id = createDock("Listad brygga");
        saveLayout(id);
        mvc.perform(get("/api/docks").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].layoutJson").doesNotExist())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void theLayoutOfAnUnknownDockIs404() throws Exception {
        mvc.perform(get("/api/docks/999999/layout").with(asUser("admin:rw")))
                .andExpect(status().isNotFound());
    }

}
