package com.kleos.transfers.prediction.controller;

import com.kleos.transfers.common.test.AbstractPostgresIntegrationTest;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class PredictionControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PREDICTIONS_PATH = "/api/v1/predictions";

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
    void createsExplainablePredictionAndSupportsEvaluationLifecycle() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham", "2003-06-29");
        UUID fromClubId = createClub("Borussia Dortmund", "BVB", "GER");
        UUID toClubId = createClub("Real Madrid", "RMA", "ESP");
        UUID priorSeasonId = createSeason("2023/24", "2023-07-01", "2024-06-30");
        UUID seasonId = createSeason("2024/25", "2024-07-01", "2025-06-30");
        UUID tournamentId = createTournament();
        createClubSeason(toClubId, seasonId, tournamentId);
        createPlayerSeason(playerId, fromClubId, priorSeasonId, 3000, 12, 8);
        createContract(playerId, fromClubId, "2022-07-01", "2025-06-30");
        createInjury(playerId, "Hamstring strain", "MODERATE", "2024-02-01", "2024-03-01");

        MvcResult createResult = mockMvc.perform(post(PREDICTIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "targetClubId": "%s",
                                  "seasonId": "%s",
                                  "note": "summer window scenario"
                                }
                                """.formatted(playerId, toClubId, seasonId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Jude Bellingham"))
                .andExpect(jsonPath("$.targetClubName").value("Real Madrid"))
                .andExpect(jsonPath("$.modelVersion").value("v0.2-heuristic"))
                .andExpect(jsonPath("$.predictedMinutes").value(greaterThan(0)))
                .andExpect(jsonPath("$.predictedGoals").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.predictedAssists").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.predictedXg").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.predictedXa").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.predictedMarketValueEur").value(notNullValue()))
                .andExpect(jsonPath("$.compatibilityScore").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.compatibilityScore").value(lessThanOrEqualTo(100.0)))
                .andExpect(jsonPath("$.compatibilityBreakdown.system").exists())
                .andExpect(jsonPath("$.compatibilityBreakdown.role").exists())
                .andExpect(jsonPath("$.compatibilityBreakdown.tempo").exists())
                .andExpect(jsonPath("$.compatibilityBreakdown.league").exists())
                .andExpect(jsonPath("$.compatibilityBreakdown.manager").exists())
                .andExpect(jsonPath("$.predictedMinutesLow").exists())
                .andExpect(jsonPath("$.predictedMinutesHigh").exists())
                .andExpect(jsonPath("$.confidenceScore").value(greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.confidenceScore").value(lessThanOrEqualTo(100.0)))
                .andExpect(jsonPath("$.explanations", hasSize(greaterThan(3))))
                .andExpect(jsonPath("$.explanations[0].factorCode").value(notNullValue()))
                .andExpect(jsonPath("$.explanations[0].direction").value(notNullValue()))
                .andReturn();

        UUID predictionId = UUID.fromString(readId(createResult));
        UUID runId = UUID.fromString(objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("runId")
                .asText());

        mockMvc.perform(get(PREDICTIONS_PATH + "/{id}", predictionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(predictionId.toString()));

        mockMvc.perform(get("/api/v1/prediction-runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").value("v0.2-heuristic"))
                .andExpect(jsonPath("$.predictions", hasSize(1)));

        createPlayerSeason(playerId, toClubId, seasonId, 2500, 9, 6);

        mockMvc.perform(post(PREDICTIONS_PATH + "/{id}/evaluate", predictionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluation.actualMinutes").value(2500))
                .andExpect(jsonPath("$.evaluation.actualGoals").value(9))
                .andExpect(jsonPath("$.evaluation.minutesError").value(notNullValue()));

        mockMvc.perform(post(PREDICTIONS_PATH + "/{id}/evaluate", predictionId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete(PREDICTIONS_PATH + "/{id}", predictionId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PREDICTIONS_PATH + "/{id}", predictionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnknownPlayer() throws Exception {
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25", "2024-07-01", "2025-06-30");

        mockMvc.perform(post(PREDICTIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "targetClubId": "%s",
                                  "seasonId": "%s"
                                }
                                """.formatted(UUID.randomUUID(), clubId, seasonId)))
                .andExpect(status().isNotFound());
    }

    private UUID createPlayer(String name, String dob) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "%s",
                                  "dateOfBirth": "%s",
                                  "nationality": "ENG",
                                  "heightCm": 186,
                                  "preferredFoot": "RIGHT",
                                  "primaryPosition": "CM"
                                }
                                """.formatted(name, dob)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private UUID createClub(String name, String shortName, String country) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "shortName": "%s",
                                  "countryCode": "%s"
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

    private UUID createTournament() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "La Liga",
                                  "shortName": "LAL",
                                  "confederation": "UEFA",
                                  "type": "LEAGUE",
                                  "countryCode": "ESP"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private void createClubSeason(UUID clubId, UUID seasonId, UUID tournamentId) throws Exception {
        mockMvc.perform(post("/api/v1/club-seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clubId": "%s",
                                  "seasonId": "%s",
                                  "tournamentId": "%s"
                                }
                                """.formatted(clubId, seasonId, tournamentId)))
                .andExpect(status().isCreated());
    }

    private void createPlayerSeason(
            UUID playerId,
            UUID clubId,
            UUID seasonId,
            int minutes,
            int goals,
            int assists
    ) throws Exception {
        mockMvc.perform(post("/api/v1/player-seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "clubId": "%s",
                                  "seasonId": "%s",
                                  "appearances": 30,
                                  "minutesPlayed": %d,
                                  "goals": %d,
                                  "assists": %d,
                                  "xg": 10.0,
                                  "xa": 6.0,
                                  "primaryPosition": "CM"
                                }
                                """.formatted(playerId, clubId, seasonId, minutes, goals, assists)))
                .andExpect(status().isCreated());
    }

    private void createContract(UUID playerId, UUID clubId, String start, String end) throws Exception {
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "clubId": "%s",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(playerId, clubId, start, end)))
                .andExpect(status().isCreated());
    }

    private void createInjury(UUID playerId, String type, String severity, String start, String end)
            throws Exception {
        mockMvc.perform(post("/api/v1/injuries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "injuryType": "%s",
                                  "severity": "%s",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(playerId, type, severity, start, end)))
                .andExpect(status().isCreated());
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}
