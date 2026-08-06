package com.kleos.transfers.playerseason.controller;

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
class PlayerSeasonControllerIntegrationTest {

    private static final String PLAYER_SEASONS_PATH = "/api/v1/player-seasons";

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
    void createsReadsAndUpdatesPlayerSeason() throws Exception {
        UUID playerId = createPlayer("Bukayo Saka");
        UUID clubId = createClub("Arsenal", "ARS");
        UUID seasonId = createSeason("2024/25");

        MvcResult createResult = mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, clubId, seasonId, 30, 2450, 8, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Bukayo Saka"))
                .andExpect(jsonPath("$.clubName").value("Arsenal"))
                .andExpect(jsonPath("$.seasonLabel").value("2024/25"))
                .andExpect(jsonPath("$.appearances").value(30))
                .andExpect(jsonPath("$.minutesPlayed").value(2450))
                .andExpect(jsonPath("$.goals").value(8))
                .andExpect(jsonPath("$.assists").value(5))
                .andExpect(jsonPath("$.xg").value(7.20))
                .andExpect(jsonPath("$.xa").value(4.10))
                .andExpect(jsonPath("$.primaryPosition").value("RW"))
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(PLAYER_SEASONS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(put(PLAYER_SEASONS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, clubId, seasonId, 32, 2600, 10, 6)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals").value(10))
                .andExpect(jsonPath("$.minutesPlayed").value(2600));
    }

    @Test
    void allowsSamePlayerAtTwoClubsInOneSeason() throws Exception {
        UUID playerId = createPlayer("Bukayo Saka");
        UUID arsenalId = createClub("Arsenal", "ARS");
        UUID chelseaId = createClub("Chelsea", "CHE");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, arsenalId, seasonId, 20, 1600, 5, 3)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, chelseaId, seasonId, 12, 900, 2, 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(PLAYER_SEASONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void rejectsDuplicatePlayerClubSeason() throws Exception {
        UUID playerId = createPlayer("Bukayo Saka");
        UUID clubId = createClub("Arsenal", "ARS");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, clubId, seasonId, 30, 2450, 8, 5)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, clubId, seasonId, 30, 2450, 8, 5)))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsNegativeStats() throws Exception {
        UUID playerId = createPlayer("Bukayo Saka");
        UUID clubId = createClub("Arsenal", "ARS");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "clubId": "%s",
                                  "seasonId": "%s",
                                  "appearances": -1,
                                  "minutesPlayed": 100,
                                  "goals": 0,
                                  "assists": 0,
                                  "xg": 0,
                                  "xa": 0,
                                  "primaryPosition": "RW"
                                }
                                """.formatted(playerId, clubId, seasonId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void softDeletesAndAllowsRecreate() throws Exception {
        UUID playerId = createPlayer("Bukayo Saka");
        UUID clubId = createClub("Arsenal", "ARS");
        UUID seasonId = createSeason("2024/25");

        MvcResult createResult = mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, clubId, seasonId, 30, 2450, 8, 5)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));
        mockMvc.perform(delete(PLAYER_SEASONS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(PLAYER_SEASONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerSeasonJson(playerId, clubId, seasonId, 30, 2450, 8, 5)))
                .andExpect(status().isCreated());
    }

    @Test
    void importsPlayerSeasonsInBulkAndSkipsDuplicates() throws Exception {
        UUID playerId = createPlayer("Bukayo Saka");
        UUID clubId = createClub("Arsenal", "ARS");
        UUID seasonId = createSeason("2024/25");
        UUID otherSeasonId = createSeason("2023/24");

        mockMvc.perform(post(PLAYER_SEASONS_PATH + "/bulk")
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
                                playerSeasonJson(playerId, clubId, seasonId, 30, 2450, 8, 5),
                                playerSeasonJson(playerId, clubId, seasonId, 30, 2450, 8, 5),
                                playerSeasonJson(playerId, clubId, otherSeasonId, 28, 2200, 6, 4))))
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
                                  "dateOfBirth": "2001-09-05",
                                  "nationality": "ENG",
                                  "heightCm": 178,
                                  "preferredFoot": "LEFT",
                                  "primaryPosition": "RW"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private UUID createClub(String name, String shortName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "shortName": "%s",
                                  "countryCode": "ENG"
                                }
                                """.formatted(name, shortName)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private UUID createSeason(String label) throws Exception {
        String start = label.startsWith("2023") ? "2023-07-01" : "2024-07-01";
        String end = label.startsWith("2023") ? "2024-06-30" : "2025-06-30";
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

    private String playerSeasonJson(
            UUID playerId,
            UUID clubId,
            UUID seasonId,
            int appearances,
            int minutes,
            int goals,
            int assists
    ) {
        return """
                {
                  "playerId": "%s",
                  "clubId": "%s",
                  "seasonId": "%s",
                  "appearances": %d,
                  "minutesPlayed": %d,
                  "goals": %d,
                  "assists": %d,
                  "xg": 7.20,
                  "xa": 4.10,
                  "primaryPosition": "RW"
                }
                """.formatted(playerId, clubId, seasonId, appearances, minutes, goals, assists);
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}
