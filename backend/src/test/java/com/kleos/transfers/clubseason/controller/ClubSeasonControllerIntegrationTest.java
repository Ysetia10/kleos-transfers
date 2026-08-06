package com.kleos.transfers.clubseason.controller;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClubSeasonControllerIntegrationTest {

    private static final String CLUB_SEASONS_PATH = "/api/v1/club-seasons";

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
    void createsReadsAndUpdatesClubSeason() throws Exception {
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25", "2024-07-01", "2025-06-30");
        UUID tournamentId = createTournament("Premier League", "EPL", "ENG");

        MvcResult createResult = mockMvc.perform(post(CLUB_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clubSeasonJson(clubId, seasonId, tournamentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clubId").value(clubId.toString()))
                .andExpect(jsonPath("$.clubName").value("Arsenal"))
                .andExpect(jsonPath("$.seasonId").value(seasonId.toString()))
                .andExpect(jsonPath("$.seasonLabel").value("2024/25"))
                .andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
                .andExpect(jsonPath("$.tournamentName").value("Premier League"))
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(CLUB_SEASONS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        UUID otherTournamentId = createTournament("La Liga", "LaLiga", "ESP");
        mockMvc.perform(put(CLUB_SEASONS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clubSeasonJson(clubId, seasonId, otherTournamentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentName").value("La Liga"));

        mockMvc.perform(get(CLUB_SEASONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsUnknownIdentityReferences() throws Exception {
        mockMvc.perform(post(CLUB_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clubSeasonJson(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateClubSeasonPair() throws Exception {
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25", "2024-07-01", "2025-06-30");
        UUID tournamentId = createTournament("Premier League", "EPL", "ENG");

        mockMvc.perform(post(CLUB_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clubSeasonJson(clubId, seasonId, tournamentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(CLUB_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clubSeasonJson(clubId, seasonId, tournamentId)))
                .andExpect(status().isConflict());
    }

    @Test
    void softDeletesAndAllowsRecreateForSameClubSeason() throws Exception {
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25", "2024-07-01", "2025-06-30");
        UUID tournamentId = createTournament("Premier League", "EPL", "ENG");

        MvcResult createResult = mockMvc.perform(post(CLUB_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clubSeasonJson(clubId, seasonId, tournamentId)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));
        mockMvc.perform(delete(CLUB_SEASONS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CLUB_SEASONS_PATH + "/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(CLUB_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clubSeasonJson(clubId, seasonId, tournamentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clubName").value("Arsenal"));
    }

    @Test
    void importsClubSeasonsInBulkAndSkipsDuplicates() throws Exception {
        UUID arsenalId = createClub("Arsenal", "ARS", "ENG");
        UUID liverpoolId = createClub("Liverpool", "LIV", "ENG");
        UUID seasonId = createSeason("2024/25", "2024-07-01", "2025-06-30");
        UUID tournamentId = createTournament("Premier League", "EPL", "ENG");

        mockMvc.perform(post(CLUB_SEASONS_PATH + "/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    %s,
                                    %s,
                                    %s
                                  ]
                                }
                                """.formatted(
                                clubSeasonJson(arsenalId, seasonId, tournamentId),
                                clubSeasonJson(arsenalId, seasonId, tournamentId),
                                clubSeasonJson(liverpoolId, seasonId, tournamentId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.skipped[0].reason").value("duplicate within request"));
    }

    private UUID createClub(String name, String shortName, String country) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "shortName": "%s",
                                  "countryCode": "%s",
                                  "foundedYear": 1886
                                }
                                """.formatted(name, shortName, country)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private UUID createSeason(String label, String start, String end) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "%s",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(label, start, end)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private UUID createTournament(String name, String shortName, String country) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "shortName": "%s",
                                  "confederation": "UEFA",
                                  "type": "LEAGUE",
                                  "countryCode": "%s"
                                }
                                """.formatted(name, shortName, country)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private String clubSeasonJson(UUID clubId, UUID seasonId, UUID tournamentId) {
        return """
                {
                  "clubId": "%s",
                  "seasonId": "%s",
                  "tournamentId": "%s"
                }
                """.formatted(clubId, seasonId, tournamentId);
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}
