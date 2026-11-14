package se.portplanner.module;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
