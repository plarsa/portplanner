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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "portplanner.modules.rentals.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:rentalobjects;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
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
