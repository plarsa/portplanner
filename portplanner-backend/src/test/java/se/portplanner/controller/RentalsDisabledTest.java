package se.portplanner.controller;

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

/** The module is off in application.yml, so its API must answer MODULE_DISABLED rather than 404. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
