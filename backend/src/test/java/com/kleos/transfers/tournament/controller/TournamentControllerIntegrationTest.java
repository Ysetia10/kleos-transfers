package com.kleos.transfers.tournament.controller;

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

class TournamentControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String TOURNAMENTS_PATH = "/api/v1/tournaments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearTournaments() {
        DatabaseCleaner.clearAll(jdbcTemplate);
    }

    @Test
    void createsReadsAndUpdatesDomesticTournament() throws Exception {
        MvcResult createResult = mockMvc.perform(post(TOURNAMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(premierLeagueRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Premier League"))
                .andExpect(jsonPath("$.shortName").value("EPL"))
                .andExpect(jsonPath("$.confederation").value("UEFA"))
                .andExpect(jsonPath("$.type").value("LEAGUE"))
                .andExpect(jsonPath("$.countryCode").value("ENG"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(TOURNAMENTS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(put(TOURNAMENTS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Premier League",
                                  "shortName": "PL",
                                  "confederation": "UEFA",
                                  "type": "LEAGUE",
                                  "countryCode": "eng"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortName").value("PL"))
                .andExpect(jsonPath("$.countryCode").value("ENG"));

        mockMvc.perform(get(TOURNAMENTS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void acceptsContinentalTournamentWithoutCountry() throws Exception {
        mockMvc.perform(post(TOURNAMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "UEFA Champions League",
                                  "shortName": "UCL",
                                  "confederation": "UEFA",
                                  "type": "CUP"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("UEFA Champions League"))
                .andExpect(jsonPath("$.countryCode").value(nullValue()));
    }

    @Test
    void rejectsInvalidTournamentRequest() throws Exception {
        mockMvc.perform(post(TOURNAMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "X",
                                  "shortName": "Y",
                                  "confederation": "UEFA",
                                  "type": "LEAGUE",
                                  "countryCode": "XXX"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(3));
    }

    @Test
    void rejectsDuplicateActiveName() throws Exception {
        mockMvc.perform(post(TOURNAMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(premierLeagueRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(TOURNAMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(premierLeagueRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void softDeletesTournamentAndAllowsNameReuse() throws Exception {
        MvcResult createResult = mockMvc.perform(post(TOURNAMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(premierLeagueRequest()))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(delete(TOURNAMENTS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TOURNAMENTS_PATH + "/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(TOURNAMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(premierLeagueRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Premier League"));
    }

    @Test
    void importsTournamentsInBulkAndSkipsDuplicates() throws Exception {
        mockMvc.perform(post(TOURNAMENTS_PATH + "/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "name": "La Liga",
                                      "shortName": "LaLiga",
                                      "confederation": "UEFA",
                                      "type": "LEAGUE",
                                      "countryCode": "ESP"
                                    },
                                    {
                                      "name": "La Liga",
                                      "shortName": "LaLiga",
                                      "confederation": "UEFA",
                                      "type": "LEAGUE",
                                      "countryCode": "ESP"
                                    },
                                    {
                                      "name": "FIFA World Cup",
                                      "shortName": "WC",
                                      "confederation": "FIFA",
                                      "type": "CUP"
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
    void returnsNotFoundForUnknownTournament() throws Exception {
        mockMvc.perform(get(TOURNAMENTS_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }

    private String premierLeagueRequest() {
        return """
                {
                  "name": "Premier League",
                  "shortName": "EPL",
                  "confederation": "UEFA",
                  "type": "LEAGUE",
                  "countryCode": "ENG"
                }
                """;
    }
}
