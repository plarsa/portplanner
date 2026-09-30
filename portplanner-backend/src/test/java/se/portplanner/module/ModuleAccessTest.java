package se.portplanner.module;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static se.portplanner.module.TestUsers.asUser;
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
        "portplanner.modules.audit.access=admin:r,superadmin:r",
        "portplanner.modules.import-export.access=admin:r,superadmin:rw"
})
class ModuleAccessTest {
    @Autowired MockMvc mvc;

    @Test
        void disabledModuleGives403() throws Exception {
        mvc.perform(get("/api/winter-seasons").with(asUser("admin:rw")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"))
                .andExpect(jsonPath("$.module").value("storage"));
    }

    @Test
        void readOnlyRoleCannotWrite() throws Exception {
        mvc.perform(get("/api/export/docks").with(asUser("admin:rw"))).andExpect(status().isOk());
        mvc.perform(post("/api/import/docks").with(asUser("admin:rw")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_ROLE_FORBIDDEN"));
    }

    @Test
        void memberHasNoAuditAccess() throws Exception {
        mvc.perform(get("/api/audit").with(asUser("member:rw")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_ROLE_FORBIDDEN"));
    }

    @Test
        void modulesListIsPersonal() throws Exception {
        mvc.perform(get("/api/modules").with(asUser("member:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[?(@.id=='storage')].enabled").value(false))
                .andExpect(jsonPath("$.modules[?(@.id=='audit')].accessible").value(false))
                .andExpect(jsonPath("$.modules[?(@.id=='member-portal')].accessible").value(true))
                .andExpect(jsonPath("$.modules[?(@.id=='audit')].settings").isEmpty());
    }

    @Test
        void dashboardStillWorksAndUnknownModuleIs404() throws Exception {
        mvc.perform(get("/api/dashboard/badges").with(asUser("admin:rw"))).andExpect(status().isOk());
        mvc.perform(get("/api/modules/nope").with(asUser("admin:rw"))).andExpect(status().isNotFound());
    }
}
