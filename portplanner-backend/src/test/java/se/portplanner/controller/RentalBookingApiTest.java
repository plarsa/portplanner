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
import se.portplanner.repository.RentalObjectRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        "spring.datasource.url=jdbc:h2:mem:rentalbookings;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class RentalBookingApiTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired MockMvc mvc;
    @Autowired PersonRepository persons;
    @Autowired AppUserRepository users;
    @Autowired RentalObjectRepository objects;
    @Autowired RentalBookingRepository bookings;

    private Long annaPersonId;
    private long approvalObjectId;
    private LocalDateTime start;

    @BeforeEach
    void setUp() throws Exception {
        bookings.deleteAll();
        objects.deleteAll();
        users.deleteAll();
        persons.deleteAll();

        annaPersonId = member("anna@hamn.se", "Anna").getPerson().getId();
        member("bo@hamn.se", "Bo");
        approvalObjectId = createObject("Trailer", true);
        start = LocalDateTime.now().plusDays(3).withHour(8).withMinute(0).withSecond(0).withNano(0);
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

    private long createObject(String name, boolean requiresApproval) throws Exception {
        var body = mvc.perform(post("/api/rentals/objects").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content("""
                        {"name":"%s","bookable":true,"maxDurationHours":24,
                         "requiresApproval":%s,"maxDaysAhead":60}""".formatted(name, requiresApproval)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private RequestPostProcessor anna() { return asUserNamed("anna@hamn.se", "member:rw"); }
    private RequestPostProcessor bo() { return asUserNamed("bo@hamn.se", "member:rw"); }

    private long memberBooks(RequestPostProcessor who, LocalDateTime from, LocalDateTime to) throws Exception {
        var body = mvc.perform(post("/api/me/rentals/bookings").with(who)
                        .contentType(APPLICATION_JSON).content("""
                        {"objectId":%d,"startAt":"%s","endAt":"%s"}"""
                        .formatted(approvalObjectId, from.format(ISO), to.format(ISO))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void anObjectWithApprovalCreatesAPendingBooking() throws Exception {
        mvc.perform(post("/api/me/rentals/bookings").with(anna())
                        .contentType(APPLICATION_JSON).content("""
                        {"objectId":%d,"startAt":"%s","endAt":"%s"}"""
                        .formatted(approvalObjectId, start.format(ISO), start.plusHours(4).format(ISO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void aPendingBookingHoldsTheTime() throws Exception {
        memberBooks(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/me/rentals/bookings").with(bo())
                        .contentType(APPLICATION_JSON).content("""
                        {"objectId":%d,"startAt":"%s","endAt":"%s"}"""
                        .formatted(approvalObjectId, start.format(ISO), start.plusHours(4).format(ISO))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectingFreesTheTime() throws Exception {
        long id = memberBooks(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/rentals/bookings/" + id + "/reject").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        memberBooks(bo(), start, start.plusHours(4));
    }

    @Test
    void approvingMakesItConfirmed() throws Exception {
        long id = memberBooks(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/rentals/bookings/" + id + "/approve").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void anAdminBookingIsConfirmedEvenWhenApprovalIsRequired() throws Exception {
        mvc.perform(post("/api/rentals/bookings").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content("""
                        {"objectId":%d,"personId":%d,"startAt":"%s","endAt":"%s"}"""
                        .formatted(approvalObjectId, annaPersonId, start.format(ISO),
                                start.plusHours(4).format(ISO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void anAdminCancelsAMembersBooking() throws Exception {
        long id = memberBooks(anna(), start, start.plusHours(4));
        mvc.perform(delete("/api/rentals/bookings/" + id).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/rentals/bookings/" + id + "/approve").with(asUser("admin:rw")))
                .andExpect(status().isConflict());
    }

    @Test
    void approvingAnAlreadyRejectedBookingIsRejected() throws Exception {
        long id = memberBooks(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/rentals/bookings/" + id + "/reject").with(asUser("admin:rw")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/rentals/bookings/" + id + "/approve").with(asUser("admin:rw")))
                .andExpect(status().isConflict());
    }

    @Test
    void adminListsBookingsForAnObject() throws Exception {
        memberBooks(anna(), start, start.plusHours(4));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/rentals/bookings")
                        .param("objectId", String.valueOf(approvalObjectId))
                        .param("from", start.minusDays(1).format(ISO))
                        .param("to", start.plusDays(1).format(ISO))
                        .with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].personId").value(annaPersonId))
                .andExpect(jsonPath("$[0].personName").value("Anna Ek"));
    }

    @Test
    void aMemberCannotApprove() throws Exception {
        long id = memberBooks(anna(), start, start.plusHours(4));
        mvc.perform(post("/api/rentals/bookings/" + id + "/approve").with(anna()))
                .andExpect(status().isForbidden());
    }
}
