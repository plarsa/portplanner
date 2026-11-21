package se.portplanner.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The landing page and the assets it loads must be readable without signing in. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicAssetsTest {

    @Autowired MockMvc mvc;

    @Test
    void theLandingPageIsPublic() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void theStyleLayerIsPublic() throws Exception {
        mvc.perform(get("/portplanner.css")).andExpect(status().isOk());
    }

    @Test
    void theWordmarkIsPublic() throws Exception {
        mvc.perform(get("/portplanner-mark.svg")).andExpect(status().isOk());
    }
}
