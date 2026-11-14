package se.portplanner.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import se.portplanner.file.StoredFileRepository;
import se.portplanner.model.Person;
import se.portplanner.repository.AppUserRepository;
import se.portplanner.repository.PersonRepository;
import se.portplanner.repository.RentalBookingRepository;
import se.portplanner.repository.RentalObjectRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "portplanner.modules.rentals.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:rentaldeletion;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class RentalDeletionApiTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired MockMvc mvc;
    @Autowired PersonRepository persons;
    @Autowired AppUserRepository users;
    @Autowired RentalObjectRepository objects;
    @Autowired RentalBookingRepository bookings;
    @Autowired StoredFileRepository storedFiles;

    private long personId;
    private long objectId;
    private LocalDateTime start;

    @BeforeEach
    void setUp() throws Exception {
        bookings.deleteAll();
        objects.deleteAll();
        storedFiles.deleteAll();
        users.deleteAll();
        persons.deleteAll();

        var p = new Person();
        p.setFirstName("Anna");
        p.setLastName("Ek");
        p.setEmail("anna@hamn.se");
        personId = persons.save(p).getId();
        objectId = createObject();
        start = LocalDateTime.now().plusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0);
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

    /** Booked through the admin API, so the person needs no account of their own. */
    private void bookForPerson() throws Exception {
        mvc.perform(post("/api/rentals/bookings").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content("""
                        {"objectId":%d,"personId":%d,"startAt":"%s","endAt":"%s"}"""
                        .formatted(objectId, personId, start.format(ISO), start.plusHours(4).format(ISO))))
                .andExpect(status().isCreated());
    }

    @Test
    void deletingAPersonWithRentalBookingsGivesConflict() throws Exception {
        bookForPerson();
        mvc.perform(delete("/api/persons/" + personId).with(asUser("admin:rw")))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingAPersonWithoutRentalBookingsStillWorks() throws Exception {
        mvc.perform(delete("/api/persons/" + personId).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
    }

    @Test
    void theRefusalDoesNotAskForCancellationThatIsAlreadyDone() throws Exception {
        bookForPerson();
        var bookingId = bookings.findAll().getFirst().getId();
        mvc.perform(delete("/api/rentals/bookings/" + bookingId).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        // The booking is cancelled now, but the row remains and the person still cannot be
        // deleted. The message must say that rather than ask for another cancellation.
        mvc.perform(delete("/api/persons/" + personId).with(asUser("admin:rw")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("rental history")));
    }

    @Test
    void deletingAnObjectWithBookingsGivesConflict() throws Exception {
        bookForPerson();
        mvc.perform(delete("/api/rentals/objects/" + objectId).with(asUser("admin:rw")))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingAnObjectWithoutBookingsRemovesTheImage() throws Exception {
        var png = new MockMultipartFile("file", "o.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/rentals/objects/" + objectId + "/image").file(png).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        assertEquals(1, storedFiles.count());

        mvc.perform(delete("/api/rentals/objects/" + objectId).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        assertEquals(0, storedFiles.count(), "the object's image must not be left in stored_files");
    }
}
