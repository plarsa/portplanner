package se.portplanner.module;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUser;

/** Profilen demo ska aktivera alla moduler och ge admin:rw skrivrätt i dem. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:demotest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class DemoProfileTest {

    @Autowired ModuleService modules;
    @Autowired MockMvc mvc;

    @Test
    void everyModuleIsEnabledAndAdminCanWriteInAll() {
        for (PortModule m : PortModule.values()) {
            assertEquals(true, modules.isEnabled(m.id()), m.id());
            assertEquals(AccessLevel.WRITE, modules.accessLevel(m.id(), Arrays.asList(RoleGrant.valueOf("admin:rw"))), m.id());
        }
    }

    @Test
    void demoSuperadminCanLogIn() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"password\":\"demo-lösenord-2026\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void auditIsWritableForAdminInDemo() throws Exception {
        mvc.perform(get("/api/audit").with(asUser("admin:rw"))).andExpect(status().isOk());
    }
}
