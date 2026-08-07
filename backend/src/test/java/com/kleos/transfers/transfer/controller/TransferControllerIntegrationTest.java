package com.kleos.transfers.transfer.controller;

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

class TransferControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String TRANSFERS_PATH = "/api/v1/transfers";

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
    void createsReadsAndUpdatesPermanentTransfer() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID fromClubId = createClub("Borussia Dortmund", "BVB", "GER");
        UUID toClubId = createClub("Real Madrid", "RMA", "ESP");
        UUID seasonId = createSeason("2024/25");

        MvcResult createResult = mockMvc.perform(post(TRANSFERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(
                                playerId, fromClubId, toClubId, seasonId,
                                "2024-06-10", "103000000", "PERMANENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Jude Bellingham"))
                .andExpect(jsonPath("$.fromClubName").value("Borussia Dortmund"))
                .andExpect(jsonPath("$.toClubName").value("Real Madrid"))
                .andExpect(jsonPath("$.seasonLabel").value("2024/25"))
                .andExpect(jsonPath("$.transferDate").value("2024-06-10"))
                .andExpect(jsonPath("$.feeEur").value(103000000))
                .andExpect(jsonPath("$.type").value("PERMANENT"))
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(TRANSFERS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(put(TRANSFERS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(
                                playerId, fromClubId, toClubId, seasonId,
                                "2024-06-15", "110000000", "PERMANENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferDate").value("2024-06-15"))
                .andExpect(jsonPath("$.feeEur").value(110000000));
    }

    @Test
    void acceptsFreeAgentSigningWithoutFromClub() throws Exception {
        UUID playerId = createPlayer("Free Agent");
        UUID toClubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(TRANSFERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "toClubId": "%s",
                                  "seasonId": "%s",
                                  "transferDate": "2024-07-01",
                                  "type": "FREE"
                                }
                                """.formatted(playerId, toClubId, seasonId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromClubId").value(nullValue()))
                .andExpect(jsonPath("$.toClubName").value("Arsenal"))
                .andExpect(jsonPath("$.feeEur").value(nullValue()))
                .andExpect(jsonPath("$.type").value("FREE"));
    }

    @Test
    void rejectsSameFromAndToClub() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(TRANSFERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(
                                playerId, clubId, clubId, seasonId,
                                "2024-07-01", "1000000", "PERMANENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTransferWithNoClubs() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID seasonId = createSeason("2024/25");

        mockMvc.perform(post(TRANSFERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "seasonId": "%s",
                                  "transferDate": "2024-07-01",
                                  "type": "FREE"
                                }
                                """.formatted(playerId, seasonId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateTransferEvent() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID fromClubId = createClub("Borussia Dortmund", "BVB", "GER");
        UUID toClubId = createClub("Real Madrid", "RMA", "ESP");
        UUID seasonId = createSeason("2024/25");
        String body = transferJson(
                playerId, fromClubId, toClubId, seasonId, "2024-06-10", "103000000", "PERMANENT");

        mockMvc.perform(post(TRANSFERS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(TRANSFERS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void softDeletesAndAllowsRecreate() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID fromClubId = createClub("Borussia Dortmund", "BVB", "GER");
        UUID toClubId = createClub("Real Madrid", "RMA", "ESP");
        UUID seasonId = createSeason("2024/25");
        String body = transferJson(
                playerId, fromClubId, toClubId, seasonId, "2024-06-10", "103000000", "PERMANENT");

        MvcResult createResult = mockMvc.perform(post(TRANSFERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));
        mockMvc.perform(delete(TRANSFERS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(TRANSFERS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void importsTransfersInBulkAndSkipsDuplicates() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID fromClubId = createClub("Borussia Dortmund", "BVB", "GER");
        UUID toClubId = createClub("Real Madrid", "RMA", "ESP");
        UUID seasonId = createSeason("2024/25");
        String first = transferJson(
                playerId, fromClubId, toClubId, seasonId, "2024-06-10", "103000000", "PERMANENT");
        String loan = """
                {
                  "playerId": "%s",
                  "fromClubId": "%s",
                  "toClubId": "%s",
                  "seasonId": "%s",
                  "transferDate": "2024-01-15",
                  "type": "LOAN"
                }
                """.formatted(playerId, toClubId, fromClubId, seasonId);

        mockMvc.perform(post(TRANSFERS_PATH + "/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [%s, %s, %s]}
                                """.formatted(first, first, loan)))
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

    private UUID createSeason(String label) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "%s",
                                  "startDate": "2024-07-01",
                                  "endDate": "2025-06-30"
                                }
                                """.formatted(label)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readId(result));
    }

    private String transferJson(
            UUID playerId,
            UUID fromClubId,
            UUID toClubId,
            UUID seasonId,
            String date,
            String fee,
            String type
    ) {
        return """
                {
                  "playerId": "%s",
                  "fromClubId": "%s",
                  "toClubId": "%s",
                  "seasonId": "%s",
                  "transferDate": "%s",
                  "feeEur": %s,
                  "type": "%s"
                }
                """.formatted(playerId, fromClubId, toClubId, seasonId, date, fee, type);
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}
