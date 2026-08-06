package com.kleos.transfers.season.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.kleos.transfers.common.test.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeasonControllerIntegrationTest {

    private static final String SEASONS_PATH = "/api/v1/seasons";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearSeasons() {
        DatabaseCleaner.clearAll(jdbcTemplate);
    }

    @Test
    void createsReadsAndUpdatesSeasonIdentity() throws Exception {
        MvcResult createResult = mockMvc.perform(post(SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSeasonRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("2024/25"))
                .andExpect(jsonPath("$.startDate").value("2024-07-01"))
                .andExpect(jsonPath("$.endDate").value("2025-06-30"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(SEASONS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(put(SEASONS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "2024/25",
                                  "startDate": "2024-08-01",
                                  "endDate": "2025-05-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2024-08-01"))
                .andExpect(jsonPath("$.endDate").value("2025-05-31"));

        mockMvc.perform(get(SEASONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void acceptsCalendarYearSeasonLabel() throws Exception {
        mockMvc.perform(post(SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "2024",
                                  "startDate": "2024-01-01",
                                  "endDate": "2024-12-31"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("2024"));
    }

    @Test
    void rejectsInvalidLabelAndInvertedDateRange() throws Exception {
        mockMvc.perform(post(SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "2024/26",
                                  "startDate": "2025-06-30",
                                  "endDate": "2024-07-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(2));
    }

    @Test
    void rejectsDuplicateActiveLabel() throws Exception {
        mockMvc.perform(post(SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSeasonRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSeasonRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void softDeletesSeasonAndAllowsLabelReuse() throws Exception {
        MvcResult createResult = mockMvc.perform(post(SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSeasonRequest()))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(delete(SEASONS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(SEASONS_PATH + "/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSeasonRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("2024/25"));
    }

    @Test
    void importsSeasonsInBulkAndSkipsDuplicates() throws Exception {
        mockMvc.perform(post(SEASONS_PATH + "/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "label": "2023/24",
                                      "startDate": "2023-07-01",
                                      "endDate": "2024-06-30"
                                    },
                                    {
                                      "label": "2023/24",
                                      "startDate": "2023-07-01",
                                      "endDate": "2024-06-30"
                                    },
                                    {
                                      "label": "2024",
                                      "startDate": "2024-01-01",
                                      "endDate": "2024-12-31"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.skipped[0].reason").value("duplicate within request"));
    }

    @Test
    void returnsNotFoundForUnknownSeason() throws Exception {
        mockMvc.perform(get(SEASONS_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }

    private String validSeasonRequest() {
        return """
                {
                  "label": "2024/25",
                  "startDate": "2024-07-01",
                  "endDate": "2025-06-30"
                }
                """;
    }
}
