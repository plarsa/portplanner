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
import se.portplanner.exception.ConflictException;
import se.portplanner.model.AppUser;
import se.portplanner.model.Person;
import se.portplanner.model.RentalBooking;
import se.portplanner.model.RentalBookingStatus;
import se.portplanner.module.RoleGrant;
import se.portplanner.repository.AppUserRepository;
import se.portplanner.repository.PersonRepository;
import se.portplanner.repository.RentalBookingRepository;
import se.portplanner.repository.RentalObjectRepository;
import se.portplanner.service.RentalBookingService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        "spring.datasource.url=jdbc:h2:mem:merentals;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class MeRentalsApiTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired MockMvc mvc;
    @Autowired PersonRepository persons;
    @Autowired AppUserRepository users;
    @Autowired RentalObjectRepository objects;
    @Autowired RentalBookingRepository bookings;
    @Autowired RentalBookingService bookingService;

    private Long personId;
    private Long otherPersonId;
    private long objectId;
    private LocalDateTime start;

    @BeforeEach
    void setUp() throws Exception {
        bookings.deleteAll();
        objects.deleteAll();
        users.deleteAll();
        persons.deleteAll();

        personId = member("anna@hamn.se", "Anna", "Ek").getPerson().getId();
        otherPersonId = member("bo@hamn.se", "Bo", "Ek").getPerson().getId();
        objectId = createObject(false);
        // Two days out: comfortably in the future and inside maxDaysAhead.
        start = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0);
    }

    private AppUser member(String email, String first, String last) {
        var p = new Person();
        p.setFirstName(first);
        p.setLastName(last);
        p.setEmail(email);
        var person = persons.save(p);

        var u = new AppUser();
        // setEmail also sets the username: they are always the same on an account.
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRoles(List.of(RoleGrant.valueOf("member:rw")));
        u.setPerson(person);
        return users.save(u);
    }

    private long createObject(boolean requiresApproval) throws Exception {
        var body = mvc.perform(post("/api/rentals/objects").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content("""
                        {"name":"Dagseglare","bookable":true,"maxDurationHours":8,
                         "requiresApproval":%s,"maxDaysAhead":30}""".formatted(requiresApproval)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private RequestPostProcessor asMember() {
        return asUserNamed("anna@hamn.se", "member:rw");
    }

    private RequestPostProcessor asOtherMember() {
        return asUserNamed("bo@hamn.se", "member:rw");
    }

    private String bookingBody(LocalDateTime from, LocalDateTime to) {
        return """
            {"objectId":%d,"startAt":"%s","endAt":"%s"}""".formatted(objectId, from.format(ISO), to.format(ISO));
    }

    @Test
    void memberBooksAFreeInterval() throws Exception {
        mvc.perform(post("/api/me/rentals/bookings").with(asMember())
                        .contentType(APPLICATION_JSON).content(bookingBody(start, start.plusHours(4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.objectName").value("Dagseglare"));
    }

    @Test
    void anOverlappingBookingIsRejected() throws Exception {
        mvc.perform(post("/api/me/rentals/bookings").with(asMember())
                        .contentType(APPLICATION_JSON).content(bookingBody(start, start.plusHours(4))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/me/rentals/bookings").with(asOtherMember())
                        .contentType(APPLICATION_JSON).content(bookingBody(start.plusHours(2), start.plusHours(6))))
                .andExpect(status().isConflict());
    }

    @Test
    void availabilityDoesNotRevealWhoBooked() throws Exception {
        mvc.perform(post("/api/me/rentals/bookings").with(asMember())
                        .contentType(APPLICATION_JSON).content(bookingBody(start, start.plusHours(4))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/me/rentals/objects/" + objectId + "/availability")
                        .param("from", start.minusDays(1).format(ISO))
                        .param("to", start.plusDays(1).format(ISO))
                        .with(asMember()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startAt").exists())
                .andExpect(jsonPath("$[0].personName").doesNotExist())
                .andExpect(jsonPath("$[0].personId").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void aMemberCannotCancelAnotherMembersBooking() throws Exception {
        var body = mvc.perform(post("/api/me/rentals/bookings").with(asMember())
                        .contentType(APPLICATION_JSON).content(bookingBody(start, start.plusHours(4))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(delete("/api/me/rentals/bookings/" + id).with(asOtherMember()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancellingBeforeTheStartFreesTheTime() throws Exception {
        var body = mvc.perform(post("/api/me/rentals/bookings").with(asMember())
                        .contentType(APPLICATION_JSON).content(bookingBody(start, start.plusHours(4))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(delete("/api/me/rentals/bookings/" + id).with(asMember()))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/me/rentals/bookings").with(asOtherMember())
                        .contentType(APPLICATION_JSON).content(bookingBody(start, start.plusHours(4))))
                .andExpect(status().isCreated());
    }

    @Test
    void cancellingAfterTheStartIsRejected() throws Exception {
        // The rule validator refuses a start in the past, so this one is seeded directly.
        var b = new RentalBooking();
        b.setObject(objects.findById(objectId).orElseThrow());
        b.setPerson(persons.findById(personId).orElseThrow());
        b.setStartAt(LocalDateTime.now().minusHours(1));
        b.setEndAt(LocalDateTime.now().plusHours(1));
        b.setStatus(RentalBookingStatus.CONFIRMED);
        long id = bookings.save(b).getId();

        mvc.perform(delete("/api/me/rentals/bookings/" + id).with(asMember()))
                .andExpect(status().isConflict());
    }

    @Test
    void twoSimultaneousBookingsOnlyOneSucceeds() throws Exception {
        Callable<Object> book = () -> {
            try {
                return bookingService.book(objectId, personId, start, start.plusHours(4));
            } catch (Exception e) {
                return e;
            }
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var results = pool.invokeAll(List.of(book, book)).stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    return e;
                }
            }).toList();

            var failures = results.stream().filter(r -> r instanceof Exception).toList();
            assertEquals(1, results.size() - failures.size(), "exactly one booking must succeed");
            assertEquals(1, failures.size(), "the other must be refused");
            assertInstanceOf(ConflictException.class, failures.getFirst(),
                    "the loser must be refused with a conflict, not a database error");
        }
        assertEquals(1, bookings.count());
    }
}
