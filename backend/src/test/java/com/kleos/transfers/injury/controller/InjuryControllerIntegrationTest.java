package com.kleos.transfers.injury.controller;

import com.kleos.transfers.common.test.AbstractPostgresIntegrationTest;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kleos.transfers.common.test.DatabaseCleaner;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class InjuryControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String INJURIES_PATH = "/api/v1/injuries";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearData() {
        DatabaseCleaner.clearAll(jdbcTemplate);
    }

    @Test
    void createsReadsAndClosesInjurySpell() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");

        MvcResult createResult = mockMvc.perform(post(INJURIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "injuryType": "Shoulder dislocation",
                                  "severity": "MODERATE",
                                  "startDate": "2023-11-15"
                                }
                                """.formatted(playerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Jude Bellingham"))
                .andExpect(jsonPath("$.severity").value("MODERATE"))
                .andExpect(jsonPath("$.ongoing").value(true))
                .andExpect(jsonPath("$.daysOut").value(nullValue()))
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(INJURIES_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(put(INJURIES_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(injuryJson(playerId, "Shoulder dislocation", "MODERATE", "2023-11-15", "2023-11-24")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ongoing").value(false))
                .andExpect(jsonPath("$.daysOut").value(10));
    }

    @Test
    void acceptsSameDayInjurySpell() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");

        mockMvc.perform(post(INJURIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(injuryJson(playerId, "Knock", "MINOR", "2024-03-01", "2024-03-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.daysOut").value(1));
    }

    @Test
    void rejectsEndDateBeforeStartDate() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");

        mockMvc.perform(post(INJURIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(injuryJson(playerId, "Knock", "MINOR", "2024-03-05", "2024-03-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownSeverity() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");

        mockMvc.perform(post(INJURIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(injuryJson(playerId, "Knock", "CATASTROPHIC", "2024-03-01", "2024-03-05")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateInjurySpell() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        String body = injuryJson(playerId, "Hamstring strain", "MODERATE", "2024-03-01", "2024-04-01");

        mockMvc.perform(post(INJURIES_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(INJURIES_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void softDeletesAndAllowsRecreate() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        String body = injuryJson(playerId, "Hamstring strain", "MODERATE", "2024-03-01", "2024-04-01");

        MvcResult createResult = mockMvc.perform(post(INJURIES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));
        mockMvc.perform(delete(INJURIES_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(INJURIES_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void importsInjuriesInBulkAndSkipsDuplicates() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        String first = injuryJson(playerId, "Hamstring strain", "MODERATE", "2024-03-01", "2024-04-01");
        String second = injuryJson(playerId, "Ankle sprain", "MINOR", "2024-09-10", "2024-09-20");

        mockMvc.perform(post(INJURIES_PATH + "/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [%s, %s, %s]}
                                """.formatted(first, first, second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }

    private UUID createPlayer(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "dateOfBirth": "2003-06-29",
                                  "nationality": "ENG",
                                  "heightCm": 186,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "CM"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private String injuryJson(UUID playerId, String type, String severity, String startDate, String endDate) {
        return """
                {
                  "playerId": "%s",
                  "injuryType": "%s",
                  "severity": "%s",
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(playerId, type, severity, startDate, endDate);
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}
