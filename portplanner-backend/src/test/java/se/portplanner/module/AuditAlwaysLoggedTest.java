package se.portplanner.module;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import se.portplanner.repository.AuditLogRepository;
import se.portplanner.service.AuditService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.portplanner.module.TestUsers.asUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "portplanner.modules.audit.enabled=false")
class AuditAlwaysLoggedTest {

    @Autowired MockMvc mvc;
    @Autowired AuditService auditService;
    @Autowired AuditLogRepository auditRepo;

    @Test
    void auditIsWrittenEvenWhenModuleIsDisabled() {
        long before = auditRepo.count();
        auditService.log("TEST", "Test", 1L, "skrivs alltid");
        assertEquals(before + 1, auditRepo.count());
    }

    @Test
    void auditViewIsClosedWhenModuleIsDisabled() throws Exception {
        mvc.perform(get("/api/audit").with(asUser("admin:rw")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("MODULE_DISABLED"));
    }
}
