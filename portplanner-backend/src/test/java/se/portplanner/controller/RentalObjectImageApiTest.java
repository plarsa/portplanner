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
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.file.FileStore;
import se.portplanner.file.StoredFileRepository;
import se.portplanner.repository.RentalObjectRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static se.portplanner.module.TestUsers.asUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "portplanner.modules.rentals.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:rentalimages;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class RentalObjectImageApiTest {

    @Autowired MockMvc mvc;
    @Autowired StoredFileRepository storedFiles;
    @Autowired FileStore fileStore;
    @Autowired RentalObjectRepository objects;

    /** MockMvc requests commit, and the whole class shares one in-memory database, so the
     *  absolute counts below only mean anything on a clean slate. Objects go first: they
     *  hold the foreign key into stored_files. */
    @BeforeEach
    void clearRentals() {
        objects.deleteAll();
        storedFiles.deleteAll();
    }

    private static final String TRAILER = """
        {"name":"Upptagningstrailer","bookable":true,"maxDurationHours":24,
         "requiresApproval":false,"maxDaysAhead":30}""";

    private long createTrailer() throws Exception {
        var body = mvc.perform(post("/api/rentals/objects").with(asUser("admin:rw"))
                        .contentType(APPLICATION_JSON).content(TRAILER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static MockMultipartFile png(byte... bytes) {
        return new MockMultipartFile("file", "trailer.png", "image/png", bytes);
    }

    @Test
    void uploadThenFetchReturnsTheBytes() throws Exception {
        long id = createTrailer();
        mvc.perform(multipart("/api/rentals/objects/" + id + "/image").file(png((byte) 1, (byte) 2, (byte) 3))
                        .with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/rentals/objects/" + id + "/image").with(asUser("admin:rw")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
        mvc.perform(get("/api/rentals/objects/" + id).with(asUser("admin:rw")))
                .andExpect(jsonPath("$.hasImage").value(true));
    }

    @Test
    void anUnsupportedTypeIsRejected() throws Exception {
        long id = createTrailer();
        var text = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[]{1});
        mvc.perform(multipart("/api/rentals/objects/" + id + "/image").file(text).with(asUser("admin:rw")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aFileOverFiveMegabytesIsRejected() throws Exception {
        long id = createTrailer();
        var big = png(new byte[5 * 1024 * 1024 + 1]);
        mvc.perform(multipart("/api/rentals/objects/" + id + "/image").file(big).with(asUser("admin:rw")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replacingTheImageRemovesTheOldFile() throws Exception {
        long id = createTrailer();
        mvc.perform(multipart("/api/rentals/objects/" + id + "/image").file(png((byte) 1)).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        var firstKey = storedFiles.findAll().getFirst().getStorageKey();

        mvc.perform(multipart("/api/rentals/objects/" + id + "/image").file(png((byte) 2)).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());

        assertEquals(1, storedFiles.count(), "the replaced row must not be left behind");
        assertThrows(ResourceNotFoundException.class, () -> fileStore.get(firstKey),
                "the replaced file must not be left in the store");
    }

    @Test
    void fetchingAMissingImageGives404() throws Exception {
        long id = createTrailer();
        mvc.perform(get("/api/rentals/objects/" + id + "/image").with(asUser("admin:rw")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingTheImageRemovesItFromTheStore() throws Exception {
        long id = createTrailer();
        mvc.perform(multipart("/api/rentals/objects/" + id + "/image").file(png((byte) 1)).with(asUser("admin:rw")))
                .andExpect(status().isNoContent());
        var key = storedFiles.findAll().getFirst().getStorageKey();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/rentals/objects/" + id + "/image").with(asUser("admin:rw")))
                .andExpect(status().isNoContent());

        assertEquals(0, storedFiles.count());
        assertThrows(ResourceNotFoundException.class, () -> fileStore.get(key));
    }
}
