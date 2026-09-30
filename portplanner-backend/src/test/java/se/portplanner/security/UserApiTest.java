package se.portplanner.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import se.portplanner.model.Person;
import se.portplanner.model.UserStatus;
import se.portplanner.repository.AppUserRepository;
import se.portplanner.repository.AuditLogRepository;
import se.portplanner.repository.PersonRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUserNamed;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "portplanner.iam.superadmins[0].username=admin",
        "portplanner.iam.superadmins[0].password=admin123",
        "portplanner.iam.superadmins[0].enabled=true",
        "portplanner.modules.audit.enabled=false",
        "portplanner.modules.audit.access=admin:r,audit_master:r,superadmin:r"
})
class UserApiTest {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PersonRepository persons;
    @Autowired AuditLogRepository auditLog;
    @Autowired FindByIndexNameSessionRepository<?> sessions;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    private static String body(String email, String roles, Long personId) {
        return "{\"email\":\"" + email + "\",\"roles\":[" + roles + "]"
                + (personId == null ? "" : ",\"personId\":" + personId) + "}";
    }

    private ResultActions post(String url, String content, org.springframework.test.web.servlet.request.RequestPostProcessor who) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(content).with(who));
    }

    private Long createAccount(String email, String roles) throws Exception {
        var res = post("/api/users", body(email, roles, null), asUserNamed("boss", "superadmin:rw"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("user").get("id").asLong();
    }

    private Person person() {
        var p = new Person();
        p.setFirstName("Pia");
        p.setLastName("Person");
        p.setEmail("pia-" + System.nanoTime() + "@x.se");
        return persons.save(p);
    }

    @Test
    void createReturnsOneTimePasswordAndSetsUsernameFromEmail() throws Exception {
        long before = auditLog.count();
        var res = post("/api/users", body("anna@hamn.se", "\"admin:rw\",\"audit_master:r\"", null),
                asUserNamed("boss", "admin:rw"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("anna@hamn.se"))
                .andExpect(jsonPath("$.user.status").value("INVITED"))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andExpect(jsonPath("$.user.loginMethods[0]").value("PASSWORD"))
                .andExpect(jsonPath("$.user.roles[0]").value("admin:rw"))
                .andExpect(jsonPath("$.temporaryPassword").isString())
                .andReturn().getResponse().getContentAsString();
        assertFalse(res.contains("passwordHash"));
        assertEquals(before + 1, auditLog.count(), "audit skrivs även när modulen audit är avstängd");
        // Engångslösenordet fungerar för inloggning.
        var pw = json.readTree(res).get("temporaryPassword").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"anna@hamn.se\",\"password\":\"" + pw + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void readOnlyAdminListsButCannotChange() throws Exception {
        createAccount("a@hamn.se", "\"admin:rw\"");
        mvc.perform(get("/api/users").with(asUserNamed("r", "admin:r")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='admin')].readOnly").value(true))
                .andExpect(jsonPath("$[?(@.username=='a@hamn.se')]").exists());
        post("/api/users", body("b@hamn.se", "\"member:rw\"", null), asUserNamed("r", "admin:r"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_ACCESS_FORBIDDEN"));
        mvc.perform(get("/api/roles").with(asUserNamed("r", "admin:r"))).andExpect(status().isForbidden());
    }

    @Test
    void memberAndOtherRolesGet403() throws Exception {
        for (var who : new String[] {"member:rw", "audit_master:rw"}) {
            mvc.perform(get("/api/users").with(asUserNamed("m", who))).andExpect(status().isForbidden());
            post("/api/users", body("z@hamn.se", "\"admin:rw\"", null), asUserNamed("m", who))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminCannotAssignSuperadmin() throws Exception {
        post("/api/users", body("x@hamn.se", "\"superadmin:rw\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ROLE_ASSIGNMENT_FORBIDDEN"));
        post("/api/users", body("x@hamn.se", "\"superadmin:rw\"", null), asUserNamed("s", "superadmin:rw"))
                .andExpect(status().isForbidden());
        assertTrue(users.findByUsername("x@hamn.se").isEmpty());
    }

    @Test
    void unknownRoleAndBadFormatAreBadRequests() throws Exception {
        post("/api/users", body("x@hamn.se", "\"finns_inte:r\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().isBadRequest());
        post("/api/users", body("x@hamn.se", "\"admin\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().isBadRequest());
        post("/api/users", body("inte-en-epost", "\"admin:rw\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newRoleFromModuleConfigIsAssignable() throws Exception {
        mvc.perform(get("/api/roles").with(asUserNamed("a", "admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@=='audit_master')]").exists())
                .andExpect(jsonPath("$[?(@=='superadmin')]").doesNotExist());
        post("/api/users", body("am@hamn.se", "\"audit_master:r\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateEmailAndSuperadminUsernameConflict() throws Exception {
        createAccount("dup@hamn.se", "\"admin:r\"");
        post("/api/users", body("DUP@hamn.se", "\"admin:r\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().isConflict());
        post("/api/users", body("admin", "\"admin:r\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().is4xxClientError());
        assertEquals(1, users.count());
    }

    @Test
    void memberRoleRequiresAPersonWhoHasNoAccount() throws Exception {
        post("/api/users", body("m1@hamn.se", "\"member:rw\"", null), asUserNamed("a", "admin:rw"))
                .andExpect(status().isBadRequest());
        var p = person();
        post("/api/users", body("m1@hamn.se", "\"member:rw\"", p.getId()), asUserNamed("a", "admin:rw"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.personId").value(p.getId()));
        post("/api/users", body("m2@hamn.se", "\"member:rw\"", p.getId()), asUserNamed("a", "admin:rw"))
                .andExpect(status().isConflict());
    }

    @Test
    void personWithAccountCannotBeDeleted() throws Exception {
        var p = person();
        post("/api/users", body("m1@hamn.se", "\"member:rw\"", p.getId()), asUserNamed("a", "admin:rw"))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/persons/" + p.getId()).with(asUserNamed("a", "admin:rw")))
                .andExpect(status().isConflict());
    }

    @Test
    void updateChangesEmailUsernameAndRolesAndIsAudited() throws Exception {
        long id = createAccount("old@hamn.se", "\"member:r\"".replace("member:r", "admin:r"));
        long before = auditLog.count();
        mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("new@hamn.se", "\"admin:rw\",\"audit_master:r\"", null))
                .with(asUserNamed("boss", "superadmin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new@hamn.se"))
                .andExpect(jsonPath("$.roles.length()").value(2));
        assertEquals(before + 2, auditLog.count()); // e-post + roller
    }

    @Test
    void adminCannotChangeOwnRolesButCanChangeOthers() throws Exception {
        long me = createAccount("me@hamn.se", "\"admin:rw\"");
        mvc.perform(put("/api/users/" + me).contentType(MediaType.APPLICATION_JSON)
                .content(body("me@hamn.se", "\"admin:rw\",\"audit_master:r\"", null))
                .with(asUserNamed("me@hamn.se", "admin:rw")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SELF_MODIFICATION"));
        long other = createAccount("other@hamn.se", "\"admin:r\"");
        mvc.perform(put("/api/users/" + other).contentType(MediaType.APPLICATION_JSON)
                .content(body("other@hamn.se", "\"admin:rw\"", null))
                .with(asUserNamed("me@hamn.se", "admin:rw")))
                .andExpect(status().isOk());
    }

    @Test
    void emptyLoginMethodListIsRejected() throws Exception {
        post("/api/users", "{\"email\":\"e@hamn.se\",\"roles\":[\"admin:r\"],\"loginMethods\":[]}",
                asUserNamed("a", "admin:rw")).andExpect(status().isBadRequest());
        post("/api/users", "{\"email\":\"e@hamn.se\",\"roles\":[\"admin:r\"],\"loginMethods\":[\"GOOGLE\"]}",
                asUserNamed("a", "admin:rw")).andExpect(status().isBadRequest());
    }

    @Test
    void disableBlocksLoginEndsSessionsAndEnableRestores() throws Exception {
        var created = post("/api/users", body("d@hamn.se", "\"admin:rw\"", null), asUserNamed("boss", "superadmin:rw"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("user").get("id").asLong();
        var pw = json.readTree(created).get("temporaryPassword").asText();
        var loginBody = "{\"username\":\"d@hamn.se\",\"password\":\"" + pw + "\"}";

        var login = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody)).andExpect(status().isOk()).andReturn();
        var cookie = login.getResponse().getCookie("SESSION");
        assertNotNull(cookie, "sessionscookie ska sättas vid inloggning");
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk());
        assertFalse(sessions.findByPrincipalName("d@hamn.se").isEmpty(), "sessionen ska vara indexerad på användarnamn");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users/" + id + "/disable")
                .with(asUserNamed("boss", "superadmin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
        assertTrue(sessions.findByPrincipalName("d@hamn.se").isEmpty(), "sessionerna ska vara avslutade");
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().is4xxClientError());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody)).andExpect(status().is4xxClientError());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users/" + id + "/enable")
                .with(asUserNamed("boss", "superadmin:rw")))
                .andExpect(jsonPath("$.status").value(UserStatus.INVITED.name()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody)).andExpect(status().isOk());
    }

    @Test
    void roleChangeEndsTheAccountsSessions() throws Exception {
        var created = post("/api/users", body("s@hamn.se", "\"admin:r\"", null), asUserNamed("boss", "superadmin:rw"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("user").get("id").asLong();
        var pw = json.readTree(created).get("temporaryPassword").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"s@hamn.se\",\"password\":\"" + pw + "\"}")).andExpect(status().isOk());
        assertFalse(sessions.findByPrincipalName("s@hamn.se").isEmpty());
        mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("s@hamn.se", "\"admin:rw\"", null)).with(asUserNamed("boss", "superadmin:rw")))
                .andExpect(status().isOk());
        assertTrue(sessions.findByPrincipalName("s@hamn.se").isEmpty());
    }

    @Test
    void disablingYourselfIsForbidden() throws Exception {
        long id = createAccount("self@hamn.se", "\"admin:rw\"");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users/" + id + "/disable")
                .with(asUserNamed("self@hamn.se", "admin:rw")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SELF_MODIFICATION"));
    }
}
