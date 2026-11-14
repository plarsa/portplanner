package se.portplanner.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import se.portplanner.model.AppUser;
import se.portplanner.model.LoginMethod;
import se.portplanner.model.UserStatus;
import se.portplanner.module.RoleGrant;
import se.portplanner.repository.AppUserRepository;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountLoginTest {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    private AppUser save(String email, UserStatus status, Set<LoginMethod> methods, String... grants) {
        var u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("secret-password"));
        u.setStatus(status);
        u.setLoginMethods(methods);
        u.setRoles(java.util.Arrays.stream(grants).map(RoleGrant::valueOf).toList());
        return users.save(u);
    }

    private ResultActions login(String user, String pw) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"password\":\"" + pw + "\"}"));
    }

    @Test
    void usernameFollowsEmail() {
        var u = save("Anna@Hamn.se", UserStatus.ACTIVE, Set.of(LoginMethod.PASSWORD), "admin:rw");
        assertEquals("Anna@Hamn.se", u.getUsername());
        u.setEmail("anna@nyhamn.se");
        assertEquals("anna@nyhamn.se", users.save(u).getUsername());
    }

    @Test
    void sameRoleTwiceKeepsTheHighestLevel() {
        var u = save("a@hamn.se", UserStatus.ACTIVE, Set.of(LoginMethod.PASSWORD), "admin:r", "admin:rw", "member:r");
        var stored = users.findById(u.getId()).orElseThrow();
        assertEquals(Set.of(RoleGrant.valueOf("admin:rw"), RoleGrant.valueOf("member:r")), stored.getRoles());
    }

    @Test
    void accountWithGrantsLogsInAndMeShowsThem() throws Exception {
        save("anna@hamn.se", UserStatus.ACTIVE, Set.of(LoginMethod.PASSWORD), "admin:rw", "audit_master:r");
        var result = login("anna@hamn.se", "secret-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anna@hamn.se"))
                .andExpect(jsonPath("$.roles[0]").value("admin:rw"))
                .andExpect(jsonPath("$.roles[1]").value("audit_master:r"))
                .andExpect(jsonPath("$.loginMethods[0]").value("PASSWORD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        var cookie = result.getResponse().getCookie("SESSION");
        mvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2));
        assertNotNull(users.findByUsername("anna@hamn.se").orElseThrow().getLastLoginAt());
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        save("off@hamn.se", UserStatus.DISABLED, Set.of(LoginMethod.PASSWORD), "admin:rw");
        login("off@hamn.se", "secret-password").andExpect(status().is4xxClientError());
    }

    @Test
    void accountWithoutPasswordMethodCannotUsePassword() throws Exception {
        save("nopw@hamn.se", UserStatus.ACTIVE, Set.of(), "admin:rw");
        login("nopw@hamn.se", "secret-password").andExpect(status().is4xxClientError());
    }

    @Test
    void wrongPasswordAndUnknownAccountLookTheSame() throws Exception {
        save("x@hamn.se", UserStatus.ACTIVE, Set.of(LoginMethod.PASSWORD), "admin:rw");
        int a = login("x@hamn.se", "fel").andReturn().getResponse().getStatus();
        int b = login("finns-inte@hamn.se", "fel").andReturn().getResponse().getStatus();
        assertEquals(a, b);
    }

    @Test
    void configuredSuperadminMeHasNoDatabaseRow() throws Exception {
        login("admin", "admin123").andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("superadmin:rw"))
                .andExpect(jsonPath("$.email").doesNotExist())
                ;
        assertTrue(users.findByUsername("admin").isEmpty());
        assertEquals(List.of(), users.findAll());
    }
}
