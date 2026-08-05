package com.kleos.transfers.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kleos.transfers.repository.PlayerRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlayerControllerIntegrationTest {

    private static final String PLAYERS_PATH = "/api/v1/players";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void clearPlayers() {
        playerRepository.deleteAll();
    }

    @Test
    void createsReadsAndUpdatesPlayerIdentity() throws Exception {
        MvcResult createResult = mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPlayerRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Test Player"))
                .andExpect(jsonPath("$.nationality").value("ENG"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(PLAYERS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.primaryPosition").value("CM"));

        mockMvc.perform(put(PLAYERS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedPlayerRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Player"))
                .andExpect(jsonPath("$.heightCm").value(181))
                .andExpect(jsonPath("$.preferredFoot").value("LEFT"))
                .andExpect(jsonPath("$.nationality").value("NED"));

        mockMvc.perform(get(PLAYERS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Updated Player"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void acceptsLowercaseNationalityAndNormalizesIt() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Case Player",
                                  "dateOfBirth": "2000-01-01",
                                  "nationality": "ger",
                                  "heightCm": 180,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "ST"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nationality").value("GER"));
    }

    @Test
    void rejectsInvalidPlayerRequest() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "A",
                                  "dateOfBirth": "2999-01-01",
                                  "nationality": "XXX",
                                  "heightCm": 139,
                                  "preferredFoot": null,
                                  "primaryPosition": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(6));
    }

    @Test
    void rejectsMalformedJsonWithBadRequest() throws Exception {
        mockMvc.perform(post(PLAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Broken Player",
                                  "preferredFoot": "NOT_A_FOOT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void rejectsInvalidUuidPathWithBadRequest() throws Exception {
        mockMvc.perform(get(PLAYERS_PATH + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsNotFoundForUnknownPlayer() throws Exception {
        mockMvc.perform(get(PLAYERS_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }

    private String validPlayerRequest() {
        return """
                {
                  "fullName": "Test Player",
                  "dateOfBirth": "2000-01-01",
                  "nationality": "ENG",
                  "heightCm": 180,
                  "preferredFoot": "RIGHT",
                  "primaryPosition": "CM"
                }
                """;
    }

    private String updatedPlayerRequest() {
        return """
                {
                  "fullName": "Updated Player",
                  "dateOfBirth": "2000-01-01",
                  "nationality": "NED",
                  "heightCm": 181,
                  "preferredFoot": "LEFT",
                  "primaryPosition": "CAM"
                }
                """;
    }
}
