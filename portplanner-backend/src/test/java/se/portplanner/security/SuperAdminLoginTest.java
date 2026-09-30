package se.portplanner.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import se.portplanner.repository.AppUserRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "portplanner.iam.superadmins[0].username=admin",
        "portplanner.iam.superadmins[0].password=admin123",
        "portplanner.iam.superadmins[0].enabled=true",
        "portplanner.iam.superadmins[1].username=off@hamn.se",
        "portplanner.iam.superadmins[1].password=secret-off",
        "portplanner.iam.superadmins[1].enabled=false"
})
class SuperAdminLoginTest {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;

    private org.springframework.test.web.servlet.ResultActions login(String user, String pw) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"password\":\"" + pw + "\"}"));
    }

    @Test
    void configuredSuperadminCanLogInAndIsNotInTheDatabase() throws Exception {
        login("admin", "admin123").andExpect(status().isOk());
        assertTrue(users.findByUsername("admin").isEmpty());
    }

    @Test
    void wrongPasswordAndDisabledAccountsAreRejected() throws Exception {
        login("admin", "fel").andExpect(status().is4xxClientError());
        login("off@hamn.se", "secret-off").andExpect(status().is4xxClientError());
    }
}
