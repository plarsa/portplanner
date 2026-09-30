package se.portplanner.module;

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
@TestPropertySource(properties = {
        "portplanner.modules.storage.enabled=false",
        "portplanner.modules.audit.access.ADMIN=read",
        "portplanner.modules.audit.access.HARBOUR_MASTER=read",
        "portplanner.modules.import-export.access.ADMIN=read"
})
class ModuleAccessTest {
    @Autowired MockMvc mvc;

    @Test
        void disabledModuleGives403() throws Exception {
        mvc.perform(get("/api/winter-seasons").with(user("u").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.module").value("storage"));
    }

    @Test
        void readOnlyRoleCannotWrite() throws Exception {
        mvc.perform(get("/api/export/docks").with(user("u").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/api/import/docks").with(user("u").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_ROLE_FORBIDDEN"));
    }

    @Test
        void memberHasNoAuditAccess() throws Exception {
        mvc.perform(get("/api/audit").with(user("u").roles("MEMBER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_ROLE_FORBIDDEN"));
    }

    @Test
        void modulesListIsPersonal() throws Exception {
        mvc.perform(get("/api/modules").with(user("u").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[?(@.id=='storage')].enabled").value(false))
                .andExpect(jsonPath("$.modules[?(@.id=='audit')].accessible").value(false))
                .andExpect(jsonPath("$.modules[?(@.id=='member-portal')].accessible").value(true))
                .andExpect(jsonPath("$.modules[?(@.id=='audit')].settings").isEmpty());
    }

    @Test
        void dashboardStillWorksAndUnknownModuleIs404() throws Exception {
        mvc.perform(get("/api/dashboard/badges").with(user("u").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/modules/nope").with(user("u").roles("ADMIN"))).andExpect(status().isNotFound());
    }
}
