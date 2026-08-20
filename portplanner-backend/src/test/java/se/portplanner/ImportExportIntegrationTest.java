package se.portplanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.portplanner.service.DockService;
import se.portplanner.repository.DockRepository;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportExportIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DockRepository dockRepository;
    @Autowired DockService dockService;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("token").asText();
    }

    @AfterEach
    void cleanup() {
        Set<String> testDocks = Set.of("badhusb", "ytterbyvik");
        dockRepository.findAll().stream()
                .filter(d -> testDocks.contains(d.getName()))
                .forEach(d -> dockService.delete(d.getId()));
    }

    @Test
    void importDocksAndDeleteThem() throws Exception {
        String json = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/bryggor-import.json"))
                        .readAllBytes(),
                StandardCharsets.UTF_8);

        // 1. Import bryggor.json – should create 2 docks and 61 slips
        mvc.perform(post("/api/import/docks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docksCreated").value(2))
                .andExpect(jsonPath("$.slipsCreated").value(61))
                .andExpect(jsonPath("$.warnings").isEmpty());

        // 2. Verify both docks are visible in the docks list
        mvc.perform(get("/api/docks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='badhusb')]").exists())
                .andExpect(jsonPath("$[?(@.name=='ytterbyvik')]").exists());

        // 3. Fetch all dock IDs and delete each one
        String docksJson = mvc.perform(get("/api/docks")
                .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();

        for (JsonNode dock : objectMapper.readTree(docksJson)) {
            mvc.perform(delete("/api/docks/" + dock.get("id").asLong())
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        // 4. Verify no docks remain
        mvc.perform(get("/api/docks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void importIsIdempotentForExistingDocks() throws Exception {
        String json = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/bryggor-import.json"))
                        .readAllBytes(),
                StandardCharsets.UTF_8);

        // First import
        mvc.perform(post("/api/import/docks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docksCreated").value(2));

        // Second import – both docks already exist, no slips added
        mvc.perform(post("/api/import/docks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docksCreated").value(0))
                .andExpect(jsonPath("$.slipsCreated").value(0));
    }
}
