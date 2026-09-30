package se.portplanner.module;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModuleAllEnabledTest {
    @Autowired MockMvc mvc;

    @Test
        void storageWorksWhenEnabled() throws Exception {
        mvc.perform(get("/api/winter-seasons").with(user("u").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/modules/storage").with(user("u").roles("ADMIN")))
                .andExpect(jsonPath("$.settings.defaultSeasonStart").value("09-15"));
    }
}
