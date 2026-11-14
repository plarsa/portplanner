package se.portplanner.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import se.portplanner.model.AppUser;
import se.portplanner.model.Person;
import se.portplanner.module.RoleGrant;
import se.portplanner.repository.AppUserRepository;
import se.portplanner.repository.PersonRepository;
import se.portplanner.repository.RentalBookingRepository;
import se.portplanner.repository.RentalFaultReportRepository;
import se.portplanner.repository.RentalObjectRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUser;
import static se.portplanner.module.TestUsers.asUserNamed;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "portplanner.modules.rentals.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:rentalreturn;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class RentalReturnApiTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired MockMvc mvc;
    @Autowired PersonRepository persons;
    @Autowired AppUserRepository users;
    @Autowired RentalObjectRepository objects;
    @Autowired RentalBookingRepository bookings;
    @Autowired RentalFaultReportRepository faults;

    private Long annaPersonId;
    private long objectId;
    private LocalDateTime start;

    @BeforeEach
    void setUp() throws Exception {
        faults.deleteAll();
        bookings.deleteAll();
        objects.deleteAll();
        users.deleteAll();
        persons.deleteAll();

        annaPersonId = member("anna@hamn.se", "Anna").getPerson().getId();
        member("bo@hamn.se", "Bo");
        objectId = createObject();
        start = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0);
    }

    private AppUser member(String email, String first) {
        var p = new Person();
        p.setFirstName(first);
        p.setLastName("Ek");
        p.setEmail(email);
        var person = persons.save(p);
        var u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRoles(List.of(RoleGrant.valueOf("member:rw")));
        u.setPerson(person);
        return users.save(u);
    }

    private long createObject() throws Exception {
        var body = mvc.perform(post("/api/rentals/objects").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content("""
                        {"name":"Dagseglare","bookable":true,"maxDurationHours":24,
                         "requiresApproval":false,"maxDaysAhead":60}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private RequestPostProcessor anna() { return asUserNamed("anna@hamn.se", "member:rw"); }
    private RequestPostProcessor bo() { return asUserNamed("bo@hamn.se", "member:rw"); }

    private long book(RequestPostProcessor who, LocalDateTime from, LocalDateTime to) throws Exception {
        var body = mvc.perform(post("/api/me/rentals/bookings").with(who)
                        .contentType(APPLICATION_JSON).content("""
                        {"objectId":%d,"startAt":"%s","endAt":"%s"}"""
                        .formatted(objectId, from.format(ISO), to.format(ISO))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void aMemberReturnsWithAFaultReport() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(anna())
                        .contentType(APPLICATION_JSON)
                        .content("{\"faultDescription\":\"Storseglet är trasigt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        mvc.perform(get("/api/rentals/fault-reports").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Storseglet är trasigt"))
                .andExpect(jsonPath("$[0].objectName").value("Dagseglare"))
                .andExpect(jsonPath("$[0].resolved").value(false));
    }

    @Test
    void returningWithoutAFaultReportIsAllowed() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(anna())
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
        mvc.perform(get("/api/rentals/fault-reports").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anAdminReturnsOnBehalfOfAMember() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/rentals/bookings/" + id + "/return").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content("{\"faultDescription\":\"Repet saknas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
        mvc.perform(get("/api/rentals/fault-reports").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aMemberCannotReturnAnotherMembersBooking() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(bo())
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aFaultReportDoesNotBlockLaterBookings() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(anna())
                        .contentType(APPLICATION_JSON).content("{\"faultDescription\":\"Skadad fender\"}"))
                .andExpect(status().isOk());
        // REN-8: the report does not take the object out of service.
        book(bo(), start.plusDays(1), start.plusDays(1).plusHours(4));
    }

    @Test
    void resolvingAFaultReportRemovesItFromTheOpenList() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(anna())
                        .contentType(APPLICATION_JSON).content("{\"faultDescription\":\"Trasig vinsch\"}"))
                .andExpect(status().isOk());
        var body = mvc.perform(get("/api/rentals/fault-reports").with(asUser("admin:rw")))
                .andReturn().getResponse().getContentAsString();
        long faultId = ((Number) JsonPath.read(body, "$[0].id")).longValue();

        mvc.perform(post("/api/rentals/fault-reports/" + faultId + "/resolve").with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/rentals/fault-reports").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/rentals/fault-reports").param("resolved", "true").with(asUser("admin:rw")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void returningTwiceIsRejected() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(anna())
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(anna())
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void faultReportsCanBeFilteredByObject() throws Exception {
        long id = book(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings/" + id + "/return").with(anna())
                        .contentType(APPLICATION_JSON).content("{\"faultDescription\":\"Lös bom\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/rentals/fault-reports")
                        .param("objectId", String.valueOf(objectId)).with(asUser("admin:rw")))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
