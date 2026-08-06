package com.kleos.transfers.contract.controller;

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
class ContractControllerIntegrationTest {

    private static final String CONTRACTS_PATH = "/api/v1/contracts";

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
    void createsReadsAndUpdatesContract() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Real Madrid", "RMA", "ESP");

        MvcResult createResult = mockMvc.perform(post(CONTRACTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractJson(playerId, clubId, "2023-07-01", "2029-06-30", "1000000000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerName").value("Jude Bellingham"))
                .andExpect(jsonPath("$.clubName").value("Real Madrid"))
                .andExpect(jsonPath("$.endDate").value("2029-06-30"))
                .andExpect(jsonPath("$.releaseClauseEur").value(1000000000))
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));

        mockMvc.perform(get(CONTRACTS_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        mockMvc.perform(put(CONTRACTS_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractJson(playerId, clubId, "2023-07-01", "2031-06-30", "1500000000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value("2031-06-30"))
                .andExpect(jsonPath("$.releaseClauseEur").value(1500000000));
    }

    @Test
    void acceptsContractWithoutReleaseClause() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");

        mockMvc.perform(post(CONTRACTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": "%s",
                                  "clubId": "%s",
                                  "startDate": "2023-07-01",
                                  "endDate": "2027-06-30"
                                }
                                """.formatted(playerId, clubId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.releaseClauseEur").value(nullValue()));
    }

    @Test
    void rejectsEndDateBeforeStartDate() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Arsenal", "ARS", "ENG");

        mockMvc.perform(post(CONTRACTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractJson(playerId, clubId, "2027-06-30", "2023-07-01", "1000")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateContractStart() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Real Madrid", "RMA", "ESP");
        String body = contractJson(playerId, clubId, "2023-07-01", "2029-06-30", "1000000000");

        mockMvc.perform(post(CONTRACTS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(CONTRACTS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void allowsRenewalAsSeparateContractRow() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Real Madrid", "RMA", "ESP");

        mockMvc.perform(post(CONTRACTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractJson(playerId, clubId, "2023-07-01", "2029-06-30", "1000000000")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(CONTRACTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractJson(playerId, clubId, "2026-07-01", "2032-06-30", "2000000000")))
                .andExpect(status().isCreated());
    }

    @Test
    void softDeletesAndAllowsRecreate() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Real Madrid", "RMA", "ESP");
        String body = contractJson(playerId, clubId, "2023-07-01", "2029-06-30", "1000000000");

        MvcResult createResult = mockMvc.perform(post(CONTRACTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(readId(createResult));
        mockMvc.perform(delete(CONTRACTS_PATH + "/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(CONTRACTS_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void importsContractsInBulkAndSkipsDuplicates() throws Exception {
        UUID playerId = createPlayer("Jude Bellingham");
        UUID clubId = createClub("Real Madrid", "RMA", "ESP");
        String first = contractJson(playerId, clubId, "2023-07-01", "2029-06-30", "1000000000");
        String renewal = contractJson(playerId, clubId, "2026-07-01", "2032-06-30", "2000000000");

        mockMvc.perform(post(CONTRACTS_PATH + "/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [%s, %s, %s]}
                                """.formatted(first, first, renewal)))
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

    private String contractJson(UUID playerId, UUID clubId, String startDate, String endDate, String releaseClause) {
        return """
                {
                  "playerId": "%s",
                  "clubId": "%s",
                  "startDate": "%s",
                  "endDate": "%s",
                  "releaseClauseEur": %s
                }
                """.formatted(playerId, clubId, startDate, endDate, releaseClause);
    }

    private String readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}
