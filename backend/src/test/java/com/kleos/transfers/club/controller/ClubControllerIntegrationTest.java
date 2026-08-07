package com.kleos.transfers.club.controller;

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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.kleos.transfers.common.test.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class ClubControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String CLUBS_PATH = "/api/v1/clubs";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearClubs() {
        DatabaseCleaner.clearAll(jdbcTemplate);
    }

    @Test
    void createsReadsAndUpdatesClubIdentity() throws Exception {
        MvcResult createResult = mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("FC Barcelona"))
                .andExpect(jsonPath("$.shortName").value("Barcelona"))
                .andExpect(jsonPath("$.countryCode").value("ESP"))
                .andExpect(jsonPath("$.foundedYear").value(1899))
                .andExpect(jsonPath("$.currentManagerId").value(nullValue()))
                .andExpect(jsonPath("$.currentManagerName").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(CLUBS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.shortName").value("Barcelona"))
                .andExpect(jsonPath("$.currentManagerName").value(nullValue()));

        mockMvc.perform(put(CLUBS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedClubRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortName").value("Barça"))
                .andExpect(jsonPath("$.foundedYear").value(nullValue()));

        mockMvc.perform(get(CLUBS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].shortName").value("Barça"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void acceptsClubWithoutFoundedYear() throws Exception {
        mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Newcastle United",
                                  "shortName": "Newcastle",
                                  "countryCode": "ENG"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Newcastle United"))
                .andExpect(jsonPath("$.foundedYear").value(nullValue()));
    }

    @Test
    void normalizesLowercaseCountryCode() throws Exception {
        mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ajax",
                                  "shortName": "Ajax",
                                  "countryCode": "ned",
                                  "foundedYear": 1900
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countryCode").value("NED"));
    }

    @Test
    void rejectsDuplicateActiveClubNameInSameCountry() throws Exception {
        mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void allowsSameNameAfterSoftDelete() throws Exception {
        MvcResult createResult = mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubRequest()))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(delete(CLUBS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("FC Barcelona"));
    }

    @Test
    void rejectsInvalidClubRequest() throws Exception {
        mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "A",
                                  "shortName": "B",
                                  "countryCode": "XXX",
                                  "foundedYear": 1700
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(4));
    }

    @Test
    void softDeletesClubAndHidesItFromReads() throws Exception {
        MvcResult createResult = mockMvc.perform(post(CLUBS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubRequest()))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(delete(CLUBS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CLUBS_PATH + "/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(CLUBS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void returnsNotFoundForUnknownClub() throws Exception {
        mockMvc.perform(get(CLUBS_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }

    private String validClubRequest() {
        return """
                {
                  "name": "FC Barcelona",
                  "shortName": "Barcelona",
                  "countryCode": "ESP",
                  "foundedYear": 1899
                }
                """;
    }

    private String updatedClubRequest() {
        return """
                {
                  "name": "FC Barcelona",
                  "shortName": "Barça",
                  "countryCode": "ESP",
                  "foundedYear": null
                }
                """;
    }
}
